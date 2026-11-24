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
        Pose spot1 = new Pose(-6.9, -10);   // heading -1 deg
        Pose spot2 = new Pose(-10.6, -12.1);   // heading -26 deg
        Pose spot3 = new Pose(-40.1, -40.9);   // heading -91 deg
        Pose spot4 = new Pose(-39.9, -26.2);   // heading -90 deg
        Pose spot5 = new Pose(-17.3, -20.6);   // heading -46 deg
        Pose spot6 = new Pose(-30.0, 4.2);   // heading -182 deg
        Pose spot7 = new Pose(-96.6, 4.3);   // heading -180 deg
        Pose spot8 = new Pose(-120.1, -7.6);   // heading -185 deg
        Pose spot9 = new Pose(-113.1, -6.3);   // heading -171 deg
        Pose spot10 = new Pose(-104.8, -36.0);   // heading -160 deg

        Path toSpot1 = line(StartPose(), spot1).constant(Math.toRadians(-1));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-1), Math.toRadians(-26)));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(-26), Math.toRadians(-91)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-91), Math.toRadians(-90)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-90), Math.toRadians(-46)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-46), Math.toRadians(-182)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(-182), Math.toRadians(-180)));
        Path toSpot8 = line(spot7, spot8).heading(Interpolator.linear(Math.toRadians(-180), Math.toRadians(-185)));
        Path toSpot9 = line(spot8, spot9).heading(Interpolator.linear(Math.toRadians(-185), Math.toRadians(-171)));
        Path toSpot10 = line(spot9, spot10).heading(Interpolator.linear(Math.toRadians(-171), Math.toRadians(-160)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot1);
        paths.add(toSpot2);
        paths.add(LAUNCH);
        paths.add(toSpot3);
        paths.add(INTAKE_ON);
        paths.add(FLOWER);
        paths.add(INTAKE_OFF);
        paths.add(toSpot4);
        paths.add(toSpot5);
        paths.add(LAUNCH);
        paths.add(toSpot6);
        paths.add(toSpot7);
        paths.add(toSpot8);
        paths.add(INTAKE_ON);
        paths.add(FLOWER);
        paths.add(INTAKE_OFF);
        paths.add(toSpot9);
        paths.add(LAUNCH);
        paths.add(toSpot10);
        return paths;
    }
}
