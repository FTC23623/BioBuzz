package org.firstinspires.ftc.teamcode.objects;

/**
 * Where the robot is, worked out by the camera from the AprilTags alone: no odometry pods,
 * so the robot can be slid around by hand during tuning (docs/decisions/0054).
 *
 * <p>Two answers, from the same picture:
 * <ul>
 *   <li><b>From the hive</b>: from the nearest tag of ours, which is on the hive. How far to
 *       the left of the tag the camera is (as the robot faces the hive; right is minus), how
 *       far out from it, and the camera's yaw against the tag, the Limelight's own angle.
 *   <li><b>On the field</b>: the Limelight's whole-field answer (MegaTag 1), from every tag
 *       in view and the field map loaded on the Limelight. Inches from the middle of the
 *       field; its axes are the Limelight's. Only there when the Limelight has the field map.
 * </ul>
 * NaN means not known. No hardware here, so it can be tested on a laptop.
 */
public final class TagPosition {
    /** Nothing seen, nothing known. */
    public static final TagPosition UNKNOWN = new TagPosition(Double.NaN, Double.NaN, Double.NaN,
            Double.NaN, Double.NaN, Double.NaN, 0);

    /** Inches to the left of the hive's tag, as the robot faces the hive. Right is minus. */
    public final double hiveLeftIn;
    /** Inches straight out from the hive's tag. */
    public final double hiveOutIn;
    /** The camera's yaw against the tag, degrees, as the Limelight reports it. CHECK its sign. */
    public final double hiveYawDeg;
    /** The Limelight's field position, inches from the middle of the field. */
    public final double fieldXIn;
    public final double fieldYIn;
    /** The Limelight's field heading, degrees. */
    public final double fieldYawDeg;
    /** How many tags the field position came from. 0 = no field position. */
    public final int fieldTags;

    public TagPosition(double hiveLeftIn, double hiveOutIn, double hiveYawDeg,
                       double fieldXIn, double fieldYIn, double fieldYawDeg, int fieldTags) {
        this.hiveLeftIn = hiveLeftIn;
        this.hiveOutIn = hiveOutIn;
        this.hiveYawDeg = hiveYawDeg;
        this.fieldXIn = fieldXIn;
        this.fieldYIn = fieldYIn;
        this.fieldYawDeg = fieldYawDeg;
        this.fieldTags = fieldTags;
    }
}
