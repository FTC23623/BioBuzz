package org.firstinspires.ftc.teamcode.objects;

/**
 * The drive and strafe that push the robot back to a spot it was told to stay on, after it
 * has been bumped. No hardware here, so it can be tested on a laptop.
 *
 * <p>The spot and the robot's position are on the field (x forward from where the heading
 * is 0, y to the left). The answer is for the robot (drive and strafe), so it is turned by
 * the robot's heading first:
 *
 * <pre>
 *     facing 0, pushed 4 in backward:      drive forward (-), no strafe
 *     facing 90 left, pushed 4 in toward
 *     field -x (to the robot's left):      strafe right (-)
 * </pre>
 */
public final class HoldPosition {
    private HoldPosition() {
    }

    /** No push at all: not holding. */
    public static final Push NONE = new Push(0, 0, 0);

    /** What to add to the drive this loop. Same signs as the sticks: drive forward is negative, strafe left is positive. */
    public static final class Push {
        public final double drive;
        public final double strafe;
        /** How far from the spot, in inches. */
        public final double offIn;

        Push(double drive, double strafe, double offIn) {
            this.drive = drive;
            this.strafe = strafe;
            this.offIn = offIn;
        }
    }

    /**
     * @param spotX    the spot to stay on, inches
     * @param spotY    the spot to stay on, inches
     * @param x        where the robot is now, inches
     * @param y        where the robot is now, inches
     * @param yawDeg   which way the robot faces, degrees, left positive
     * @param kP       power per inch off the spot
     * @param maxPower neither push goes past this, either way
     * @param doneIn   this close to the spot counts as on it: no push, so it doesn't buzz
     * @return the push. Nothing (0, 0) when close enough or a number is broken (NaN).
     */
    public static Push Toward(double spotX, double spotY, double x, double y, double yawDeg,
                              double kP, double maxPower, double doneIn) {
        double offX = spotX - x;
        double offY = spotY - y;
        double offIn = Math.hypot(offX, offY);
        if (Double.isNaN(offIn) || Double.isNaN(yawDeg) || offIn <= doneIn) {
            return new Push(0, 0, Double.isNaN(offIn) ? 0 : offIn);
        }
        // Turn the field offset into the robot's forward and left.
        double yaw = Math.toRadians(yawDeg);
        double forwardIn = offX * Math.cos(yaw) + offY * Math.sin(yaw);
        double leftIn = -offX * Math.sin(yaw) + offY * Math.cos(yaw);
        // Forward is negative drive, like the stick.
        double drive = Clip(-kP * forwardIn, maxPower);
        double strafe = Clip(kP * leftIn, maxPower);
        return new Push(drive, strafe, offIn);
    }

    private static double Clip(double value, double limit) {
        return Math.max(-limit, Math.min(value, limit));
    }
}
