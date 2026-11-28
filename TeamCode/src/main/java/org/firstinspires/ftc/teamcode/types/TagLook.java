package org.firstinspires.ftc.teamcode.types;

/**
 * What the camera sees of ONE AprilTag in one picture: which tag, how it is turned, how far
 * and which way it is. The tip tuner lists one of these for every tag in view.
 *
 * <p>The angles are the tag's in the camera's space, in degrees. Which one shows a hive cell
 * tipping (pitch or roll) is what the tip tuner helps find. NaN = the camera did not work the
 * angles out.
 */
public final class TagLook {
    public final int id;
    public final double pitchDeg;
    public final double rollDeg;
    public final double yawDeg;
    /** Inches from the camera. 0 = not known. */
    public final double rangeIn;
    /** Degrees from straight ahead to the tag, left positive. */
    public final double bearingDeg;
    /** How much of the picture the tag fills, in percent. Bigger = nearer. */
    public final double areaPercent;

    public TagLook(int id, double pitchDeg, double rollDeg, double yawDeg, double rangeIn,
                   double bearingDeg, double areaPercent) {
        this.id = id;
        this.pitchDeg = pitchDeg;
        this.rollDeg = rollDeg;
        this.yawDeg = yawDeg;
        this.rangeIn = rangeIn;
        this.bearingDeg = bearingDeg;
        this.areaPercent = areaPercent;
    }

    /** The roll, or the pitch when {@code fromRoll} is false. */
    public double Angle(boolean fromRoll) {
        return fromRoll ? rollDeg : pitchDeg;
    }
}
