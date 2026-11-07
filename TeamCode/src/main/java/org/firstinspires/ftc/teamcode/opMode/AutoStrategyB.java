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
// Group "Auto" (it reads the alliance magnet): the Driver Station lists it before the
// "Blue" and "Red" backups. At the end it picks HyDrive, so the drivers only press INIT and
// START (docs/decisions/0046, 0053).
@Autonomous(name = "Auto: " + AutoStrategyB.NAME, group = "Auto", preselectTeleOp = HyDrive.NAME)
public class AutoStrategyB extends PedroAuto {
    /** The auto's name. On the Driver Station: "Auto: ", "Blue: " or "Red: " in front. */
    public static final String NAME = "Flower Side B";

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
