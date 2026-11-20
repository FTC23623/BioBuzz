package org.firstinspires.ftc.teamcode.objects;

import java.util.Arrays;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * The launch speed for each distance step, worked out once and kept in a table, so each
 * loop looks the RPM up (docs/decisions/0038).
 *
 * <p>The speeds come from four measured shots in BotProperties (near, two in the middle,
 * far), joined by straight lines:
 *
 * <pre>
 *     110 in 2760, 120 in 2831, 130 in 2983, 140 in 3115, steps of 2 in:
 *     step:   110   112   114  ...  120   122  ...  130   132  ...  138
 *     RPM:   2760  2774  2788  ... 2831  2861  ... 2983  3009  ... 3089     and 3115 from 140 on
 * </pre>
 *
 * <p>The table remembers the settings it was built from. If one of them changes (on the
 * dashboard, say), {@link #UpdateFrom} builds it again, so live tuning still works. No
 * hardware here, so it can be tested on a laptop.
 */
public class RpmTable {
    private double[] mRpms = null;
    // The settings the table was built from.
    private double[] mPointsIn = new double[0];
    private double[] mPointsRpm = new double[0];
    private double mBandIn;

    /** The measured distances, near to far, in inches. */
    public static double[] PointsIn(BotProperties p) {
        return new double[] {p.launchNearIn, p.launchMid1In, p.launchMid2In, p.launchFarIn};
    }

    /** The RPM measured at each of {@link #PointsIn}, in the same order. */
    public static double[] PointsRpm(BotProperties p) {
        return new double[] {p.launchNearRpm, p.launchMid1Rpm, p.launchMid2Rpm, p.launchFarRpm};
    }

    /** Build the table again, but only if a speed setting changed since last time. */
    public void UpdateFrom(BotProperties p) {
        double[] pointsIn = PointsIn(p);
        double[] pointsRpm = PointsRpm(p);
        boolean same = mRpms != null
                && Arrays.equals(pointsIn, mPointsIn) && Arrays.equals(pointsRpm, mPointsRpm)
                && p.launchBandIn == mBandIn;
        if (same) {
            return;
        }
        mPointsIn = pointsIn;
        mPointsRpm = pointsRpm;
        mBandIn = p.launchBandIn;
        double nearIn = NearIn();
        double farIn = FarIn();
        if (mBandIn <= 0 || farIn <= nearIn) {
            // No steps: nothing to put in a table. Rpm() works it out each time.
            mRpms = new double[0];
            return;
        }
        // One entry per step that starts before the far shot.
        int steps = (int) Math.ceil((farIn - nearIn) / mBandIn);
        mRpms = new double[steps];
        for (int i = 0; i < steps; i++) {
            mRpms[i] = LineRpm(nearIn + i * mBandIn, mPointsIn, mPointsRpm);
        }
    }

    /**
     * The RPM for a step, from the table. Call {@link #UpdateFrom} first.
     *
     * @param stepStartIn where the step starts, in inches (from {@code Launcher.StepStart})
     * @return whole RPM
     */
    public double Rpm(double stepStartIn) {
        double nearIn = NearIn();
        if (mRpms == null || mRpms.length == 0 || stepStartIn <= nearIn || stepStartIn >= FarIn()) {
            return LineRpm(stepStartIn, mPointsIn, mPointsRpm);
        }
        int index = (int) Math.round((stepStartIn - nearIn) / mBandIn);
        index = Math.max(0, Math.min(index, mRpms.length - 1));
        return mRpms[index];
    }

    private double NearIn() {
        return mPointsIn.length == 0 ? 0 : mPointsIn[0];
    }

    private double FarIn() {
        return mPointsIn.length == 0 ? 0 : mPointsIn[mPointsIn.length - 1];
    }

    /**
     * The speed at a distance, worked out: a straight line between the two measured shots
     * on either side of it.
     *
     * <pre>
     *     shots at 120 in 2831 and 130 in 2983; 124 in is 4/10 of the way:
     *     2831 + (2983 - 2831) x 0.4 = 2891.8, rounded 2892
     * </pre>
     *
     * Nearer than the first shot: the first shot's speed. Farther than the last: the last
     * shot's speed. A shot no farther than the one before it is skipped, so a typo on the
     * dashboard can't divide by zero.
     *
     * @param inches the measured distances, near to far
     * @param rpms   the speed at each distance, same order
     * @return whole RPM. 0 with no shots at all.
     */
    public static double LineRpm(double stepStartIn, double[] inches, double[] rpms) {
        if (inches.length == 0) {
            return 0;
        }
        if (stepStartIn <= inches[0]) {
            return rpms[0];
        }
        int last = inches.length - 1;
        if (stepStartIn >= inches[last]) {
            return rpms[last];
        }
        // The shot just nearer than this distance, and the next one farther out.
        int below = 0;
        for (int i = 1; i <= last; i++) {
            if (inches[i] <= inches[below]) {
                continue;   // out of order: skip it
            }
            if (stepStartIn <= inches[i]) {
                double fraction = (stepStartIn - inches[below]) / (inches[i] - inches[below]);
                return Math.round(rpms[below] + (rpms[i] - rpms[below]) * fraction);
            }
            below = i;
        }
        return rpms[last];
    }
}
