package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathSegment;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
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
     * real path: when the auto reaches it, the robot holds still where the last path ended
     * for {@code botProperties.autoLaunchSeconds}, then drives the next path. The indicator
     * light blinks yellow during the stop and turns off after it.
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
        // No auto uses the launcher yet.
        botProperties.launcherDisabled = true;

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
            bot.EndLoop();
        }

        // Times how long the robot has been on this step: a path, or a LAUNCH stop.
        ElapsedTime stepTimer = new ElapsedTime();
        int pathNumber = 0;
        int pathsGivenUp = 0;
        StartStep(bot, follower, paths.get(pathNumber), stepTimer);

        while (opModeIsActive()) {
            bot.StartLoop();
            follower.update();

            boolean launching = paths.get(pathNumber) == LAUNCH;
            boolean pathFinished;
            if (launching) {
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
                // The launch time is up: stop flashing the light.
                bot.ShowLaunching(false);
            }
            boolean morePathsLeft = pathNumber + 1 < paths.size();
            if (pathFinished && morePathsLeft) {
                pathNumber = pathNumber + 1;
                StartStep(bot, follower, paths.get(pathNumber), stepTimer);
            }

            // An auto has no gamepads, so there is no HandleUserInput() here.
            bot.Process();

            Pose where = follower.pose();
            telemetry.addData("Path", (pathNumber + 1) + " of " + paths.size());
            telemetry.addData("Launching", paths.get(pathNumber) == LAUNCH);
            if (!launching) {
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
        double headingChange = path.endPose().heading() - path.get(0).heading();
        headingChange = Math.atan2(Math.sin(headingChange), Math.cos(headingChange));
        double quarterTurns = Math.abs(headingChange) / Math.toRadians(90);
        double turnSeconds = quarterTurns * properties.autoQuarterTurnSeconds;

        // A slower auto needs more time for the same path.
        double seconds = (driveSeconds + turnSeconds) / properties.autoSpeedScale;
        return seconds * properties.autoGiveUpMargin + properties.autoGiveUpExtraSeconds;
    }

    /** Start one step from the list: drive a path, or stop and launch. */
    private void StartStep(Bot bot, Follower follower, Path step, ElapsedTime stepTimer) {
        stepTimer.reset();
        if (step == LAUNCH) {
            // Nothing new to follow, so the follower keeps holding the robot where it is.
            // The launcher is still off in autos, so the indicator light blinks yellow instead.
            bot.ShowLaunching(true);
        } else {
            follower.follow(step);
        }
    }
}
