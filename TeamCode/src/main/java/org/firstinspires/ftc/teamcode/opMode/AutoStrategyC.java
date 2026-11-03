package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts on the far side of the field, facing the hive. Creeps forward a few inches, then
 * waits. The hive's AprilTags only show once its basket tips up, so seeing a tag means it
 * tipped. When the camera sees one, the robot aims at the middle of the hive's tags and
 * launches.
 *
 * <p>Use this when our alliance partner can't wait: they launch first, and we launch after
 * the hive tips.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * See {@link PedroAuto} for more.
 */
@Autonomous(name = "Auto: StrategyC", group = "Basic")
public class AutoStrategyC extends PedroAuto {
    /** How far the robot creeps forward off the start, in inches. */
    private static final double creepForwardIn = 3;

    @Override
    protected List<Path> BuildPaths() {
        // Half speed while we check this path on the field. Put it back to 1 for matches.
        Pose spot1 = new Pose(-0.0, 0.0);   // heading 0 deg
        Pose spot2 = new Pose(-12.7, 0.3);   // heading -1 deg
        Pose spot3 = new Pose(-2.8, -50.7);   // heading -94 deg
        Pose spot4 = new Pose(-30.4, -21.2);   // heading -182 deg
        Pose spot5 = new Pose(-111.1, -19.3);   // heading -157 deg
        Pose spot6 = new Pose(-121.1, -2.1);   // heading -162 deg
        Pose spot7 = new Pose(-103.6, -27.5);   // heading -138 deg

//        Path toSpot1 = line(StartPose(), spot1).constant(Math.toRadians(0));
        Path toSpot2 = line(StartPose(), spot2).constant(Math.toRadians(-1));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(-1), Math.toRadians(-94)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-94), Math.toRadians(-182)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-182), Math.toRadians(-157)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-157), Math.toRadians(-162)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(-162), Math.toRadians(-138)));

        List<Path> paths = new ArrayList<>();
//        paths.add(toSpot1);
        paths.add(toSpot2);
        paths.add(LAUNCH);
        paths.add(toSpot3);
        paths.add(toSpot4);
        paths.add(toSpot5);
        paths.add(LAUNCH);
        paths.add(toSpot6);
        paths.add(toSpot7);
        paths.add(LAUNCH);
        return paths;
    }
}
