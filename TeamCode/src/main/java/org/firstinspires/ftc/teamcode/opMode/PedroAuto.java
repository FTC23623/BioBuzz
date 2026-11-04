package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathSegment;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.pedro.PedroConstants;

/**
 * Everything the Pedro autos share. An auto only has to say which paths to drive, in its
 * own {@link #BuildPaths()}; this class drives them one after another, then holds the robot
 * still at the end. To write a new auto, copy {@link AutoShapeZ} and change its paths.
 *
 * <p><b>How Pedro measures</b>: inches and radians. The robot starts at {@link #StartPose()},
 * {@code (0, 0)} facing heading 0, unless the auto says otherwise. From there:
 * <ul>
 *   <li>x grows as the robot drives FORWARD
 *   <li>y grows as the robot drives LEFT
 *   <li>heading grows as the robot turns LEFT. 90 degrees is {@code Math.toRadians(90)}.
 * </ul>
 *
 * <p>How following works: {@code follower.follow(path)} starts a path. Then, every loop,
 * {@code follower.update()} reads where the robot is and sets the motor powers. When a path
 * is done, the follower stops "following" and holds the robot in place, and we start the
 * next path. See docs/decisions/0008.
 *
 * <p><b>Alliance</b>: read from the magnet on the robot, like TeleOp (docs/decisions/0021).
 * The camera only sees our hive's tags. A backup auto (group "Backup") skips the magnet
 * and sets the alliance itself, in case the sensor plays up.
 *
 * <p><b>Scoring</b>: with {@code autoRunScoring} on, the launchers spin and the intake and
 * transfer run from START to the end. A LAUNCH step opens both launch gates.
 */
public abstract class PedroAuto extends LinearOpMode {

    /**
     * The robot's settings, so paths can use numbers stored about the bot. Every auto gets a
     * normal {@link BotProperties}; an auto may change a field on it inside {@link #BuildPaths()}
     * (for example {@code properties.someValue = 12;}). Replace the object only before START.
     * Changing it does not change the Bot's drive or launcher switches, which are set in
     * {@link #runOpMode()}.
     */
    protected BotProperties botProperties = new BotProperties();

