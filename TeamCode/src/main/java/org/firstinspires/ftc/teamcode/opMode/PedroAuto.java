package org.firstinspires.ftc.teamcode.opMode;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.List;
import org.firstinspires.ftc.teamcode.pedro.PedroConstants;
import org.firstinspires.ftc.teamcode.pedro.ShapePaths;

/**
 * Everything the Pedro autos share. An auto only has to say which paths to drive; this
 * class drives them one after another, then holds the robot still at the end.
 *
 * <p>How following works: {@code follower.follow(path)} starts a path. Then, every loop,
 * {@code follower.update()} reads where the robot is and sets the motor powers. When a path
 * is done, the follower stops "following" and holds the robot in place, and we start the
 * next path. See docs/decisions/0008.
 */
public abstract class PedroAuto extends OpMode_Base {

    /** The paths this auto drives, in order. */
    protected abstract List<Path> BuildPaths();

    @Override
    public void runOpMode() {
        CreateHydraOpMode();

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

        Follower follower = PedroConstants.CreateFollower(hardwareMap, mOpMode.mSafeMode);
        follower.setPose(ShapePaths.startPose);
        List<Path> paths = BuildPaths();
        SetLynxHubsManual();

        while (!isStarted() && !isStopRequested()) {
            mSafeModeKey.ShowStatus();
            telemetry.addLine("Put the robot on its start spot, facing forward.");
            telemetry.addData("Paths to drive", paths.size());
            telemetry.update();
            idle();
        }

        int pathNumber = 0;
        follower.follow(paths.get(pathNumber));

        while (opModeIsActive()) {
            ClearLynxHubCaches();
            follower.update();

            boolean pathFinished = !follower.following();
            boolean morePathsLeft = pathNumber + 1 < paths.size();
            if (pathFinished && morePathsLeft) {
                pathNumber = pathNumber + 1;
                follower.follow(paths.get(pathNumber));
            }

            Pose where = follower.pose();
            telemetry.addData("Path", (pathNumber + 1) + " of " + paths.size());
            telemetry.addData("Done", pathFinished && !morePathsLeft);
            telemetry.addData("x (forward, in)", where.x());
            telemetry.addData("y (left, in)", where.y());
            telemetry.addData("Heading (deg)", Math.toDegrees(where.heading()));
            telemetry.update();
        }

        follower.stop();
    }
}
