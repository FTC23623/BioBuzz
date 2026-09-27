package org.firstinspires.ftc.teamcode.objects;

/**
 * The math that turns "drive, strafe, rotate" into four wheel powers.
 *
 * <p>No hardware here, on purpose. This class only does arithmetic, so it can be tested on
 * a laptop without a robot.
 *
 * <p>Sign convention (kept exactly from the Decode robot so it drives the same):
 * {@code drive} is the raw left stick Y (pushing forward is negative), {@code strafe} and
 * {@code rotate} are the negated stick X values, and the left motors run FORWARD while the
 * right motors run REVERSE. Every sign is the opposite of the FTC sample, and they cancel
 * out.
 *
 * <p>Wheel power formulas, before scaling:
 * <pre>
 *   front left  = drive + strafe + rotate
 *   back left   = drive - strafe + rotate
 *   front right = drive - strafe - rotate
 *   back right  = drive + strafe - rotate
 * </pre>
 */
public final class MecanumMath {

    /** Four wheel powers. Reuse one instance every loop so the loop does not create objects. */
    public static final class WheelPowers {
        public double frontLeft;
        public double frontRight;
        public double backLeft;
        public double backRight;
    }

    private MecanumMath() {
    }

    /**
     * Robot-centric: "forward" means the way the robot is facing.
     */
    public static void RobotCentric(double drive, double strafe, double rotate, WheelPowers out) {
        Mix(drive, strafe, drive, strafe, rotate, out);
    }

    /**
     * Field-centric: "forward" means away from the driver, whichever way the robot faces.
     *
     * @param yawDegrees how far the robot has turned from the driver's forward, counter-clockwise
     */
    public static void FieldCentric(double drive, double strafe, double rotate, double yawDegrees,
                                    WheelPowers out) {
        // Turn the stick direction by -yaw so it points the same way on the field.
        double yawRadians = -yawDegrees / 180 * Math.PI;
        double rotX = strafe * Math.cos(yawRadians) - drive * Math.sin(yawRadians);
        double rotY = strafe * Math.sin(yawRadians) + drive * Math.cos(yawRadians);
        Mix(drive, strafe, rotY, rotX, rotate, out);
    }

    /**
     * Combine the inputs into wheel powers and scale them down if they add up past 1.
     *
     * <p>Known issue, kept from Decode on purpose: the scale uses the stick values before
     * the field-centric turn ({@code drive}, {@code strafe}), not after it ({@code forward},
     * {@code sideways}). At some headings a wheel can be asked for up to about 1.41, and the
     * motor controller cuts it to 1. See docs/decisions/0002-drive-port.md.
     */
    private static void Mix(double drive, double strafe, double forward, double sideways,
                            double rotate, WheelPowers out) {
        double sum = Math.abs(drive) + Math.abs(strafe) + Math.abs(rotate);
        double max = Math.max(sum, 1);
        out.frontLeft = (forward + sideways + rotate) / max;
        out.backLeft = (forward - sideways + rotate) / max;
        out.frontRight = (forward - sideways - rotate) / max;
        out.backRight = (forward + sideways - rotate) / max;
    }
}
