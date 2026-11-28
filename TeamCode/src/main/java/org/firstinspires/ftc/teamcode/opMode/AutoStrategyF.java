package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * A flower side auto drawn in the Pedro Pathing Visualizer (visualizer.pedropathing.com). Its
 * spots are FIELD spots, copied straight from the visualizer, not measured from the start.
 * See {@link FieldAuto}.
 *
 * <p>To build the route:
 * <ol>
 *   <li>Draw it in the visualizer on the BIOBUZZ field, for BLUE.
 *   <li>Copy the visualizer's start point into {@link #START}. This is the one number the
 *       auto can't work out by itself.
 *   <li>Copy its poses and paths into {@link #BuildPaths()}, and add the stops between
 *       them: LAUNCH, FLOWER, INTAKE_ON, INTAKE_OFF, CHECK_TIPPED and the IF steps.
 * </ol>
 *
 * <p>Right now it only backs 12 inches off the wall, so there is something to run.
 */
// Group "Auto" (it reads the alliance magnet), like the others. At the end it picks HyDrive.
@Autonomous(name = "Auto: " + AutoStrategyF.NAME, group = "Auto", preselectTeleOp = HyDrive.NAME)
public class AutoStrategyF extends FieldAuto {
    /** The auto's name. On the Driver Station: "Auto: ", "Blue: " or "Red: " in front. */
    public static final String NAME = "Flower Side F";

    /**
     * PROVIDE THIS: where the robot starts, from the visualizer (blue): x, y in inches,
     * heading in degrees. This one is the flower side start the other autos use: front against
     * the top wall, center 9 in off it.
     */
    static final Pose START = PoseFactory.degrees().of(55.75, 132.5, 90);

    @Override
    protected Pose FieldStart() {
        return START;
    }

    /**
     * The robot's front (the intake) points to the driver's LEFT (90), at the flower side, like
     * every flower side auto. Seen from the driver, NOT the field heading in START. Change it
     * only if START faces a different way.
     */
    @Override
    protected double StartFacingDeg() {
        return 90;
    }

    @Override
    protected List<Path> BuildPaths() {
        PoseFactory poseFactory = PoseFactory.degrees();

        // Field spots from the visualizer: x, y in inches, heading in degrees.
        Pose offTheWall = poseFactory.of(55.75, 120.5, 90);

        Path offTheWallPath = line(START, offTheWall).constant(offTheWall);

        List<Path> paths = new ArrayList<>();
        paths.add(offTheWallPath);
        return paths;
    }
}