    /**
     * Put this in the list from {@link #BuildPaths()} to stop and launch there. It is not a
     * real path: when the auto reaches it, the robot holds still where the last path ended,
     * waits for the launchers to reach speed (at most {@code autoSpinUpSeconds}), then opens
     * both launch gates for {@code autoLaunchSeconds}. Once the gates are open, the intake and
     * transfer push the balls in. Then the gates close and the next path starts. The
     * indicator light blinks yellow for the whole stop.
     *
     * <pre>
     *     paths.add(driveToLaunchSpot);
     *     paths.add(LAUNCH);
     *     paths.add(driveAway);
     * </pre>
     */
    // Pedro will not build a line with zero length, so the marker is one inch long. It is
    // never driven: the loop only checks "is this step LAUNCH?".
    protected static final Path LAUNCH = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Like {@link #LAUNCH}, but the robot first holds still until the camera sees an
     * AprilTag. The indicator light blinks orange while it waits. Once a tag is seen, the
     * robot turns in place to face the middle of that tag's group, from
     * {@code botProperties.tagGroups} (the light stays orange), then launches just like
     * LAUNCH: blinks yellow for {@code botProperties.autoLaunchSeconds}. The aim is picked
     * once, from the first look at the tag, and is not corrected during the launch. Only our
     * hive's tags count (blue: 38-45). If none is seen by {@code autoLaunchAnywayAtSeconds}
     * after START, it launches anyway, facing the way it is.
     */
    // A different marker from LAUNCH, so the loop can tell the two apart.
    protected static final Path LAUNCH_WHEN_TAG_SEEN = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Put this in the list after the path to a flower to work it there (docs/decisions/0042).
     * The intake runs, and from where the last path ended the robot:
     * <ol>
     *   <li>backs up {@code autoFlowerBackIn} (9 in), and holds that spot
     *       {@code autoFlowerSettleSeconds} (0.3 s) so the robot has stopped,
     *   <li>swings the extractor out, and holds still {@code autoExtractorSeconds} (0.5 s)
     *       before the first stroke,
     *   <li>strokes forward {@code autoFlowerStrokeIn} (8 in), holds there
     *       {@code autoFlowerDwellSeconds} (1 s), strokes back, {@code autoFlowerPasses} times (1),
     *   <li>swings the extractor in.
     * </ol>
     * Every move is along the way the robot faces, so it ends {@code autoFlowerBackIn} behind
     * the spot, still facing the flower. Tool: Pose Recorder writes it after a circle press.
     */
    // A third marker, like LAUNCH, so the loop can tell it apart.
    protected static final Path FLOWER = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * One thing the FLOWER step does: drive to a spot, hold still for a time, or swing the
     * extractor out or in (and hold still while it swings).
     */
    static final class FlowerAction {
        static final int MOVE = 0;
        static final int HOLD = 1;
        static final int ARM_OUT = 2;
        static final int ARM_IN = 3;

        final int kind;
        /** MOVE: where to drive. HOLD and the arm: where to hold still. */
        final Pose spot;
        /** HOLD and the arm: how long. */
        final double seconds;
        /** For the Driver Station: "back up", "settle", "stroke forward", ... */
        final String name;

        FlowerAction(int kind, Pose spot, double seconds, String name) {
            this.kind = kind;
            this.spot = spot;
            this.seconds = seconds;
            this.name = name;
        }
    }

    // The FLOWER step's state while it runs.
    private List<FlowerAction> mFlowerActions = new ArrayList<>();
    private Pose mFlowerStart = new Pose(0, 0, 0);
    /** Which action is running. -1 before the first; past the end when done. */
    private int mFlowerIndex = 0;
    private final ElapsedTime mFlowerTimer = new ElapsedTime();
    private double mFlowerGiveUpSeconds = 0;
    /** The robot's top speed, for each flower move's give-up time. Set in runOpMode. */
    private double mTopSpeed = 1;

    /** The paths this auto drives, in order. Runs after {@link #properties} is set up. */
    protected abstract List<Path> BuildPaths();

    /** Where the robot starts: (0, 0), facing forward. An auto may override this. */
    protected Pose StartPose() {
        return new Pose(0, 0, 0);
    }

    @Override
    public void runOpMode() {
        // In an auto, Pedro Pathing drives the wheels, so driver control is switched off.
        botProperties.driveDisabled = true;

        Bot bot = new Bot(this, botProperties);

        if (!PedroConstants.foresightTuned) {
            // Without tuned numbers, Pedro does not know how hard to push or when to brake.
            while (!isStopRequested()) {
                telemetry.addLine("PEDRO IS NOT TUNED YET. This auto will not move.");
                telemetry.addLine("Run AutoTune, paste the results into PedroConstants,");
                telemetry.addLine("then set foresightTuned to true. See docs/decisions/0008.");
                telemetry.update();
                idle();
            }
            return;
        }

        // Paths first, so a setting the auto changes in BuildPaths() (like autoSpeedScale)
        // is in place before the follower is built.
        List<Path> paths = BuildPaths();
        Follower follower = bot.CreateFollower();
        follower.setPose(StartPose());

        // How long each path may take before we give up on it, worked out now from how long
        // it is. Strafing is the robot's slower way to drive, so use that speed: it can only
        // give more time, never less.
        double topSpeed = PedroConstants.ForesightConfig().maxAchievableStrafeVelocity.get();
        mTopSpeed = topSpeed;
        double[] giveUpSeconds = new double[paths.size()];
        for (int i = 0; i < paths.size(); i++) {
            giveUpSeconds[i] = GiveUpSeconds(paths.get(i), botProperties, topSpeed);
        }
        bot.Init();

        while (opModeInInit()) {
            bot.StartLoop();
            bot.ShowStatus();
            telemetry.addLine("Put the robot on its start spot, facing forward.");
            telemetry.addData("Paths to drive", paths.size());
            if (botProperties.autoSpeedScale < 1) {
                telemetry.addLine(String.format("SLOW AUTO: %.0f%% speed. Put autoSpeedScale back to 1 for matches.",
                        botProperties.autoSpeedScale * 100));
            }
            bot.EndLoop();
        }

        if (botProperties.autoRunScoring) {
            // Spin up now, so the launchers are at speed by the first LAUNCH.
            bot.SpinLaunchers(true);
            bot.RunIntake(true);
        }

        // Times how long the robot has been on this step: a path, or a LAUNCH stop.
        ElapsedTime stepTimer = new ElapsedTime();
        // Times the whole auto, from START, for "launch anyway".
        ElapsedTime autoTimer = new ElapsedTime();
        int pathNumber = 0;
        int pathsGivenUp = 0;
        // true while a LAUNCH_WHEN_TAG_SEEN step is still waiting for the camera.
        boolean waitingForTag = paths.get(pathNumber) == LAUNCH_WHEN_TAG_SEEN;
        // true while a LAUNCH_WHEN_TAG_SEEN step is turning to face the tag it saw.
        boolean aiming = false;
        // The heading that faces the tag, in radians. Picked once, when the tag is first seen.
        double aimHeading = 0;
        // true while a launch step waits for the launchers to reach speed, gates still shut.
        boolean spinningUp = paths.get(pathNumber) == LAUNCH;
        // How many launches have opened the gates so far. The first one waits longer.
        int launchesDone = 0;
        StartStep(bot, follower, paths.get(pathNumber), stepTimer);

        while (opModeIsActive()) {
            bot.StartLoop();
            follower.update();

            Path step = paths.get(pathNumber);
            if (waitingForTag && bot.SeesTag()) {
                // A tag! Aim at the middle of its group (a hive's tags), not just this one tag.
                // Bearing is left-positive, like heading, so add it to face that middle.
                // Hold at the same spot with that heading: the robot turns in place.
                waitingForTag = false;
                aiming = true;
                Pose here = follower.pose();
                aimHeading = here.heading() + Math.toRadians(bot.GetTagGroupBearing());
                follower.hold(new Pose(here.x(), here.y(), aimHeading));
                stepTimer.reset();
            }
            if (waitingForTag && autoTimer.seconds() >= botProperties.autoLaunchAnywayAtSeconds) {
                // No tag, and the auto is nearly over. Launch anyway, facing the way we are.
                waitingForTag = false;
                spinningUp = true;
                bot.ShowWaitingForTag(false);
                bot.ShowLaunching(true);
                stepTimer.reset();
            }
            if (aiming) {
                boolean onTarget = Math.abs(AngleBetween(follower.pose().heading(), aimHeading))
                        <= Math.toRadians(botProperties.aimDoneDegrees);
                boolean aimTookTooLong = stepTimer.seconds() >= botProperties.autoAimSeconds;
                if (onTarget || aimTookTooLong) {
                    // Facing the tag (or close enough). Now launch, like LAUNCH. The follower
                    // keeps holding this heading; the camera is not asked again.
                    aiming = false;
                    spinningUp = true;
                    bot.ShowWaitingForTag(false);
                    bot.ShowLaunching(true);
                    stepTimer.reset();
                }
            }
            if (spinningUp) {
                // Fast enough to launch: the pollen wheel at autoLaunchMinRpm or more.
                boolean fastEnough = bot.PollenLauncherAtLeast(botProperties.autoLaunchMinRpm);
                if (ReadyToOpen(fastEnough, stepTimer.seconds(), launchesDone == 0, botProperties)) {
                    // Up to speed (or waited as long as allowed): open the gates. The launch
                    // time counts from now. The rollers start once the gates are fully open.
                    spinningUp = false;
                    launchesDone = launchesDone + 1;
                    bot.OpenLaunchGates(true);
                    stepTimer.reset();
                }
            }
            boolean launching = step == LAUNCH || step == LAUNCH_WHEN_TAG_SEEN;
            boolean pathFinished;
            if (waitingForTag || aiming || spinningUp) {
                pathFinished = false;
            } else if (step == FLOWER) {
                pathFinished = FlowerStep(bot, follower);
            } else if (launching) {
                pathFinished = stepTimer.seconds() >= botProperties.autoLaunchSeconds;
            } else {
                boolean tookTooLong = stepTimer.seconds() >= giveUpSeconds[pathNumber];
                if (follower.following() && tookTooLong) {
                    // Stuck, maybe on a ball pinned against the wall. Stop pushing: hold
                    // where the robot is now, and count this path as done.
                    follower.hold(follower.pose());
                    pathsGivenUp = pathsGivenUp + 1;
                }
                pathFinished = !follower.following();
            }
            if (launching && pathFinished) {
                // The launch time is up: close the gates and stop flashing the light.
                bot.ShowLaunching(false);
                bot.OpenLaunchGates(false);
            }
            boolean morePathsLeft = pathNumber + 1 < paths.size();
            if (pathFinished && morePathsLeft) {
                pathNumber = pathNumber + 1;
                waitingForTag = paths.get(pathNumber) == LAUNCH_WHEN_TAG_SEEN;
                spinningUp = paths.get(pathNumber) == LAUNCH;
                StartStep(bot, follower, paths.get(pathNumber), stepTimer);
            }

            // An auto has no gamepads, so there is no HandleUserInput() here.
            bot.Process();

            Pose where = follower.pose();
            telemetry.addData("Path", (pathNumber + 1) + " of " + paths.size());
            telemetry.addData("Waiting for a tag", waitingForTag);
            telemetry.addData("Aiming at the tag", aiming);
            if (spinningUp) {
                double limit = botProperties.autoSpinUpSeconds;
                if (launchesDone == 0) {
                    limit = botProperties.autoFirstSpinUpSeconds;
                }
                telemetry.addData("Waiting for launchers to reach speed", "yes, pollen %.0f of %.0f RPM, %.1f of at most %.1f s%s",
                        bot.PollenLauncherRpm(), botProperties.autoLaunchMinRpm,
                        stepTimer.seconds(), limit, launchesDone == 0 ? " (first launch)" : "");
            } else {
                telemetry.addData("Waiting for launchers to reach speed", "no");
            }
            telemetry.addData("Launching", !waitingForTag && !aiming && !spinningUp && launching);
            telemetry.addData("Auto time (s)", "%.1f", autoTimer.seconds());
            telemetry.addData("Launchers at speed", bot.LaunchersAtSpeed());
            if (step == FLOWER) {
                String doing = "done";
                if (mFlowerIndex >= 0 && mFlowerIndex < mFlowerActions.size()) {
                    doing = mFlowerActions.get(mFlowerIndex).name;
                }
                telemetry.addData("Flower", "%d of %d: %s", mFlowerIndex + 1, mFlowerActions.size(), doing);
            } else if (!launching) {
                telemetry.addData("Give up on this path after (s)", "%.1f", giveUpSeconds[pathNumber]);
            }
            telemetry.addData("Paths given up (took too long)", pathsGivenUp);
            telemetry.addData("Done", pathFinished && !morePathsLeft);
            telemetry.addData("x (forward, in)", where.x());
            telemetry.addData("y (left, in)", where.y());
            telemetry.addData("Heading (deg)", Math.toDegrees(where.heading()));
            bot.EndLoop();
        }

        follower.stop();
    }

    /**
     * The most time a path may take, in seconds, before the auto gives up on it:
     *
     * <pre>
     *     (drive time + turn time) x margin + extra
     * </pre>
     *
     * Drive time is the path's length at the auto's speed. Turn time is how far the heading
     * changes, in quarter turns, times the time one quarter turn takes. The margin and the
     * extra are room for speeding up and slowing down. Every number is in BotProperties.
     *
     * @param topSpeed the robot's top speed, in inches per second
     */
    static double GiveUpSeconds(Path path, BotProperties properties, double topSpeed) {
        double lengthIn = 0;
        for (PathSegment segment : path.getSegments()) {
            lengthIn = lengthIn + segment.curve.length();
        }
        double driveSeconds = lengthIn / topSpeed;

        // The heading change, from -180 to 180 degrees, so a turn from -90 to 180 counts
        // as the 90 it really is and not 270.
        double headingChange = AngleBetween(path.get(0).heading(), path.endPose().heading());
        double quarterTurns = Math.abs(headingChange) / Math.toRadians(90);
        double turnSeconds = quarterTurns * properties.autoQuarterTurnSeconds;

        // A slower auto needs more time for the same path.
        double seconds = (driveSeconds + turnSeconds) / properties.autoSpeedScale;
        return seconds * properties.autoGiveUpMargin + properties.autoGiveUpExtraSeconds;
    }

    /**
     * May a LAUNCH step open the gates yet? Yes once the pollen launcher spins at
     * autoLaunchMinRpm or faster. If it never gets there, yes anyway after autoSpinUpSeconds, or
     * after autoFirstSpinUpSeconds for the first launch of the auto, so a slow or broken
     * launcher can't freeze the auto.
     *
     * @param fastEnough    true when the pollen launcher is at autoLaunchMinRpm or faster
     * @param waitedSeconds how long this launch has waited so far
     * @param firstLaunch   true for the first launch of this auto
     */
    static boolean ReadyToOpen(boolean fastEnough, double waitedSeconds, boolean firstLaunch,
                               BotProperties properties) {
        if (fastEnough) {
            return true;
        }
        double limit = properties.autoSpinUpSeconds;
        if (firstLaunch) {
            limit = properties.autoFirstSpinUpSeconds;
        }
        return waitedSeconds >= limit;
    }

    /**
     * How far to turn from one heading to another, in radians, from -180 to 180 degrees.
     * So a turn from -90 to 180 counts as the 90 it really is, not 270.
     */
    static double AngleBetween(double fromHeading, double toHeading) {
        double change = toHeading - fromHeading;
        return Math.atan2(Math.sin(change), Math.cos(change));
    }

    /** Start one step from the list: drive a path, stop and launch, or stop and wait for a tag. */
    private void StartStep(Bot bot, Follower follower, Path step, ElapsedTime stepTimer) {
        stepTimer.reset();
        if (step == LAUNCH) {
            // Nothing new to follow, so the follower keeps holding the robot where it is.
            // Blink the light yellow. The gates open once the launchers are at speed.
            bot.ShowLaunching(true);
        } else if (step == LAUNCH_WHEN_TAG_SEEN) {
            // Hold still, like LAUNCH, but blink orange until the camera sees a tag.
            bot.ShowWaitingForTag(true);
        } else if (step == FLOWER) {
            // Work the flower from here. The intake runs for the whole routine.
            mFlowerStart = follower.pose();
            mFlowerActions = FlowerActions(mFlowerStart, botProperties);
            mFlowerIndex = -1;
            bot.RunIntake(true);
            NextFlowerAction(bot, follower);
        } else {
            follower.follow(step);
        }
    }

    /**
     * Where each FLOWER move ends, worked out from where the robot is and the way it faces:
     * first the back-up, then each stroke forward and back. Every pose keeps the heading.
     *
     * <pre>
     *     facing 0 at (0, 0), back 9, strokes of 8, 2 passes (today's 1 pass stops after the third):
     *     (-9, 0)  (-1, 0)  (-9, 0)  (-1, 0)  (-9, 0)
     * </pre>
     */
    static List<Pose> FlowerMoves(Pose spot, BotProperties p) {
        double heading = spot.heading();
        // One inch "forward" on the field, for a robot facing this heading.
        double forwardX = Math.cos(heading);
        double forwardY = Math.sin(heading);
        List<Pose> moves = new ArrayList<>();
        double alongIn = -p.autoFlowerBackIn;
        moves.add(new Pose(spot.x() + forwardX * alongIn, spot.y() + forwardY * alongIn, heading));
        for (int pass = 0; pass < p.autoFlowerPasses; pass++) {
            alongIn = alongIn + p.autoFlowerStrokeIn;
            moves.add(new Pose(spot.x() + forwardX * alongIn, spot.y() + forwardY * alongIn, heading));
            alongIn = alongIn - p.autoFlowerStrokeIn;
            moves.add(new Pose(spot.x() + forwardX * alongIn, spot.y() + forwardY * alongIn, heading));
        }
        return moves;
    }

    /**
     * The FLOWER step in order, from the recorded spot:
     *
     * <pre>
     *     move: back up                 autoFlowerBackIn (9)
     *     hold: settle                  autoFlowerSettleSeconds (0.3)
     *     arm out                       autoExtractorSeconds (0.5)
     *     move: stroke forward          autoFlowerStrokeIn (8)
     *     hold: at the flower           autoFlowerDwellSeconds (1)
     *     move: stroke back             autoFlowerStrokeIn (8)
     *     ... again, if autoFlowerPasses (1) is more than 1 ...
     *     arm in                        autoExtractorSeconds (0.5)
     * </pre>
     */
    static List<FlowerAction> FlowerActions(Pose spot, BotProperties p) {
        List<Pose> moves = FlowerMoves(spot, p);
        List<FlowerAction> actions = new ArrayList<>();
        Pose backedUp = moves.get(0);
        actions.add(new FlowerAction(FlowerAction.MOVE, backedUp, 0, "back up"));
        actions.add(new FlowerAction(FlowerAction.HOLD, backedUp, p.autoFlowerSettleSeconds, "settle"));
        actions.add(new FlowerAction(FlowerAction.ARM_OUT, backedUp, p.autoExtractorSeconds, "arm out"));
        // moves 1, 3, 5... are forward strokes; 2, 4, 6... are back strokes.
        for (int i = 1; i < moves.size(); i++) {
            Pose to = moves.get(i);
            boolean forward = i % 2 == 1;
            if (forward) {
                actions.add(new FlowerAction(FlowerAction.MOVE, to, 0, "stroke forward"));
                if (p.autoFlowerDwellSeconds > 0) {
                    actions.add(new FlowerAction(FlowerAction.HOLD, to, p.autoFlowerDwellSeconds, "at the flower"));
                }
            } else {
                actions.add(new FlowerAction(FlowerAction.MOVE, to, 0, "stroke back"));
            }
        }
        Pose last = moves.get(moves.size() - 1);
        actions.add(new FlowerAction(FlowerAction.ARM_IN, last, p.autoExtractorSeconds, "arm in"));
        return actions;
    }

    /** Start the next FLOWER action. */
    private void NextFlowerAction(Bot bot, Follower follower) {
        mFlowerIndex = mFlowerIndex + 1;
        mFlowerTimer.reset();
        if (mFlowerIndex >= mFlowerActions.size()) {
            return;
        }
        FlowerAction action = mFlowerActions.get(mFlowerIndex);
        if (action.kind == FlowerAction.MOVE) {
            // From where the last action meant the robot to be, not where it happens to be.
            Pose from = mFlowerStart;
            if (mFlowerIndex > 0) {
                from = mFlowerActions.get(mFlowerIndex - 1).spot;
            }
            if (Math.hypot(action.spot.x() - from.x(), action.spot.y() - from.y()) < 0.01) {
                // A zero-inch move (a setting of 0): Pedro won't build it, so skip it.
                NextFlowerAction(bot, follower);
                return;
            }
            Path path = line(from, action.spot).constant(mFlowerStart.heading());
            mFlowerGiveUpSeconds = GiveUpSeconds(path, botProperties, mTopSpeed);
            follower.follow(path);
            return;
        }
        // HOLD and the arm: hold the PLANNED spot, not wherever the robot was when the last
        // move ended: it may still be braking short of it.
        follower.hold(action.spot);
        if (action.kind == FlowerAction.ARM_OUT) {
            bot.SetFlowerExtractor(true);
        } else if (action.kind == FlowerAction.ARM_IN) {
            bot.SetFlowerExtractor(false);
        }
    }

    /** One loop of the FLOWER step. @return true once the arm is back in. */
    private boolean FlowerStep(Bot bot, Follower follower) {
        if (mFlowerIndex >= mFlowerActions.size()) {
            return true;
        }
        FlowerAction action = mFlowerActions.get(mFlowerIndex);
        boolean actionDone;
        if (action.kind == FlowerAction.MOVE) {
            boolean tookTooLong = mFlowerTimer.seconds() >= mFlowerGiveUpSeconds;
            if (follower.following() && tookTooLong) {
                // Stuck on the flower: stop pushing and carry on.
                follower.hold(follower.pose());
            }
            actionDone = !follower.following();
        } else {
            actionDone = mFlowerTimer.seconds() >= action.seconds;
        }
        if (actionDone) {
            NextFlowerAction(bot, follower);
        }
        if (mFlowerIndex >= mFlowerActions.size()) {
            // Done. The intake goes back to what the auto does between steps.
            bot.RunIntake(botProperties.autoRunScoring);
            return true;
        }
        return false;
    }
}
