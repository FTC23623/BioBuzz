package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * A copy of {@link AutoStrategyGardenD} (Garden Side D) that skips the second flower. Same
 * start, same spots.
 *
 * <p>The route: from the start, back up to spot 1 and launch. Out past the hive (spot 2, the
 * stop that keeps the robot clear of the hive) to the first flower (spot 3) and work it. Back
 * the same way, through spot 2, to spot 1, and launch again there. Then the difference from
 * D: no second flower. It heads onto A's way across the field (spot 4) and straight on to the
 * final spot, the loading zone (spot 9).
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * The robot starts facing the wall, so backing up is minus x and the garden (to its right) is
 * minus y. See {@link PedroAuto} for more.
 */
// Group "Auto" (it reads the alliance magnet), like A. At the end it picks HyDrive.
@Autonomous(name = "Auto: " + AutoStrategyGardenE.NAME, group = "Auto", preselectTeleOp = HyDrive.NAME)
public class AutoStrategyGardenE extends PedroAuto {
    /** The auto's name. On the Driver Station: "Auto: ", "Blue: " or "Red: " in front. */
    public static final String NAME = "Garden Side E";

    /** The same start as Garden Side A. Blue; red is turned half a turn (0051). */
    @Override
    protected Pose FieldStartBlue() {
        return new Pose(55.75, 9, Math.toRadians(-90));
    }

    /** The robot starts facing the driver's left, like Garden Side A. */
    @Override
    protected double StartFacingDeg() {
        return 90;
    }

    @Override
    protected List<Path> BuildPaths() {
        // Spots copied from AutoStrategyA (recorded with Tool: Pose Recorder).
        Pose spot1 = new Pose(-13.6, 4.3);   // heading -0 deg: both LAUNCHes
        Pose spot2 = new Pose(-25.4, -18.1);   // heading 32 deg: the stop that misses the hive
        Pose spot3 = new Pose(-39.5, -42.2);   // heading -93 deg: the FLOWER
        Pose spot4 = new Pose(-31.7, -22.7);   // heading -181 deg: onto A's way across
        Pose spot9 = new Pose(-84.6, -38.1);   // heading -162 deg: the loading zone

        // Out to the first flower, as in A.
        Path toSpot1 = line(StartPose(), spot1).constant(Math.toRadians(-0));
        Path toSpot2 = line(spot1, spot2).heading(Interpolator.linear(Math.toRadians(-0), Math.toRadians(32)));
        Path toSpot3 = line(spot2, spot3).heading(Interpolator.linear(Math.toRadians(32), Math.toRadians(-93)));
        // Back to the first launch spot the same way, through the stop that misses the hive.
        Path backToSpot2 = line(spot3, spot2).heading(Interpolator.linear(Math.toRadians(-93), Math.toRadians(32)));
        Path backToSpot1 = line(spot2, spot1).heading(Interpolator.linear(Math.toRadians(32), Math.toRadians(-0)));
        // From the launch spot onto A's way across the field, as in D.
        // CHECK on the field: this turn (0 to -181) is bigger than A's here (-93 to -181).
        Path spot1ToSpot4 = line(spot1, spot4).heading(Interpolator.linear(Math.toRadians(-0), Math.toRadians(-181)));
        // Then straight to the loading zone, instead of on to the second flower.
        // CHECK on the field: a new leg, not recorded. Watch it clears everything on the way.
        Path spot4ToSpot9 = line(spot4, spot9).heading(Interpolator.linear(Math.toRadians(-181), Math.toRadians(-162)));

        List<Path> paths = new ArrayList<>();
        paths.add(toSpot1);
        paths.add(LAUNCH);
        paths.add(toSpot2);
        paths.add(toSpot3);
        paths.add(FLOWER);
        paths.add(backToSpot2);
        paths.add(backToSpot1);
        paths.add(LAUNCH);
        paths.add(spot1ToSpot4);
        paths.add(spot4ToSpot9);
        return paths;
    }
}
