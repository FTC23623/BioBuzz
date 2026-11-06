package org.firstinspires.ftc.teamcode.objects;

import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * The launch curve worked out once per step and kept in a table, so each loop looks the RPM
 * up instead of working out a power (docs/decisions/0038).
 *
 * <pre>
 *     near 110 in 2800, far 140 in 3275, curve 1.1, steps of 2 in:
 *     step:   110   112   114  ...  138
 *     RPM:   2800  2824  2851  ... 3240     and 3275 from 140 on
 * </pre>
 *
 * <p>The table remembers the settings it was built from. If one of them changes (on the
 * dashboard, say), {@link #UpdateFrom} builds it again, so live tuning still works. No
 * hardware here, so it can be tested on a laptop.
 */
public class RpmTable {
    private double[] mRpms = null;
    // The settings the table was built from.
    private double mNearIn;
    private double mNearRpm;
    private double mFarIn;
    private double mFarRpm;
    private double mCurve;
    private double mBandIn;

    /** Build the table again, but only if a curve setting changed since last time. */
    public void UpdateFrom(BotProperties p) {
        boolean same = mRpms != null
                && p.launchNearIn == mNearIn && p.launchNearRpm == mNearRpm
                && p.launchFarIn == mFarIn && p.launchFarRpm == mFarRpm
                && p.launchCurve == mCurve && p.launchBandIn == mBandIn;
        if (same) {
            return;
        }
        mNearIn = p.launchNearIn;
        mNearRpm = p.launchNearRpm;
        mFarIn = p.launchFarIn;
        mFarRpm = p.launchFarRpm;
        mCurve = p.launchCurve;
        mBandIn = p.launchBandIn;
        if (mBandIn <= 0 || mFarIn <= mNearIn) {
            // No steps: nothing to put in a table. Rpm() works it out each time.
            mRpms = new double[0];
            return;
        }
        // One entry per step that starts before the far shot.
        int steps = (int) Math.ceil((mFarIn - mNearIn) / mBandIn);
        mRpms = new double[steps];
        for (int i = 0; i < steps; i++) {
            mRpms[i] = CurveRpm(mNearIn + i * mBandIn, mNearIn, mNearRpm, mFarIn, mFarRpm, mCurve);
        }
    }

    /**
     * The RPM for a step, from the table. Call {@link #UpdateFrom} first.
     *
     * @param stepStartIn where the step starts, in inches (from {@code Launcher.StepStart})
     * @return whole RPM
     */
    public double Rpm(double stepStartIn) {
        if (stepStartIn <= mNearIn) {
            return mNearRpm;
        }
        if (stepStartIn >= mFarIn) {
            return mFarRpm;
        }
        if (mRpms == null || mRpms.length == 0) {
            return CurveRpm(stepStartIn, mNearIn, mNearRpm, mFarIn, mFarRpm, mCurve);
        }
        int index = (int) Math.round((stepStartIn - mNearIn) / mBandIn);
        index = Math.max(0, Math.min(index, mRpms.length - 1));
        return mRpms[index];
    }

    /**
     * The curve itself, worked out:
     * <pre>
     *     rpm = near + (far - near) x ((step - nearIn) / (farIn - nearIn)) ^ curve
     * </pre>
     * Nearer than nearIn: the near speed. Farther than farIn: the far speed.
     *
     * @return whole RPM
     */
    public static double CurveRpm(double stepStartIn, double nearIn, double nearRpm,
                                  double farIn, double farRpm, double curve) {
        if (stepStartIn <= nearIn) {
            return nearRpm;
        }
        if (stepStartIn >= farIn) {
            return farRpm;
        }
        double fraction = (stepStartIn - nearIn) / (farIn - nearIn);
        double rpm = nearRpm + (farRpm - nearRpm) * Math.pow(fraction, curve);
        return Math.round(rpm);
    }
}
