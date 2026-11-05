package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * A route recorded with Tool: Pose Recorder (docs/decisions/0024): six spots and three
 * LAUNCH stops, driven at full speed.
 *
 * <p>The robot backs up off its start spot and launches, drives a loop out to the far side
 * of the field and launches again, then moves to a third spot and launches once more. Each
 * LAUNCH is a plain stop: the launchers are already spinning from START, so the gates open
 * as soon as they are at speed (docs/decisions/0030). Nothing here waits for a tag.
 *
 * <p>The numbers are the recording, not a plan: to change the route, drive it again with
 * the recorder and paste the new spots here. Start the auto on the exact spot, facing the
 * same way, that the recording started from.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * See {@link PedroAuto} for more.
 */
// No group: the Driver Station lists these first, before the "Backup" ones. At the end it
// picks HyDrive, so the drivers only press INIT and START (docs/decisions/0046).
@Autonomous(name = AutoStrategyC.NAME, preselectTeleOp = HyDrive.NAME)
public class AutoStrategyC extends PedroAuto {
    /** The name on the Driver Station. The backups add their color to it. */
    public static final String NAME = "Flower Side Auto C";

    /** The robot starts facing the driver's left. */
    @Override
    protected double StartFacingDeg() {
        return 90;
    }


    @Override
    protected List<Path> BuildPaths() {
        Pose spot2 = new Pose(-12.7, 0.3);   // heading -1 deg
        Pose spot3 = new Pose(-2.8, -50.7);   // heading -94 deg
        Pose spot4 = new Pose(-30.4, -21.2);   // heading -182 deg
        Pose spot5 = new Pose(-111.1, -19.3);   // heading -157 deg
        Pose spot6 = new Pose(-121.1, -2.1);   // heading -162 deg
        Pose spot7 = new Pose(-103.6, -27.5);   // heading -138 deg

        Path toSpot2 = line(StartPose(), spot2).constant(Math.toRadians(-1));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(-1), Math.toRadians(-94)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-94), Math.toRadians(-182)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-182), Math.toRadians(-157)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-157), Math.toRadians(-162)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(-162), Math.toRadians(-138)));

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
        return paths;
    }
}
