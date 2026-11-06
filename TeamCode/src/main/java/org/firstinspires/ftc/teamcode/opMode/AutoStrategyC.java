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

    /**
     * Flower side: front against the top wall, in front of the cell there, on the tile
     * east of the seam (x 46.75), toward the blue alliance wall. The robot is 18 x 18, so its center is
     * 9 in off the wall and 9 in from the seam. Blue; red is turned half a turn (0051).
     */
    @Override
    protected Pose FieldStartBlue() {
        return new Pose(55.75, 132.5, Math.toRadians(90));
    }

    /** The robot starts facing the driver's right (was 90 until 2026-11-05: backwards). */
    @Override
    protected double StartFacingDeg() {
        return -90;
    }


    @Override
    protected List<Path> BuildPaths() {
        Pose spot1 = new Pose(-14.3, 0.3);   // heading -4 deg
        Pose spot2 = new Pose(-4.9, 17.7);   // heading -0 deg
        Pose spot3 = new Pose(-14.0, 8.4);   // heading 1 deg
        Pose spot4 = new Pose(-34.0, 28.4);   // heading -2 deg
        Pose spot5 = new Pose(-85.1, 49.8);   // heading 94 deg
        Pose spot6 = new Pose(-112.2, 9.7);   // heading 178 deg
        Pose spot7 = new Pose(-125.9, 18.7);   // heading 89 deg
        Pose spot8 = new Pose(-124.3, 55.0);   // heading 90 deg
        Pose spot9 = new Pose(-111.2, 6.3);   // heading 177 deg
        Pose spot10 = new Pose(-24.6, 41.2);   // heading 22 deg

        Path toSpot1 = line(StartPose(), spot1).heading(Interpolator.linear(Math.toRadians(0), Math.toRadians(-4)));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-4), Math.toRadians(-0)));
        Path toSpot3 = line(spot2, spot3).constant(Math.toRadians(1));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(1), Math.toRadians(-2)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-2), Math.toRadians(94)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(94), Math.toRadians(178)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(178), Math.toRadians(89)));
        Path toSpot8 = line(spot7, spot8).constant(Math.toRadians(90));
        Path toSpot9 = line(spot7, spot9).heading(Interpolator.linear(Math.toRadians(90), Math.toRadians(177)));
        Path toSpot10 = line(spot9, spot10).heading(Interpolator.linear(Math.toRadians(177), Math.toRadians(22)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot1);
        paths.add(LAUNCH_WHEN_TAG_SEEN);
        paths.add(toSpot2);
        paths.add(FLOWER);
        paths.add(toSpot3);
        paths.add(LAUNCH);
        paths.add(toSpot4);
        paths.add(toSpot5);
        paths.add(FLOWER);
        paths.add(toSpot6);
        paths.add(LAUNCH);
        paths.add(toSpot7);
        paths.add(LAUNCH); // Actually intake
        paths.add(toSpot9);
        paths.add(LAUNCH);
        paths.add(toSpot10);
        return paths;
    }
}
