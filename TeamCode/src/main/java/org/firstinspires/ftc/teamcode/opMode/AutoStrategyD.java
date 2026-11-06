package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * A route recorded with Tool: Pose Recorder (docs/decisions/0024): seven spots and three
 * LAUNCH stops, driven at full speed.
 *
 * <p>The robot backs up off its start spot and launches, drives out to the far side of the
 * field and launches again, shuffles to a third spot and launches once more, then drives
 * on to a last spot. Each LAUNCH is a plain stop: the launchers are already spinning from
 * START, so the gates open as soon as they are at speed (docs/decisions/0030). Nothing
 * here waits for a tag.
 *
 * <p>The numbers are the recording, not a plan: to change the route, drive it again with
 * the recorder and paste the new spots here. Start the auto on the exact spot, facing the
 * same way, that the recording started from.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * See {@link PedroAuto} for more.
 */
@Autonomous(name = "Auto: StrategyD", group = "Basic")
public class AutoStrategyD extends PedroAuto {

    @Override
    protected List<Path> BuildPaths() {
        Pose spot1 = new Pose(-11.0, -0.1);   // heading -1 deg
        Pose spot2 = new Pose(-38.5, -27.6);   // heading -89 deg
        Pose spot3 = new Pose(-38.7, -41.8);   // heading -91 deg
        Pose spot4 = new Pose(-35.9, -28.1);   // heading -88 deg
        Pose spot5 = new Pose(-111.1, -20.6);   // heading -185 deg
        Pose spot6 = new Pose(-111.1, 5.1);   // heading -184 deg
        Pose spot7 = new Pose(-119.6, -6.2);   // heading -177 deg
        Pose spot8 = new Pose(-116.3, 3.8);   // heading -180 deg
        Pose spot9 = new Pose(-106.3, -31.2);   // heading -159 deg

        Path toSpot1 = line(StartPose(), spot1).constant(Math.toRadians(-1));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-1), Math.toRadians(-89)));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(-89), Math.toRadians(-91)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-91), Math.toRadians(-88)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-88), Math.toRadians(-185)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-185), Math.toRadians(-184)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(-184), Math.toRadians(-177)));
        Path toSpot8 = line(spot7, spot8).heading(Interpolator.linear(Math.toRadians(-177), Math.toRadians(-180)));
        Path toSpot9 = line(spot8, spot9).heading(Interpolator.linear(Math.toRadians(-180), Math.toRadians(-159)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot1);
        paths.add(LAUNCH);
        paths.add(toSpot2);
        paths.add(toSpot3);
        paths.add(FLOWER);
        paths.add(toSpot4);
        paths.add(toSpot5);
        paths.add(toSpot6);
        paths.add(LAUNCH);
        paths.add(toSpot7);
        paths.add(FLOWER);
        paths.add(toSpot8);
        paths.add(LAUNCH);
        paths.add(toSpot9);
        return paths;
    }
}
