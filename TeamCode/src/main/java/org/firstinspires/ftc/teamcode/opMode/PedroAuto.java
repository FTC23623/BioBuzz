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
     *   <li>strokes forward then back {@code autoFlowerStrokeIn} (7 in), {@code autoFlowerPasses}
     *       times (2),
     *   <li>swings the extractor in.
     * </ol>
     * Every move is along the way the robot faces, so it ends {@code autoFlowerBackIn} behind
     * the spot, still facing the flower. Tool: Pose Recorder writes it after a circle press.
     */
    // A third marker, like LAUNCH, so the loop can tell it apart.
    protected static final Path FLOWER = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    // The FLOWER step's state while it runs, with n = how many moves there are:
    // stage 0 backs up, 1 settles at the backed-up spot, 2 swings the arm out, 3 to n + 1
    // are the strokes, n + 2 swings it in, n + 3 is done.
    private List<Pose> mFlowerMoves = new ArrayList<>();
    private Pose mFlowerStart = new Pose(0, 0, 0);
    private int mFlowerStage = 0;
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
        // How long the launchers have been at speed without a break.
        ElapsedTime atSpeedTimer = new ElapsedTime();
        boolean wasAtSpeed = false;
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
            // At speed must hold for a moment, not just touch the target once. Watched every
            // loop, so a dip between launches starts the count over.
            boolean atSpeed = bot.LaunchersAtSpeed();
            if (atSpeed && !wasAtSpeed) {
                atSpeedTimer.reset();
            }
            wasAtSpeed = atSpeed;
            if (spinningUp) {
                boolean steadyAtSpeed = atSpeed && atSpeedTimer.seconds() >= botProperties.autoAtSpeedHoldSeconds;
                if (ReadyToOpen(steadyAtSpeed, stepTimer.seconds(), launchesDone == 0, botProperties)) {
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
                telemetry.addData("Waiting for launchers to reach speed", "yes, %.1f of at most %.1f s%s",
                        stepTimer.seconds(), limit, launchesDone == 0 ? " (first launch)" : "");
            } else {
                telemetry.addData("Waiting for launchers to reach speed", "no");
            }
            telemetry.addData("Launching", !waitingForTag && !aiming && !spinningUp && launching);
            telemetry.addData("Auto time (s)", "%.1f", autoTimer.seconds());
            telemetry.addData("Launchers at speed", bot.LaunchersAtSpeed());
            if (step == FLOWER) {
                telemetry.addData("Flower", "stage %d of %d", mFlowerStage + 1, mFlowerMoves.size() + 3);
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
     * May a LAUNCH step open the gates yet? Yes once the launchers have held at speed for
     * autoAtSpeedHoldSeconds. If they never get there, yes anyway after autoSpinUpSeconds, or
     * after autoFirstSpinUpSeconds for the first launch of the auto, so a slow or broken
     * launcher can't freeze the auto.
     *
     * @param steadyAtSpeed true when at speed has held for the hold time
     * @param waitedSeconds how long this launch has waited so far
     * @param firstLaunch   true for the first launch of this auto
     */
    static boolean ReadyToOpen(boolean steadyAtSpeed, double waitedSeconds, boolean firstLaunch,
                               BotProperties properties) {
        if (steadyAtSpeed) {
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
            mFlowerMoves = FlowerMoves(mFlowerStart, botProperties);
            mFlowerStage = -1;
            bot.RunIntake(true);
            NextFlowerStage(bot, follower);
        } else {
            follower.follow(step);
        }
    }

    /**
     * Where each FLOWER move ends, worked out from where the robot is and the way it faces:
     * first the back-up, then each stroke forward and back. Every pose keeps the heading.
     *
     * <pre>
     *     facing 0 at (0, 0), back 9, strokes of 7, 2 passes:
     *     (-9, 0)  (-2, 0)  (-9, 0)  (-2, 0)  (-9, 0)
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

    /** Start the next FLOWER stage: a move, a settle, or swinging the arm out or in. */
    private void NextFlowerStage(Bot bot, Follower follower) {
        mFlowerStage = mFlowerStage + 1;
        mFlowerTimer.reset();
        int moves = mFlowerMoves.size();
        Pose backedUp = mFlowerMoves.get(0);
        if (mFlowerStage == 1) {
            // Hold the PLANNED backed-up spot, not wherever the robot was when the path
            // ended: it may still be braking short of it.
            follower.hold(backedUp);
        } else if (mFlowerStage == 2) {
            bot.SetFlowerExtractor(true);
            follower.hold(backedUp);
        } else if (mFlowerStage == moves + 2) {
            bot.SetFlowerExtractor(false);
            follower.hold(mFlowerMoves.get(moves - 1));
        } else if (mFlowerStage == 0 || (mFlowerStage >= 3 && mFlowerStage <= moves + 1)) {
            // Stage 0 is move 0 (the back-up); stages 3 to n + 1 are moves 1 to n - 1.
            int move = 0;
            if (mFlowerStage >= 3) {
                move = mFlowerStage - 2;
            }
            Pose from = mFlowerStart;
            if (move > 0) {
                from = mFlowerMoves.get(move - 1);
            }
            Pose to = mFlowerMoves.get(move);
            if (Math.hypot(to.x() - from.x(), to.y() - from.y()) < 0.01) {
                // A zero-inch move (a setting of 0): Pedro won't build it, so skip it.
                NextFlowerStage(bot, follower);
                return;
            }
            Path path = line(from, to).constant(mFlowerStart.heading());
            mFlowerGiveUpSeconds = GiveUpSeconds(path, botProperties, mTopSpeed);
            follower.follow(path);
        }
    }

    /** One loop of the FLOWER step. @return true once the arm is back in. */
    private boolean FlowerStep(Bot bot, Follower follower) {
        int moves = mFlowerMoves.size();
        if (mFlowerStage > moves + 2) {
            return true;
        }
        boolean stageDone;
        if (mFlowerStage == 1) {
            stageDone = mFlowerTimer.seconds() >= botProperties.autoFlowerSettleSeconds;
        } else if (mFlowerStage == 2 || mFlowerStage == moves + 2) {
            stageDone = mFlowerTimer.seconds() >= botProperties.autoExtractorSeconds;
        } else {
            boolean tookTooLong = mFlowerTimer.seconds() >= mFlowerGiveUpSeconds;
            if (follower.following() && tookTooLong) {
                // Stuck on the flower: stop pushing and carry on with the next stroke.
                follower.hold(follower.pose());
            }
            stageDone = !follower.following();
        }
        if (stageDone) {
            NextFlowerStage(bot, follower);
        }
        if (mFlowerStage > moves + 2) {
            // Done. The intake goes back to what the auto does between steps.
            bot.RunIntake(botProperties.autoRunScoring);
            return true;
        }
        return false;
    }
}
