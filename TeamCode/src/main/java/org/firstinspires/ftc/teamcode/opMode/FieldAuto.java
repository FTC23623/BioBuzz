package org.firstinspires.ftc.teamcode.opMode;

import com.pedropathing.math.Pose;

/**
 * A {@link PedroAuto} whose spots are FIELD spots, the kind the Pedro Pathing Visualizer
 * (visualizer.pedropathing.com) shows and exports: inches from the bottom-left corner of its
 * field picture, x right, y up, heading 0 pointing right (docs/decisions/0051). The other
 * autos measure every spot from where the robot starts instead.
 *
 * <p>An auto like this must say where it starts, in {@link #FieldStart()}. Pedro is told the
 * robot is there before the first path, so every later spot can be copied straight from the
 * visualizer.
 *
 * <p>Draw the route for BLUE. Red drives the same route: the field is the same turned half
 * a turn, and the robot is put on the matching red spot.
 */
public abstract class FieldAuto extends PedroAuto {

    /**
     * Where the robot starts: the visualizer's start point, blue. The robot must be put on
     * this exact spot, facing this way, before INIT.
     */
    protected abstract Pose FieldStart();

    /** The paths are field spots, so Pedro starts at the field start, not (0, 0). */
    @Override
    protected final Pose StartPose() {
        return FieldStart();
    }

    /** The same spot again, for the laptop tool that draws the autos on the field. */
    @Override
    protected final Pose FieldStartBlue() {
        return FieldStart();
    }
}
