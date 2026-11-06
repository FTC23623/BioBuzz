package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * Drives straight forward one field tile, then stops. The robot starts at (0, 0) facing
 * forward, so one tile forward is x = 24 inches with y staying at 0.
 *
 * <p>The simplest auto there is: one line, no turn. Run it first on a new field or after
 * retuning, to check the robot drives the distance it is told.
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
        double faceForward = 0;
        Pose start = StartPose();
        Pose oneTileAhead = new Pose(tileSize, 0);

        // line(from, to): a straight path. .constant(0): face straight ahead the whole way.
        Path forward = line(start, oneTileAhead).constant(faceForward);

        List<Path> paths = new ArrayList<>();
        paths.add(forward);
        return paths;
    }
}
