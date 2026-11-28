package org.firstinspires.ftc.teamcode.objects;

/**
 * The maths behind Test: Tip Detection. The tuner collects angle readings while a hive cell
 * is UP and while it is DOWN, then asks: do the two sets of readings stay apart, and what
 * windows would tell them apart with a safe gap between?
 *
 * <pre>
 *     UP readings:    -40 ... -28        DOWN readings:   20 ... 31
 *     gap between them: 20 - (-28) = 48 degrees
 *     margin on each window: a third of the gap, at most 10 degrees: 10
 *     UP window:   -50 to -18            DOWN window:  10 to 41
 *     the buffer left between the windows: -18 to 10 (28 degrees) answers nothing
 * </pre>
 *
 * No hardware here, so it can be tested on a laptop.
 */
public final class TipTuning {
    private TipTuning() {
    }

    /** A running count, lowest, highest and average of the readings given to {@link #Add}. */
    public static final class Stats {
        private int mCount = 0;
        private double mMin = Double.NaN;
        private double mMax = Double.NaN;
        private double mSum = 0;

        /** Add one reading. An unknown reading (NaN) is not counted. */
        public void Add(double deg) {
            if (Double.isNaN(deg)) {
                return;
            }
            if (mCount == 0) {
                mMin = deg;
                mMax = deg;
            }
            mMin = Math.min(mMin, deg);
            mMax = Math.max(mMax, deg);
            mSum = mSum + deg;
            mCount = mCount + 1;
        }

        public int Count() {
            return mCount;
        }

        /** The lowest reading. NaN with none. */
        public double Min() {
            return mMin;
        }

        /** The highest reading. NaN with none. */
        public double Max() {
            return mMax;
        }

        /** The average reading. NaN with none. */
        public double Mean() {
            return mCount == 0 ? Double.NaN : mSum / mCount;
        }

        public void Clear() {
            mCount = 0;
            mMin = Double.NaN;
            mMax = Double.NaN;
            mSum = 0;
        }
    }

    /** How far apart the UP and DOWN readings are, in degrees. Negative = they overlap. NaN = a set is empty. */
    public static double GapDeg(Stats up, Stats down) {
        if (up.Count() == 0 || down.Count() == 0) {
            return Double.NaN;
        }
        return Math.max(down.Min() - up.Max(), up.Min() - down.Max());
    }

    /** The margin to leave around a set of readings: a third of the gap, but at most {@code maxMarginDeg}. */
    public static double MarginDeg(double gapDeg, double maxMarginDeg) {
        return Math.max(0, Math.min(maxMarginDeg, gapDeg / 3));
    }

    /**
     * The window for one set of readings: from its lowest less the margin, to its highest plus
     * it. Whole degrees, rounded outward.
     */
    public static double[] Window(Stats stats, double marginDeg) {
        return new double[] {Math.floor(stats.Min() - marginDeg), Math.ceil(stats.Max() + marginDeg)};
    }

    /** Enough readings of each to trust the answer. */
    public static boolean Enough(Stats up, Stats down, int minimum) {
        return up.Count() >= minimum && down.Count() >= minimum;
    }
}
