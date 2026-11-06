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
// No group: the Driver Station lists these first, before the "Backup" ones. At the end it
// picks HyDrive, so the drivers only press INIT and START (docs/decisions/0046).
@Autonomous(name = AutoStrategyB.NAME, preselectTeleOp = HyDrive.NAME)
public class AutoStrategyB extends PedroAuto {
    /** The name on the Driver Station. The backups add their color to it. */
    public static final String NAME = "Flower Side Auto B";

    /** The robot starts facing the driver's right (was 90 until 2026-11-05: backwards). */
    @Override
    protected double StartFacingDeg() {
        return -90;
    }

    /** How far the robot creeps forward off the start, in inches. */
    private static final double creepForwardIn = 3;

    @Override
    protected List<Path> BuildPaths() {
        Pose spot1 = new Pose(-15.4, 1.1);   // heading -12 deg
        Pose spot2 = new Pose(-4.8, 16.0);   // heading -2 deg
        Pose spot3 = new Pose(-11.3, 16.6);   // heading 12 deg
        Pose spot4 = new Pose(-14.4, 47.9);   // heading 29 deg

        Path toSpot1 = line(StartPose(), spot1).heading(Interpolator.linear(Math.toRadians(0), Math.toRadians(-12)));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-12), Math.toRadians(-2)));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(-2), Math.toRadians(12)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(12), Math.toRadians(29)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot1);
        paths.add(LAUNCH_WHEN_TAG_SEEN);
        paths.add(toSpot2);
        paths.add(FLOWER);
        paths.add(toSpot3);
        paths.add(LAUNCH);
        paths.add(toSpot4);
        return paths;
    }
}
