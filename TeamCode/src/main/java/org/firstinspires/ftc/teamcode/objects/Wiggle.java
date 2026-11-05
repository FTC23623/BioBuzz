package org.firstinspires.ftc.teamcode.objects;

/**
 * The "wiggle" while launching: the robot swings a little left, then a little right, so
 * balls that leave one after another don't fly into each other (docs/decisions/0045).
 *
 * <pre>
 *     wiggleDeg 1, a ball every 1/6 s (10 frames of 60 fps video):
 *     0 to 1/6 s      aim 1 deg LEFT of where the wiggle started
 *     1/6 to 2/6 s    1 deg RIGHT
 *     2/6 to 3/6 s    1 deg LEFT, and so on
 * </pre>
 *
 * No hardware here, so it can be tested on a laptop.
 */
public final class Wiggle {
    private Wiggle() {
    }

    /**
     * Which side to aim at, this long after the wiggle started.
     *
     * @param seconds       since the wiggle started
     * @param switchSeconds how long each side lasts
     * @return 1 for left, -1 for right. Left first.
     */
    public static int Side(double seconds, double switchSeconds) {
        if (switchSeconds <= 0) {
            return 1;
        }
        long sidesSoFar = (long) Math.floor(seconds / switchSeconds);
        if (sidesSoFar % 2 == 0) {
            return 1;
        }
        return -1;
    }

    /**
     * The turn toward this side's heading.
     *
     * @param yawDeg       which way the robot faces now, degrees, counter-clockwise positive
     * @param centerYawDeg which way it faced when the wiggle started
     * @param side         1 left, -1 right (from {@link #Side})
     * @param wiggleDeg    how far either side of the center
     * @param kP           turn power per degree off
     * @param maxRotate    the turn never goes past this, either way
     * @return the rotate power for the drive. Positive turns left.
     */
    public static double Turn(double yawDeg, double centerYawDeg, int side, double wiggleDeg,
                              double kP, double maxRotate) {
        double target = centerYawDeg + side * wiggleDeg;
        double error = target - yawDeg;
        // The heading jumps from 180 to -180 when the robot faces backward: take the short way.
        while (error > 180) {
            error = error - 360;
        }
        while (error < -180) {
            error = error + 360;
        }
        double rotate = kP * error;
        return Math.max(-maxRotate, Math.min(rotate, maxRotate));
    }
}
