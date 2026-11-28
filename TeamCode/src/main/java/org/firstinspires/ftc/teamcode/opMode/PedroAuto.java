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
import org.firstinspires.ftc.teamcode.objects.AimAssist;
import org.firstinspires.ftc.teamcode.objects.HeadingHandoff;
import org.firstinspires.ftc.teamcode.pedro.PedroConstants;
import org.firstinspires.ftc.teamcode.subsystems.LaunchGates;
import org.firstinspires.ftc.teamcode.subsystems.LaunchChoice;
import org.firstinspires.ftc.teamcode.types.HiveUp;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;

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
 * <p><b>Scoring</b>: with {@code autoRunScoring} on, the launchers spin and the transfer runs
 * from START to the end. The intake roller stays off except during a FLOWER step
 * ({@code autoIntakeRoller}, docs/decisions/0043), or between an INTAKE_ON and an INTAKE_OFF
 * step (docs/decisions/0047). A LAUNCH step opens both launch gates, and while they are open
 * the intake roller runs too, so it pushes up any ball stuck low: the intake feeds at
 * {@code launchFeedIntakeScale} (80%) and the transfer at {@code launchFeedTransferScale} (80%).
 *
 * <p><b>Tipped or not</b> (docs/decisions/0057): a {@link #CHECK_TIPPED} step looks at the hive,
 * and the steps under {@link #IF_GARDEN_SIDE_UP}, {@link #IF_FLOWER_SIDE_UP},
 * {@link #IF_TIPPED} or {@link #IF_NOT_TIPPED}, up to the {@link #END_IF}, run only for the
 * answer they are under. See {@link #IF_TIPPED} for an example.
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
     * From where the last path ended the robot:
     * <ol>
     *   <li>backs up {@code autoFlowerBackIn} (7 in), and holds that spot
     *       {@code autoFlowerSettleSeconds} (0.3 s) so the robot has stopped,
     *   <li>swings the extractor out, and holds still {@code autoExtractorSeconds} (0.5 s)
     *       before the first stroke,
     *   <li>strokes forward {@code autoFlowerStrokeIn} (9 in), holds there
     *       {@code autoFlowerDwellSeconds} (1 s), strokes back, {@code autoFlowerPasses} times (1),
     *   <li>swings the extractor in.
     * </ol>
     * Every move is along the way the robot faces, so it ends {@code autoFlowerBackIn} behind
     * the spot, still facing the flower. Tool: Pose Recorder writes it after a circle press.
     *
     * <p>The intake runs from the first back-up until the extractor has finished swinging
     * out ({@link #FlowerIntakeOn}). The strokes and the swing back in run with the intake
     * as it was between steps (INTAKE_ON / INTAKE_OFF, docs/decisions/0047).
     */
    // A third marker, like LAUNCH, so the loop can tell it apart.
    protected static final Path FLOWER = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Put this in the list to turn the intake on: the intake roller and the transfer both run
     * from here on, until an {@link #INTAKE_OFF}. It takes no time: the next path starts
     * straight away. Tool: Pose Recorder writes it after a D-pad up press.
     *
     * <pre>
     *     paths.add(toBalls);
     *     paths.add(INTAKE_ON);
     *     paths.add(driveThroughBalls);
     *     paths.add(INTAKE_OFF);
     * </pre>
     */
    // Markers, like LAUNCH, so the loop can tell them apart.
    protected static final Path INTAKE_ON = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Put this in the list to turn the intake roller off again. The transfer keeps running
     * if {@code autoRunScoring} is on, so LAUNCH steps still feed. Takes no time. Tool: Pose
     * Recorder writes it after a D-pad down press.
     */
    protected static final Path INTAKE_OFF = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Put this in the list to swing the flower extractor OUT and carry on at once, so it is
     * open by the time the robot reaches the flower. Takes no time: the next path starts
     * straight away. The FLOWER step that follows sees the arm already out and skips its
     * wait for the swing (docs/decisions/0058).
     *
     * <pre>
     *     paths.add(FLOWER_OUT);
     *     paths.add(toFlower);
     *     paths.add(FLOWER);
     * </pre>
     */
    protected static final Path FLOWER_OUT = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /** Swing the flower extractor back IN and carry on at once. Takes no time. FLOWER does this itself at its end. */
    protected static final Path FLOWER_IN = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Like {@link #LAUNCH}, but only the NECTAR launcher: waits for the nectar wheel to reach
     * {@code autoNectarLaunchMinRpm}, settles it when {@code autoLaunchSettles} is on, then
     * opens only the nectar gate for {@code autoLaunchSeconds}. The pollen gate stays shut.
     */
    protected static final Path LAUNCH_NECTAR = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Like {@link #LAUNCH}, but the gates stay open until every ball switch
     * (pollenSensor1-4, nectorSensor1-4) has read empty for {@code autoEmptyClearSeconds}, or
     * {@code autoLaunchUntilEmptyMaxSeconds} after they opened. With no ball switch in the
     * configuration it runs {@code autoLaunchSeconds}, like LAUNCH.
     */
    protected static final Path LAUNCH_UNTIL_EMPTY = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * Put this in the list to look at the hive: has its basket tipped up? The robot holds still
     * where the last path ended for at most {@code autoTipLookSeconds} (1 s). It counts as
     * tipped the moment the camera sees one of our hive's tags with its roll in the UP
     * window ({@code autoTagUpMinDeg} to {@code autoTagUpMaxDeg}). No such tag by then: not
     * tipped. The tag also says WHICH cell tipped, the garden side's or the flower side's
     * ({@code gardenSideTags}, {@code flowerSideTags}). The answer is kept until the next
     * CHECK_TIPPED, for the IF steps after it. Before any CHECK_TIPPED the answer is "not
     * tipped". Stop with the robot's back (the camera) to the hive, so the camera can see it.
     * Only the nearest tag is looked at.
     *
     * <p>With {@code autoDownMeansOtherSideUp} on, a tag in the DOWN window (the underside of a
     * cell that is down, seen from close and at an angle) also answers: the OTHER side is up.
     * A roll in the buffer between the two windows answers nothing.
     */
    // Markers, like LAUNCH, so the loop can tell them apart.
    protected static final Path CHECK_TIPPED = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * The steps after this run only if the last {@link #CHECK_TIPPED} said the hive tipped.
     * They end at an {@link #IF_NOT_TIPPED} (the steps for "not tipped" follow it) or at an
     * {@link #END_IF}. The auto carries on after the END_IF either way. These markers take no
     * time.
     *
     * <pre>
     *     paths.add(toLaunchSpot);
     *     paths.add(LAUNCH);
     *     paths.add(CHECK_TIPPED);     // look at the hive: has it tipped?
     *     paths.add(IF_TIPPED);
     *     paths.add(toFlower);         //   yes: work the flower, come back
     *     paths.add(FLOWER);
     *     paths.add(backFromFlower);
     *     paths.add(IF_NOT_TIPPED);
     *     paths.add(LAUNCH);           //   no: launch again from here
     *     paths.add(END_IF);
     *     paths.add(toLoadingZone);    // both ways end here
     * </pre>
     *
     * <p>When the route depends on WHICH cell tipped, use the side steps instead:
     *
     * <pre>
     *     paths.add(CHECK_TIPPED);
     *     paths.add(IF_GARDEN_SIDE_UP);
     *     paths.add(toGardenLaunchSpot);   //   the garden side cell is up
     *     paths.add(IF_FLOWER_SIDE_UP);
     *     paths.add(toFlowerLaunchSpot);   //   the flower side cell is up
     *     paths.add(IF_NOT_TIPPED);
     *     paths.add(toWaitingSpot);        //   neither is up
     *     paths.add(END_IF);
     * </pre>
     *
     * <p>Three rules. <b>Every way must end where the robot stands at the END_IF</b>, since the
     * next path starts from there. <b>No IF inside an IF</b>: one level only. And each IF step
     * is asked by itself: IF_TIPPED is true for either side, so don't mix it with the side
     * steps in one IF, or both would run. Any of them may be left out.
     */
    protected static final Path IF_TIPPED = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * The steps after this run only if the last {@link #CHECK_TIPPED} saw nothing of ours
     * tipped. They end at the next IF step or the {@link #END_IF}.
     */
    protected static final Path IF_NOT_TIPPED = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /**
     * The steps after this run only if the last {@link #CHECK_TIPPED} saw the GARDEN side
     * cell tipped up. They end at the next IF step or the {@link #END_IF}. See
     * {@link #IF_TIPPED} for an example.
     */
    protected static final Path IF_GARDEN_SIDE_UP = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /** Like {@link #IF_GARDEN_SIDE_UP}, for the FLOWER side cell. */
    protected static final Path IF_FLOWER_SIDE_UP = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /** Ends the IF steps. The steps after it always run. */
    protected static final Path END_IF = line(new Pose(0, 0), new Pose(1, 0)).constant(0);

    /** The last CHECK_TIPPED's answer: which cell of our hive has tipped up, if any. */
    private HiveUp mHiveUp = HiveUp.NOTHING;
    /** true once a CHECK_TIPPED has run, for the Driver Station. */
    private boolean mTipChecked = false;

    /** true while the flower extractor is out because FLOWER_OUT swung it, until FLOWER_IN or the end of a FLOWER. */
    private boolean mArmOut = false;

    /** true while the intake roller should run between steps: INTAKE_ON / INTAKE_OFF set it. */
    private boolean mRollerOn = false;
    /** true while the intake and transfer should run between steps. */
    private boolean mIntakeRunning = false;

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

    /**
     * Where the robot starts, in the same measure as the auto's paths: (0, 0), facing forward,
     * for an auto measured from its start. An auto whose paths are field spots (copied from
     * the Pedro Pathing Visualizer, like Garden Side E) returns its field start here, so Pedro
     * knows where the robot really is before the first path.
     */
    protected Pose StartPose() {
        return new Pose(0, 0, 0);
    }

    /**
     * The TeleOp heading to hand over (docs/decisions/0046): which way the robot faced at its
     * start, seen from the driver, plus how far Pedro has turned it SINCE THE START. Pedro's
     * heading is measured in the auto's own measure, so the start pose's heading comes off
     * first: 0 for an auto measured from its start, the field heading for a field-spot auto.
     *
     * <pre>
     *     start facing the driver's right (-90), start pose heading 270 (field), now 300:
     *     turned 30 left since the start, so TeleOp yaw -90 + 30 = -60
     * </pre>
     *
     * @param startFacingDeg  {@link #StartFacingDeg()}, seen from the driver
     * @param startPose       {@link #StartPose()}
     * @param pedroHeadingRad Pedro's heading now, radians
     */
    static double HandoffYaw(double startFacingDeg, Pose startPose, double pedroHeadingRad) {
        double turnedDeg = Math.toDegrees(pedroHeadingRad - startPose.heading());
        return HeadingHandoff.TeleOpYaw(startFacingDeg, turnedDeg);
    }

    /**
     * Which way the robot faces on its start spot, seen from the driver, in degrees: 0 away
     * from the driver, 90 to the driver's left, -90 to the driver's right, 180 toward the
     * driver. The driver faces the hive, with the flower side to the left and the garden
     * side to the right, so a garden side auto (front at the garden) is -90 and a flower side
     * auto (front at the flowers) is 90. TeleOp uses it to keep "forward" away from the driver
     * after the auto (docs/decisions/0046). NaN (the default) = not known: TeleOp then zeroes the heading
     * at INIT, as before.
     */
    protected double StartFacingDeg() {
        return Double.NaN;
    }

    /**
     * Where the robot starts on the field, on the BLUE alliance, for the Pedro Pathing
     * Visualizer: inches from the bottom-left corner of its BIOBUZZ field (x right, y up),
     * heading in radians, 0 pointing right (docs/decisions/0051). The walls cover the outer
     * 1.25 in of the edge tiles, so the tile seams are at 22.75, 46.75, 70.75, 94.75 and
     * 118.75 in, and a robot's center against a wall is half its length off it. Red is the same spot with
     * the field turned half a turn. The robot never uses it: only the laptop tool that draws
     * the autos on the field. null (the default) = not placed, not drawn.
     */
    protected Pose FieldStartBlue() {
        return null;
    }

    @Override
    public void runOpMode() {
        // In an auto, Pedro Pathing drives the wheels, so driver control is switched off.
        botProperties.driveDisabled = true;
        // An old heading from an earlier auto must never reach TeleOp (docs/decisions/0046).
        HeadingHandoff.Clear();

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

        // What the intake does between steps, until an INTAKE_ON or INTAKE_OFF changes it.
        mIntakeRunning = botProperties.autoRunScoring;
        mRollerOn = botProperties.autoIntakeRoller;
        if (botProperties.autoRunScoring) {
            // Spin up now, so the launchers are at speed by the first LAUNCH.
            bot.SpinLaunchers(true);
            // The transfer runs the whole auto. The intake roller only if autoIntakeRoller
            // says so; otherwise FLOWER steps turn it on (docs/decisions/0043).
            bot.RunIntake(true);
            bot.SetIntakeRoller(botProperties.autoIntakeRoller);
        }

        // Times how long the robot has been on this step: a path, or a LAUNCH stop.
        ElapsedTime stepTimer = new ElapsedTime();
        // Times the whole auto, from START, for "launch anyway".
        ElapsedTime autoTimer = new ElapsedTime();
        // The first step to run: an IF marker at the top is skipped like any other.
        int pathNumber = NextStep(paths, -1, mHiveUp);
        if (pathNumber >= paths.size()) {
            // Only IF markers in the list: nothing to drive. Step 0 is one; it does nothing.
            pathNumber = 0;
        }
        int pathsGivenUp = 0;
        // true while a LAUNCH_WHEN_TAG_SEEN step is still waiting for the camera.
        boolean waitingForTag = paths.get(pathNumber) == LAUNCH_WHEN_TAG_SEEN;
        // true while a LAUNCH_WHEN_TAG_SEEN step is turning to face the tag it saw.
        boolean aiming = false;
        // The heading that faces the tag, in radians. Picked once, when the tag is first seen.
        double aimHeading = 0;
        // true while a launch step waits for the launchers to reach speed, gates still shut.
        boolean spinningUp = IsPlainLaunch(paths.get(pathNumber));
        // How many launches have opened the gates so far. The first one waits longer.
        int launchesDone = 0;
        // true while a launch, already fast enough, waits for the speed to settle
        // (autoLaunchSettles, docs/decisions/0056).
        boolean settling = false;
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
                // Aim a little off the hive's middle: the side offset for where the robot
                // stands (docs/decisions/0044) plus the pollen launcher's offset.
                aimHeading = AimHeading(here.heading(), bot.GetTagGroupBearing(),
                        bot.GetHiveSideDegrees(), bot.GetTagRange(), botProperties);
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
                boolean nectarOnly = step == LAUNCH_NECTAR;
                boolean fastEnough;
                if (nectarOnly) {
                    fastEnough = bot.NectarLauncherAtLeast(botProperties.autoNectarLaunchMinRpm);
                } else {
                    fastEnough = bot.PollenLauncherAtLeast(botProperties.autoLaunchMinRpm);
                }
                boolean openNow = false;
                if (settling) {
                    // Speed locked: open once it has settled, or the settle timed out.
                    boolean settled = nectarOnly ? bot.NectarLaunchSettled() : bot.LaunchSettled();
                    if (settled) {
                        settling = false;
                        if (nectarOnly) {
                            bot.CancelNectarLaunchSettle();
                        } else {
                            bot.CancelLaunchSettle();
                        }
                        openNow = true;
                    }
                } else if (ReadyToOpen(fastEnough, stepTimer.seconds(), launchesDone == 0, botProperties)) {
                    if (SettlesBeforeLaunch(step, botProperties)) {
                        // Fast enough: now lock the speed and let it settle (0056).
                        settling = true;
                        if (nectarOnly) {
                            bot.BeginNectarLaunchSettle();
                        } else {
                            bot.BeginLaunchSettle();
                        }
                    } else {
                        openNow = true;
                    }
                }
                if (openNow) {
                    // Up to speed (or waited as long as allowed): open the gates. The launch
                    // time counts from now. The rollers start once the gates are fully open.
                    spinningUp = false;
                    launchesDone = launchesDone + 1;
                    bot.OpenLaunchGates(true, !nectarOnly, true);
                    // The intake roller feeds the launch too, even where it is off between
                    // steps, so a ball stuck low is pushed up. Back to before at the end.
                    bot.SetIntakeRoller(true);
                    stepTimer.reset();
                }
            }
            boolean launching = IsLaunch(step);
            boolean pathFinished;
            if (waitingForTag || aiming || spinningUp) {
                pathFinished = false;
            } else if (step == FLOWER) {
                pathFinished = FlowerStep(bot, follower);
            } else if (step == INTAKE_ON || step == INTAKE_OFF || step == FLOWER_OUT || step == FLOWER_IN) {
                // Done the moment it starts: StartStep already switched the intake or the arm.
                pathFinished = true;
            } else if (step == CHECK_TIPPED) {
                // Look at the hive until a tipped tag is seen, or the look time is up.
                HiveUp seen = bot.SeesHiveUp();
                if (seen.Tipped()) {
                    mHiveUp = seen;
                    mTipChecked = true;
                    pathFinished = true;
                } else if (stepTimer.seconds() >= botProperties.autoTipLookSeconds) {
                    mHiveUp = HiveUp.NOTHING;
                    mTipChecked = true;
                    pathFinished = true;
                } else {
                    pathFinished = false;
                }
            } else if (IsBranchMarker(step)) {
                // The IF steps and END_IF: NextStep() skips them. Only here when the
                // list holds nothing else.
                pathFinished = true;
            } else if (launching) {
                pathFinished = LaunchDone(step, stepTimer.seconds(), bot.BallsEmptyForSeconds(),
                        bot.BallSensorsFound(), botProperties);
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
                // The launch time is up: close the gates and stop flashing the light. The
                // intake roller goes back to what it did before the launch.
                bot.ShowLaunching(false);
                bot.OpenLaunchGates(false);
                bot.SetIntakeRoller(mRollerOn);
            }
            // The next step, skipping the IF steps that don't match the hive.
            int nextStep = NextStep(paths, pathNumber, mHiveUp);
            boolean morePathsLeft = nextStep < paths.size();
            if (pathFinished && morePathsLeft) {
                pathNumber = nextStep;
                waitingForTag = paths.get(pathNumber) == LAUNCH_WHEN_TAG_SEEN;
                spinningUp = IsPlainLaunch(paths.get(pathNumber));
                StartStep(bot, follower, paths.get(pathNumber), stepTimer);
            }

            // An auto has no gamepads, so there is no HandleUserInput() here.
            bot.Process();

            Pose where = follower.pose();
            // Every loop, so TeleOp gets the heading from the moment the auto stops.
            if (!Double.isNaN(StartFacingDeg())) {
                HeadingHandoff.Save(HandoffYaw(StartFacingDeg(), StartPose(), where.heading()),
                        HeadingHandoff.NowSeconds());
            }
            // Grouped, so each block can be switched off on the dashboard (Telemetry group).
            if (bot.TelemetryOn(TelemetryGroup.AUTO_STEP)) {
                telemetry.addData("Step", "%d of %d: %s", pathNumber + 1, paths.size(),
                        StepText(step, where));
                int upNext = NextStep(paths, pathNumber, mHiveUp);
                if (upNext < paths.size()) {
                    telemetry.addData("Next step", "%d: %s", upNext + 1, StepText(paths.get(upNext), null));
                } else {
                    telemetry.addData("Next step", "none: the last step");
                }
                if (step == FLOWER) {
                    String doing = "done";
                    if (mFlowerIndex >= 0 && mFlowerIndex < mFlowerActions.size()) {
                        doing = mFlowerActions.get(mFlowerIndex).name;
                    }
                    telemetry.addData("Flower", "%d of %d: %s", mFlowerIndex + 1, mFlowerActions.size(), doing);
                } else if (!launching && !IsMarker(step)) {
                    telemetry.addData("Give up on this step after (s)", "%.1f (now %.1f)",
                            giveUpSeconds[pathNumber], stepTimer.seconds());
                }
                telemetry.addData("Steps given up (took too long)", pathsGivenUp);
                telemetry.addData("Done", pathFinished && !morePathsLeft);
                telemetry.addData("Auto time (s)", "%.1f", autoTimer.seconds());
            }
            if (bot.TelemetryOn(TelemetryGroup.AUTO_LAUNCH)) {
                telemetry.addData("Waiting for a tag", waitingForTag);
                telemetry.addData("Aiming at the tag", aiming);
                if (settling) {
                    telemetry.addData("Waiting for launchers to reach speed", "settling: %s",
                            step == LAUNCH_NECTAR ? bot.NectarLaunchSettleStatus() : bot.LaunchSettleStatus());
                } else if (spinningUp) {
                    double limit = botProperties.autoSpinUpSeconds;
                    if (launchesDone == 0) {
                        limit = botProperties.autoFirstSpinUpSeconds;
                    }
                    boolean nectarOnly = step == LAUNCH_NECTAR;
                    telemetry.addData("Waiting for launchers to reach speed", "yes, %s %.0f of %.0f RPM, %.1f of at most %.1f s%s",
                            nectarOnly ? "nectar" : "pollen",
                            nectarOnly ? bot.NectarLauncherRpm() : bot.PollenLauncherRpm(),
                            nectarOnly ? botProperties.autoNectarLaunchMinRpm : botProperties.autoLaunchMinRpm,
                            stepTimer.seconds(), limit, launchesDone == 0 ? " (first launch)" : "");
                } else {
                    telemetry.addData("Waiting for launchers to reach speed", "no");
                }
                telemetry.addData("Launching", !waitingForTag && !aiming && !spinningUp && launching);
                if (step == LAUNCH_UNTIL_EMPTY && !spinningUp) {
                    telemetry.addData("Balls left", "%d on the switches, empty for %.2f of %.2f s",
                            bot.BallsSeen(), bot.BallsEmptyForSeconds(), botProperties.autoEmptyClearSeconds);
                }
                if (launching || aiming || waitingForTag) {
                    // How far the hive is from the middle of the camera: 0 is centered.
                    telemetry.addData("Hive bearing off center (deg)", "%.1f (aim done within %.1f)",
                            bot.GetTagGroupBearing(), botProperties.aimDoneDegrees);
                }
                if (step == CHECK_TIPPED) {
                    telemetry.addData("Hive tipped", "looking, %.1f of at most %.1f s",
                            stepTimer.seconds(), botProperties.autoTipLookSeconds);
                } else if (mTipChecked) {
                    telemetry.addData("Hive tipped", HiveUpText(mHiveUp));
                } else {
                    telemetry.addData("Hive tipped", "not checked yet");
                }
                telemetry.addData("Launchers at speed", bot.LaunchersAtSpeed());
                telemetry.addData("Flower arm", mArmOut ? "out" : "in");
            }
            if (bot.TelemetryOn(TelemetryGroup.POSE)) {
                telemetry.addData("x (forward, in)", where.x());
                telemetry.addData("y (left, in)", where.y());
                telemetry.addData("Heading (deg)", Math.toDegrees(where.heading()));
                if (!Double.isNaN(StartFacingDeg())) {
                    // What TeleOp will be told: 0 = facing away from the driver, 90 = to the left.
                    telemetry.addData("TeleOp yaw to hand over (deg)",
                            HandoffYaw(StartFacingDeg(), StartPose(), where.heading()));
                }
            }
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
     * The heading that aims the pollen launcher at the hive, from the heading now and the
     * hive's bearing in the camera: turn by the bearing, less where the hive should sit once
     * aimed for pollen ({@link LaunchChoice#AimAtDeg}: the side offset plus the pollen
     * launcher's offset from the camera). Radians in and out, the bearing and side in
     * degrees, the range in inches (0 = not known).
     */
    static double AimHeading(double headingNow, double hiveBearingDeg, double sideDeg,
                             double rangeIn, BotProperties properties) {
        double aimAt = LaunchChoice.POLLEN.AimAtDeg(sideDeg, rangeIn, properties);
        double turnDeg = AimAssist.AimError(hiveBearingDeg, aimAt);
        return headingNow + Math.toRadians(turnDeg);
    }

    /**
     * Does this launch step lock the speed and wait for it to settle before the gates open
     * (docs/decisions/0056)? Every launch with autoLaunchSettles on; LAUNCH_WHEN_TAG_SEEN
     * also with autoTagLaunchSettles on, since the tag has just set a new target.
     */
    static boolean SettlesBeforeLaunch(Path step, BotProperties properties) {
        if (properties.autoLaunchSettles) {
            return true;
        }
        return step == LAUNCH_WHEN_TAG_SEEN && properties.autoTagLaunchSettles;
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

    /** true for the IF steps and END_IF: steps that only choose what runs next. */
    static boolean IsBranchMarker(Path step) {
        return step == IF_TIPPED || step == IF_NOT_TIPPED || step == IF_GARDEN_SIDE_UP
                || step == IF_FLOWER_SIDE_UP || step == END_IF;
    }

    /** Is this IF step true for what the last CHECK_TIPPED saw? */
    static boolean IfMatches(Path ifStep, HiveUp hiveUp) {
        if (ifStep == IF_TIPPED) {
            return hiveUp.Tipped();
        }
        if (ifStep == IF_NOT_TIPPED) {
            return hiveUp == HiveUp.NOTHING;
        }
        if (ifStep == IF_GARDEN_SIDE_UP) {
            return hiveUp == HiveUp.GARDEN_SIDE;
        }
        if (ifStep == IF_FLOWER_SIDE_UP) {
            return hiveUp == HiveUp.FLOWER_SIDE;
        }
        return false;
    }

    /** The last CHECK_TIPPED's answer, for the Driver Station. */
    static String HiveUpText(HiveUp hiveUp) {
        if (hiveUp == HiveUp.GARDEN_SIDE) {
            return "YES, garden side";
        }
        if (hiveUp == HiveUp.FLOWER_SIDE) {
            return "YES, flower side";
        }
        if (hiveUp == HiveUp.UNKNOWN_SIDE) {
            return "YES, side not known (tag on neither side's list)";
        }
        return "no";
    }

    /** true for any marker step: one the robot never drives (LAUNCH, FLOWER, IF_TIPPED, ...). */
    static boolean IsMarker(Path step) {
        return IsLaunch(step) || step == FLOWER || step == FLOWER_OUT || step == FLOWER_IN
                || step == INTAKE_ON || step == INTAKE_OFF || step == CHECK_TIPPED
                || IsBranchMarker(step);
    }

    /**
     * What a step is, for the Driver Station: a marker's name, or where a drive goes.
     *
     * @param where where the robot is now, to show how far is left. null: leave that out.
     */
    static String StepText(Path step, Pose where) {
        if (step == LAUNCH) {
            return "LAUNCH";
        } else if (step == LAUNCH_WHEN_TAG_SEEN) {
            return "LAUNCH_WHEN_TAG_SEEN";
        } else if (step == LAUNCH_NECTAR) {
            return "LAUNCH_NECTAR";
        } else if (step == LAUNCH_UNTIL_EMPTY) {
            return "LAUNCH_UNTIL_EMPTY";
        } else if (step == FLOWER) {
            return "FLOWER";
        } else if (step == FLOWER_OUT) {
            return "FLOWER_OUT";
        } else if (step == FLOWER_IN) {
            return "FLOWER_IN";
        } else if (step == INTAKE_ON) {
            return "INTAKE_ON";
        } else if (step == INTAKE_OFF) {
            return "INTAKE_OFF";
        } else if (step == CHECK_TIPPED) {
            return "CHECK_TIPPED";
        } else if (IsBranchMarker(step)) {
            return "IF marker";
        }
        Pose end = step.endPose();
        String text = String.format("drive to (%.1f, %.1f) facing %.0f deg",
                end.x(), end.y(), Math.toDegrees(end.heading()));
        if (where != null) {
            text = text + String.format(", %.1f in left", Math.hypot(end.x() - where.x(), end.y() - where.y()));
        }
        return text;
    }

    /** true for a launch that stops where the robot is and spins up first: LAUNCH, LAUNCH_NECTAR, LAUNCH_UNTIL_EMPTY. */
    static boolean IsPlainLaunch(Path step) {
        return step == LAUNCH || step == LAUNCH_NECTAR || step == LAUNCH_UNTIL_EMPTY;
    }

    /** true for any launch step, with or without waiting for a tag. */
    static boolean IsLaunch(Path step) {
        return IsPlainLaunch(step) || step == LAUNCH_WHEN_TAG_SEEN;
    }

    /**
     * Is this launch over? A LAUNCH_UNTIL_EMPTY ends once every ball switch has read empty for
     * {@code autoEmptyClearSeconds}, or after {@code autoLaunchUntilEmptyMaxSeconds}. With no
     * ball switch in the robot it is a plain launch, so it can't wait for ever. Every other
     * launch ends after {@code autoLaunchSeconds}.
     *
     * @param secondsOpen how long since the gates opened
     * @param emptyFor    how long every ball switch has read empty, in a row
     */
    static boolean LaunchDone(Path step, double secondsOpen, double emptyFor,
                              boolean sensorsFound, BotProperties p) {
        if (step != LAUNCH_UNTIL_EMPTY || !sensorsFound) {
            return secondsOpen >= p.autoLaunchSeconds;
        }
        // Not before the gates have swung open: balls can't have gone yet.
        boolean gatesOpen = secondsOpen >= LaunchGates.OpenSeconds(p);
        return (gatesOpen && emptyFor >= p.autoEmptyClearSeconds) || secondsOpen >= p.autoLaunchUntilEmptyMaxSeconds;
    }

    /**
     * Which step runs after step {@code from}, given the hive's answer. IF steps are never
     * run themselves: they only choose. The steps under an IF step, up to the next IF step
     * or END_IF, run when that IF step is true for the answer ({@link #IfMatches}).
     *
     * <pre>
     *     0 CHECK_TIPPED  1 IF_TIPPED  2 toFlower  3 IF_NOT_TIPPED  4 LAUNCH  5 END_IF  6 toEnd
     *     tipped:     after 0 comes 2; after 2 comes 6 (the not-tipped steps are skipped)
     *     not tipped: after 0 comes 4; after 4 comes 6
     * </pre>
     *
     * @param from   the step that just finished. -1 for the first step of the auto.
     * @param hiveUp the last CHECK_TIPPED's answer
     * @return the next step's number, or {@code paths.size()} when no steps are left
     */
    static int NextStep(List<Path> paths, int from, HiveUp hiveUp) {
        int i = from + 1;
        while (i < paths.size()) {
            Path step = paths.get(i);
            if (step == END_IF) {
                i = i + 1;
            } else if (IsBranchMarker(step)) {
                if (IfMatches(step, hiveUp)) {
                    i = i + 1;   // into this IF step's steps
                } else {
                    // Skip its steps: on to the next IF step, or the END_IF.
                    i = NextBranchMarker(paths, i);
                }
            } else {
                return i;
            }
        }
        return paths.size();
    }

    /**
     * The next IF step or END_IF after step {@code from}. None found: {@code paths.size()},
     * so the rest of the list is skipped.
     */
    private static int NextBranchMarker(List<Path> paths, int from) {
        for (int i = from + 1; i < paths.size(); i++) {
            if (IsBranchMarker(paths.get(i))) {
                return i;
            }
        }
        return paths.size();
    }

    /** Start one step from the list: drive a path, stop and launch, or stop and wait for a tag. */
    private void StartStep(Bot bot, Follower follower, Path step, ElapsedTime stepTimer) {
        stepTimer.reset();
        if (step == LAUNCH) {
            // Nothing new to follow, so the follower keeps holding the robot where it is.
            // Blink the light yellow. The gates open once the launchers are at speed.
            bot.ShowLaunching(true);
        } else if (step == LAUNCH_NECTAR || step == LAUNCH_UNTIL_EMPTY) {
            // Like LAUNCH: hold still, blink yellow. The gates open once the wheel is ready.
            bot.ShowLaunching(true);
        } else if (step == LAUNCH_WHEN_TAG_SEEN) {
            // Hold still, like LAUNCH, but blink orange until the camera sees a tag.
            bot.ShowWaitingForTag(true);
        } else if (step == FLOWER) {
            // Work the flower from here. NextFlowerAction turns the intake on for the back-up
            // and the arm swinging out.
            mFlowerStart = follower.pose();
            mFlowerActions = FlowerActions(mFlowerStart, botProperties, mArmOut);
            mFlowerIndex = -1;
            NextFlowerAction(bot, follower);
        } else if (step == FLOWER_OUT || step == FLOWER_IN) {
            // Swing the arm and carry on: the servo moves while the robot drives.
            mArmOut = step == FLOWER_OUT;
            bot.SetFlowerExtractor(mArmOut);
        } else if (step == INTAKE_ON) {
            // Roller and transfer on. The follower keeps holding the robot where it is.
            mIntakeRunning = true;
            mRollerOn = true;
            bot.RunIntake(true);
            bot.SetIntakeRoller(true);
        } else if (step == INTAKE_OFF) {
            // Roller off. The transfer goes back to what the auto does (autoRunScoring).
            mIntakeRunning = botProperties.autoRunScoring;
            mRollerOn = false;
            bot.RunIntake(mIntakeRunning);
            bot.SetIntakeRoller(false);
        } else if (step == CHECK_TIPPED || IsBranchMarker(step)) {
            // Nothing new to follow: the follower keeps holding the robot where it is.
            // CHECK_TIPPED looks at the hive in the loop.
        } else {
            follower.follow(step);
        }
    }

    /**
     * Where each FLOWER move ends, worked out from where the robot is and the way it faces:
     * first the back-up, then each stroke forward and back. Every pose keeps the heading.
     *
     * <pre>
     *     facing 0 at (0, 0), back 6, strokes of 8, 2 passes (today's 1 pass stops after the third):
     *     (-6, 0)  (2, 0)  (-6, 0)  (2, 0)  (-6, 0)
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
     *     move: back up                 autoFlowerBackIn (7)       intake ON from here...
     *     hold: settle                  autoFlowerSettleSeconds (0.3)
     *     arm out                       autoExtractorSeconds (0.5)  ...to here
     *     move: stroke forward          autoFlowerStrokeIn (9)
     *     hold: at the flower           autoFlowerDwellSeconds (1)
     *     move: stroke back             autoFlowerStrokeIn (9)
     *     ... again, if autoFlowerPasses (1) is more than 1 ...
     *     arm in                        autoExtractorSeconds (0.5)
     * </pre>
     */
    static List<FlowerAction> FlowerActions(Pose spot, BotProperties p) {
        return FlowerActions(spot, p, false);
    }

    /**
     * The same, with {@code armAlreadyOut} true when a FLOWER_OUT swung the arm earlier: the
     * "arm out" action then takes no time, so the robot goes straight into the first stroke.
     */
    static List<FlowerAction> FlowerActions(Pose spot, BotProperties p, boolean armAlreadyOut) {
        List<Pose> moves = FlowerMoves(spot, p);
        List<FlowerAction> actions = new ArrayList<>();
        Pose backedUp = moves.get(0);
        actions.add(new FlowerAction(FlowerAction.MOVE, backedUp, 0, "back up"));
        actions.add(new FlowerAction(FlowerAction.HOLD, backedUp, p.autoFlowerSettleSeconds, "settle"));
        if (armAlreadyOut) {
            actions.add(new FlowerAction(FlowerAction.ARM_OUT, backedUp, 0, "arm already out"));
        } else {
            actions.add(new FlowerAction(FlowerAction.ARM_OUT, backedUp, p.autoExtractorSeconds, "arm out"));
        }
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

    /**
     * Is the intake on for this FLOWER action? On from the first back-up until the arm has
     * finished swinging out: the actions up to and including "arm out". After that it goes
     * back to what the auto does between steps.
     */
    static boolean FlowerIntakeOn(List<FlowerAction> actions, int index) {
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).kind == FlowerAction.ARM_OUT) {
                return index <= i;
            }
        }
        return true;
    }

    /** Start the next FLOWER action. */
    private void NextFlowerAction(Bot bot, Follower follower) {
        mFlowerIndex = mFlowerIndex + 1;
        mFlowerTimer.reset();
        if (mFlowerIndex >= mFlowerActions.size()) {
            return;
        }
        if (FlowerIntakeOn(mFlowerActions, mFlowerIndex)) {
            bot.RunIntake(true);
            bot.SetIntakeRoller(true);
        } else {
            // The arm is out: the intake goes back to what the auto does between steps.
            bot.RunIntake(mIntakeRunning);
            bot.SetIntakeRoller(mRollerOn);
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
            mArmOut = true;
        } else if (action.kind == FlowerAction.ARM_IN) {
            bot.SetFlowerExtractor(false);
            mArmOut = false;
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
            // Done. The intake goes back to what it did before the flower: the start
            // settings, or the last INTAKE_ON / INTAKE_OFF.
            bot.RunIntake(mIntakeRunning);
            bot.SetIntakeRoller(mRollerOn);
            return true;
        }
        return false;
    }
}
