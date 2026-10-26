package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives straight forward one field tile, then stops. The robot starts at (0, 0) facing
 * forward, so one tile forward is x = 24 inches with y staying at 0.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * See {@link PedroAuto} for more.
 */
@Autonomous(name = "Forward: One Tile", group = "Basic")
public class AutoForwardOneTile extends PedroAuto {
    /** One field tile, in inches. */
    private static final double tileSize = 24;

    @Override
    protected List<Path> BuildPaths() {
        double turnLeft = Math.toRadians(90);
        Pose start = StartPose();
        Pose twoTilesLeft = new Pose(0, 2 * tileSize);    // Step 1: turn in place from heading 0 to 90 degrees (left), without moving.
        Path turn = line(start, start).heading(Interpolator.linear(0, turnLeft));    // Step 2: drive two tiles in the direction now faced, keeping that heading.
        Path driveLeft = line(start, twoTilesLeft).constant(turnLeft);
        List<Path> paths = new ArrayList<>();
        paths.add(turn);
        paths.add(driveLeft);
        return paths;
//        double faceForward = 0;
//        Pose start = StartPose();
//        Pose oneTileAhead = new Pose(tileSize, 0);
//
//        // line(from, to): a straight path. .constant(0): face straight ahead the whole way.
//        Path forward = line(start, oneTileAhead).constant(faceForward);
//
//        List<Path> paths = new ArrayList<>();
//        paths.add(forward);
//        return paths;
    }
}
