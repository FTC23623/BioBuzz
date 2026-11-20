package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * Something that knows which way the robot is facing, and maybe where it is.
 *
 * <p>The drive always needs a heading. A position (x, y) is only there when the hardware can
 * measure one: the Pinpoint can, the Control Hub's IMU can't ({@link #HasPosition()}).
 */
public interface Imu extends Subsystem {
    /** @return true once the heading can be trusted */
    boolean Ready();

    /**
     * @return how far the robot has turned since the last reset, in degrees,
     *     counter-clockwise positive, between -180 and 180
     */
    double GetYaw();

    /** Make the direction the robot faces right now count as 0 degrees. */
    void ResetYaw();

    /**
     * Make the direction the robot faces right now count as {@code yawDeg}. TeleOp uses this
     * to keep the heading the auto left (docs/decisions/0046). Call it once {@link #Ready()}.
     */
    void SetYaw(double yawDeg);

    /** @return true when {@link #GetXIn()} and {@link #GetYIn()} mean something */
    default boolean HasPosition() {
        return false;
    }

    /**
     * @return inches along the direction {@link #GetYaw()} calls 0, from where the robot
     *     started. 0 with no position.
     */
    default double GetXIn() {
        return 0;
    }

    /**
     * @return inches to the left of the direction {@link #GetYaw()} calls 0, from where the
     *     robot started. 0 with no position.
     */
    default double GetYIn() {
        return 0;
    }
}
