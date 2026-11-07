package org.firstinspires.ftc.teamcode.objects;

import com.pedropathing.math.Pose;

/**
 * Where a spot is on the whole field, for the Pedro Pathing Visualizer
 * (visualizer.pedropathing.com, docs/decisions/0051).
 *
 * <p>Our autos measure every spot from where the robot starts: (0, 0), facing heading 0,
 * the same as Tool: Pose Recorder. The visualizer measures from the field's corner:
 *
 * <pre>
 *     (0, 0) is the bottom-left corner of its BIOBUZZ field picture
 *     x grows to the right, y grows up, both 0 to 141.5 inches (the floor inside the walls)
 *     heading 0 points right, and grows turning left, like ours
 * </pre>
 *
 * The field is the same for both alliances turned half a turn, so a blue spot gives the red
 * one. No hardware here, so it can be tested on a laptop.
 */
public final class FieldPose {
    private FieldPose() {
    }

    /** The floor inside the walls, in inches, as the visualizer draws it. */
    public static final double FIELD_IN = 141.5;

    /**
     * A spot measured from the auto's start, as a spot on the field.
     *
     * <pre>
     *     start (105, 9) facing down (-90); the auto backs up 11 in (x -11):
     *     on the field that is 11 in up: (105, 20), still facing down
     * </pre>
     *
     * @param start     where the robot starts on the field, heading in radians
     * @param fromStart a spot from the auto, measured from its start (x forward, y left)
     */
    public static Pose ToField(Pose start, Pose fromStart) {
        double cos = Math.cos(start.heading());
        double sin = Math.sin(start.heading());
        double x = start.x() + fromStart.x() * cos - fromStart.y() * sin;
        double y = start.y() + fromStart.x() * sin + fromStart.y() * cos;
        return new Pose(x, y, start.heading() + fromStart.heading());
    }

    /**
     * A spot on the field, measured from the auto's start instead: the opposite of {@link
     * #ToField}. For turning a visualizer file into an auto.
     */
    public static Pose FromField(Pose start, Pose onField) {
        double dx = onField.x() - start.x();
        double dy = onField.y() - start.y();
        double cos = Math.cos(start.heading());
        double sin = Math.sin(start.heading());
        double x = dx * cos + dy * sin;
        double y = -dx * sin + dy * cos;
        return new Pose(x, y, onField.heading() - start.heading());
    }

    /** The same spot for the other alliance: the field turned half a turn about its middle. */
    public static Pose OtherAlliance(Pose spot) {
        return new Pose(FIELD_IN - spot.x(), FIELD_IN - spot.y(), spot.heading() + Math.PI);
    }

    /** Any angle in degrees as the same direction between -180 and 180. */
    public static double WrapDegrees(double deg) {
        double wrapped = deg % 360;
        if (wrapped > 180) {
            wrapped = wrapped - 360;
        }
        if (wrapped < -180) {
            wrapped = wrapped + 360;
        }
        return wrapped;
    }
}
