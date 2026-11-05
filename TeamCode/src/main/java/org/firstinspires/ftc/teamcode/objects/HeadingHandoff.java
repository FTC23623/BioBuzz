package org.firstinspires.ftc.teamcode.objects;

/**
 * Carries the robot's heading from the auto into TeleOp, so field-centric "forward" is still
 * away from the driver after the auto has turned the robot around (docs/decisions/0046).
 *
 * <p>The auto saves the heading every loop. TeleOp takes it once, at INIT. It is kept in a
 * static field, which lives as long as the Robot Controller app: the auto and TeleOp are
 * two OpModes, but the same app. No hardware here, so it can be tested on a laptop.
 *
 * <p>The heading is TeleOp's kind: degrees, counter-clockwise (left) positive, 0 = facing
 * away from the driver, between -180 and 180.
 */
public final class HeadingHandoff {
    private HeadingHandoff() {
    }

    /** true once an auto has saved a heading that TeleOp has not taken yet. */
    private static boolean sSaved = false;
    /** The heading the auto saved last, in degrees. */
    private static double sYawDeg = 0;
    /** When the auto saved it, in seconds from {@link #NowSeconds()}. */
    private static double sSavedAtSeconds = 0;

    /** A clock in seconds, for {@link #Save} and {@link #Take}. */
    public static double NowSeconds() {
        return System.nanoTime() / 1e9;
    }

    /** The auto: this is the robot's heading right now, measured from "away from the driver". */
    public static void Save(double yawDeg, double nowSeconds) {
        sYawDeg = Wrap(yawDeg);
        sSavedAtSeconds = nowSeconds;
        sSaved = true;
    }

    /** Forget any saved heading. An auto calls this at INIT, so an old one is never used. */
    public static void Clear() {
        sSaved = false;
    }

    /**
     * TeleOp: the heading the auto left, and forget it, so it is only used once.
     *
     * @param maxAgeSeconds a heading saved longer ago than this is too old: the robot may
     *                      have been picked up and moved since ({@code headingHandoffMaxSeconds})
     * @return degrees, or NaN when there is none, or it is too old
     */
    public static double Take(double nowSeconds, double maxAgeSeconds) {
        if (!sSaved) {
            return Double.NaN;
        }
        sSaved = false;
        double ageSeconds = nowSeconds - sSavedAtSeconds;
        if (ageSeconds > maxAgeSeconds) {
            return Double.NaN;
        }
        return sYawDeg;
    }

    /**
     * The TeleOp heading from the auto's: which way the robot faced at its start spot (seen
     * from the driver), plus how far Pedro has turned it since.
     *
     * <pre>
     *     start facing the driver's right (-90), Pedro turned it 90 left: 0, away from the driver
     *     start facing the driver's left (90), Pedro turned it 120 left: -150
     * </pre>
     *
     * @param startFacingDeg   the robot's start heading, 0 = away from the driver, left positive
     * @param pedroHeadingDeg  Pedro's heading now, 0 = the start heading, left positive
     */
    public static double TeleOpYaw(double startFacingDeg, double pedroHeadingDeg) {
        return Wrap(startFacingDeg + pedroHeadingDeg);
    }

    /** Any angle, in degrees, as the same direction between -180 and 180. */
    static double Wrap(double deg) {
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
