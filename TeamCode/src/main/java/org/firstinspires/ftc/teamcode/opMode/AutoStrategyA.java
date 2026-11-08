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

    /** The robot starts facing the driver's left (was -90 until 2026-11-05: backwards). */
    @Override
    protected double StartFacingDeg() {
        return 90;
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

        Pose spot1 = new Pose(-13.6, 4.3);   // heading -0 deg
        Pose spot2 = new Pose(-25.4, -18.1);   // heading 32 deg
        Pose spot3 = new Pose(-39.5, -42.2);   // heading -93 deg
        Pose spot4 = new Pose(-31.7, -22.7);   // heading -181 deg
        Pose spot5 = new Pose(-100.0, -22.7);   // heading -177 deg
        Pose spot6 = new Pose(-110.9, -5.9);   // heading -175 deg
        Pose spot7 = new Pose(-118.7, -6.5);   // heading -174 deg
        Pose spot8 = new Pose(-111.0, -5.7);   // heading -173 deg
        Pose spot9 = new Pose(-84.6, -38.1);   // heading -162 deg

        Path toSpot1 = line(StartPose(), spot1).constant(Math.toRadians(-0));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-0), Math.toRadians(32)));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(32), Math.toRadians(-93)));
        Path toSpot4 = line(spot3, spot4).heading(Interpolator.linear(Math.toRadians(-93), Math.toRadians(-181)));
        Path toSpot5 = line(spot4, spot5).heading(Interpolator.linear(Math.toRadians(-181), Math.toRadians(-177)));
        Path toSpot6 = line(spot5, spot6).heading(Interpolator.linear(Math.toRadians(-177), Math.toRadians(-175)));
        Path toSpot7 = line(spot6, spot7).constant(Math.toRadians(-174));
        Path toSpot8 = line(spot7, spot8).heading(Interpolator.linear(Math.toRadians(-174), Math.toRadians(-173)));
        Path toSpot9 = line(spot8, spot9).heading(Interpolator.linear(Math.toRadians(-173), Math.toRadians(-162)));

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
