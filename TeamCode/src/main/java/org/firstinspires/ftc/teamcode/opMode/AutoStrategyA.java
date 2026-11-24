package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts with its front (the intake) on the wall. Backs up just far enough to spin without a
 * corner hitting the wall, launches, turns right 90, then puts its left side on the wall and
 * drives along it to collect pollen. Then it moves off the wall, turns right again to face the
 * middle of the field, and drives on toward the far side. Last, it slides right, turns left
 * 20 degrees to look at the hive's AprilTags, waits until it sees one, aims at the middle of
 * the hive's tags, and launches.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * The robot starts facing the wall, so backing up is minus x and the garden (to its right) is
 * minus y. See {@link PedroAuto} for more.
 */
// Group "Auto" (it reads the alliance magnet): the Driver Station lists it before the
// "Blue" and "Red" backups. At the end it picks HyDrive, so the drivers only press INIT and
// START (docs/decisions/0046, 0053).
@Autonomous(name = "Auto: " + AutoStrategyA.NAME, group = "Auto", preselectTeleOp = HyDrive.NAME)
public class AutoStrategyA extends PedroAuto {
    /** The auto's name. On the Driver Station: "Auto: ", "Blue: " or "Red: " in front. */
    public static final String NAME = "Garden Side A";

    /**
     * Garden side: front against the bottom wall, on the tile east of the seam at x 46.75
     * (coach, 2026-11-06: two tiles left of the first guess at 94.75). The robot is 18 x 18,
     * so its center is 9 in off the wall and 9 in from the seam. Blue; red is turned half a
     * turn (0051).
     */
    @Override
    protected Pose FieldStartBlue() {
        return new Pose(55.75, 9, Math.toRadians(-90));
    }

    /**
     * The robot's front (the intake) points to the driver's RIGHT (-90), at the garden side.
     * The driver faces the hive, flowers to the left, garden to the right. The back (camera
     * and launchers) faces the hive cell that tips up. Coach, 2026-11-24.
     */
    @Override
    protected double StartFacingDeg() {
        return -90;
    }

    /** How far right the robot slides while it turns, in inches. */
    private static final double turnSlideIn = 3;
    /** How far right of the start the pollen run starts, in inches. */
    private static final double collectStartIn = 24;
    /** How far right of the start the pollen run ends, in inches. */
    private static final double collectEndIn = 48;
    /** How far the robot drives toward the far side of the field, in inches. Clears the hive stand. */
    private static final double driveToMiddleIn = 102;
    /** How far the robot slides back toward the start while turning, to miss the flower. */
    private static final double flowerClearanceIn = 12;
    /** How far right the robot slides before it aims at the tag, in inches. */
    private static final double strafeRightIn = 24;
    /** Part of that slide happens during the aiming turn, since Pedro can't turn in place. */
    private static final double aimTurnSlideIn = 2;
    /** How far LEFT the robot turns to see the AprilTag, in degrees. Left is toward the hive. */
    private static final double aimTurnDeg = 20;

    @Override
    protected List<Path> BuildPaths() {
        // Recorded with Tool: Pose Recorder. 9 spots.
// Paste into BuildPaths() of a copy of AutoShapeZ. It also needs:
//     import static com.pedropathing.api.Paths.line;
//     import com.pedropathing.paths.interpolator.Interpolator;
// Start the auto from the spot the recording started from, facing the same way.

        Pose spot1 = new Pose(-8.6, -11.0);   // heading -25 deg
        Pose spot2 = new Pose(-39.6, -41.4);   // heading -94 deg
        Pose spot3 = new Pose(-37.8, -22.2);   // heading -184 deg
        Pose spot4 = new Pose(-112.5, -18.5);   // heading -163 deg
        Pose spot5 = new Pose(-118.7, -3.3);   // heading -183 deg
        Pose spot6 = new Pose(-116.0, -3.5);   // heading -179 deg
        Pose spot7 = new Pose(-106.3, -32.7);   // heading -160 deg

        Path toSpot1 = line(StartPose(), spot1).heading(Interpolator.linear(Math.toRadians(0), Math.toRadians(-25)));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-25), Math.toRadians(-94)));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(-94), Math.toRadians(-184)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-184), Math.toRadians(-163)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-163), Math.toRadians(-183)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-183), Math.toRadians(-179)));
        Path toSpot7 = line(spot6, spot7).heading(Interpolator.linear(Math.toRadians(-179), Math.toRadians(-160)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot1);
        paths.add(LAUNCH);
        paths.add(toSpot2);
        paths.add(FLOWER);
        paths.add(toSpot3);
        paths.add(toSpot4);
        paths.add(LAUNCH);
        paths.add(toSpot5);
        paths.add(FLOWER);
        paths.add(toSpot6);
        paths.add(LAUNCH);
        paths.add(toSpot7);
        return paths;
    }
}
