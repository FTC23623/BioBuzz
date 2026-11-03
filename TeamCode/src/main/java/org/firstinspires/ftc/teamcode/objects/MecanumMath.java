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

    /** The four wheel powers, one per motor. */
    public static final class WheelPowers {
        public double frontLeft;
        public double frontRight;
        public double backLeft;
        public double backRight;
    }

    private MecanumMath() {
    }

    /**
     * Drift assist, part 1: the extra drive that moves the pivot to the inside of the turn.
     * A robot spinning on its inside wheels carries its middle forward, whichever way it
     * turns, so this is always forward (negative), never backward.
     *
     * @param sideGain 0 = pivot in the middle, 0.5 = pivot on the inside wheels (square robot)
     */
    public static double DriftDrive(double rotate, double sideGain) {
        return -Math.abs(rotate) * sideGain;
    }

    /**
     * Drift assist, part 2: the extra strafe that moves the pivot to the back. A robot
     * spinning on its back wheels swings its middle toward the inside of the turn: left
     * when turning left (positive rotate), right when turning right.
     *
     * @param rearGain 0 = pivot in the middle, 0.5 = pivot on the back wheels (square robot)
     */
    public static double DriftStrafe(double rotate, double rearGain) {
        return rotate * rearGain;
    }

    /**
     * Robot-centric: "forward" means the way the robot is facing.
     */
    public static WheelPowers RobotCentric(double drive, double strafe, double rotate) {
        // If the inputs add up to more than 1, shrink every wheel by the same amount
        // so no wheel is asked for more than full power.
        double total = Math.abs(drive) + Math.abs(strafe) + Math.abs(rotate);
        double divideBy = Math.max(total, 1);

        WheelPowers powers = new WheelPowers();
        powers.frontLeft = (drive + strafe + rotate) / divideBy;
        powers.backLeft = (drive - strafe + rotate) / divideBy;
        powers.frontRight = (drive - strafe - rotate) / divideBy;
        powers.backRight = (drive + strafe - rotate) / divideBy;
        return powers;
    }

    /**
     * Field-centric: "forward" means away from the driver, whichever way the robot faces.
     *
     * <p>Known issue, kept from Decode on purpose: {@code divideBy} uses the stick values
     * before they are turned, not after. At some headings a wheel can be asked for up to
     * about 1.41, and the motor controller cuts it to 1. See docs/decisions/0002-drive-port.md.
     *
     * @param yawDegrees how far the robot has turned from the driver's forward, counter-clockwise
     */
    public static WheelPowers FieldCentric(double drive, double strafe, double rotate,
                                           double yawDegrees) {
        return FieldCentric(drive, strafe, rotate, yawDegrees, 0, 0);
    }

    /**
     * Field-centric, plus some drive and strafe that stay robot-centric. Drift assist uses
     * this: its pivot point is on the robot, so it must not be turned with the sticks.
     *
     * @param robotDrive  extra drive, added after the turn. Negative is forward, like drive.
     * @param robotStrafe extra strafe, added after the turn. Positive is left, like strafe.
     */
    public static WheelPowers FieldCentric(double drive, double strafe, double rotate,
                                           double yawDegrees, double robotDrive,
                                           double robotStrafe) {
        // Turn the stick direction backward by the robot's heading, so pushing the stick
        // "up" always means "away from the driver" on the field.
        double turnRadians = -yawDegrees / 180 * Math.PI;
        double fieldStrafe = strafe * Math.cos(turnRadians) - drive * Math.sin(turnRadians)
                + robotStrafe;
        double fieldDrive = strafe * Math.sin(turnRadians) + drive * Math.cos(turnRadians)
                + robotDrive;

        double total = Math.abs(drive) + Math.abs(strafe) + Math.abs(rotate)
                + Math.abs(robotDrive) + Math.abs(robotStrafe);
        double divideBy = Math.max(total, 1);

        WheelPowers powers = new WheelPowers();
        powers.frontLeft = (fieldDrive + fieldStrafe + rotate) / divideBy;
        powers.backLeft = (fieldDrive - fieldStrafe + rotate) / divideBy;
        powers.frontRight = (fieldDrive - fieldStrafe - rotate) / divideBy;
        powers.backRight = (fieldDrive + fieldStrafe - rotate) / divideBy;
        return powers;
    }
}
