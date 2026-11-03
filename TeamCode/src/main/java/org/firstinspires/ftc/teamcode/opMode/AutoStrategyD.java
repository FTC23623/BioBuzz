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
        Pose spot2 = new Pose(-9.8, -0.2);   // heading 1 deg
        Pose spot3 = new Pose(-28.7, -36.5);   // heading -114 deg
        Pose spot4 = new Pose(-106.4, -33.6);   // heading -145 deg
        Pose spot5 = new Pose(-110.4, -14.5);   // heading -158 deg
        Pose spot6 = new Pose(-106.6, -13.2);   // heading -177 deg
        Pose spot7 = new Pose(-111.7, -14.8);   // heading -163 deg
        Pose spot8 = new Pose(-114.6, -43.7);   // heading -137 deg

        Path toSpot2 = line(StartPose(), spot2).constant(Math.toRadians(1));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(1), Math.toRadians(-114)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-114), Math.toRadians(-145)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-145), Math.toRadians(-158)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-158), Math.toRadians(-177)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(-177), Math.toRadians(-163)));
        Path toSpot8 = line(spot7, spot8).heading(Interpolator.linear(Math.toRadians(-163), Math.toRadians(-137)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot2);
        paths.add(LAUNCH);
        paths.add(toSpot3);
        paths.add(toSpot4);
        paths.add(toSpot5);
        paths.add(LAUNCH);
        paths.add(toSpot6);
        paths.add(toSpot7);
        paths.add(LAUNCH);
        paths.add(toSpot8);
        return paths;
    }
}
