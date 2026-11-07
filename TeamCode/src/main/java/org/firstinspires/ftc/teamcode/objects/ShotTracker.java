package org.firstinspires.ftc.teamcode.objects;

/**
 * Finds each ball leaving the pollen launcher from its speed, and measures the dip it made
 * (docs/decisions/0052).
 *
 * <p>A ball takes speed out of the wheel as it goes through. So while the gates are open,
 * the speed falling more than {@code dipRpm} under the target means a ball is going; the
 * shot is over once the speed is back within {@code recoveredRpm} of the target, or after
 * {@code timeoutSeconds} if it never gets back.
 *
 * <pre>
 *     target 3000, dip 100, recovered 30:
 *     3000 2990 2860 2710 2790 2930 2975   (gates open)
 *               ^ shot starts       ^ shot done: dip 290 RPM, recovered in the time between
 * </pre>
 *
 * <p>Call {@link #Update} every loop. No hardware here, so it can be tested on a laptop.
 */
public class ShotTracker {
    /** One ball, once its dip is over. */
    public static final class Shot {
        /** Counts from 1. */
        public final int number;
        /** The target speed when the dip started, RPM. */
        public final double targetRpm;
        /** How far under the target the speed fell, RPM. */
        public final double dipRpm;
        /** From the dip starting to the speed being back near the target, seconds. */
        public final double recoverySeconds;
        /** false when the speed never came back near the target before the time ran out. */
        public final boolean recovered;

        Shot(int number, double targetRpm, double dipRpm, double recoverySeconds, boolean recovered) {
            this.number = number;
            this.targetRpm = targetRpm;
            this.dipRpm = dipRpm;
            this.recoverySeconds = recoverySeconds;
            this.recovered = recovered;
        }
    }

    private final double mDipRpm;
    private final double mRecoveredRpm;
    private final double mTimeoutSeconds;

    private boolean mInShot = false;
    private double mStartSeconds = 0;
    private double mStartTargetRpm = 0;
    private double mLowestRpm = 0;
    private int mShots = 0;

    /**
     * @param dipRpm         this far under the target, with the gates open, is a ball going
     * @param recoveredRpm   back within this of the target is the shot over
     * @param timeoutSeconds a shot that hasn't recovered by now is over anyway
     */
    public ShotTracker(double dipRpm, double recoveredRpm, double timeoutSeconds) {
        mDipRpm = dipRpm;
        mRecoveredRpm = recoveredRpm;
        mTimeoutSeconds = timeoutSeconds;
    }

    /**
     * One loop.
     *
     * @return the shot that just finished, or null
     */
    public Shot Update(double nowSeconds, boolean gatesOpen, double targetRpm, double rpm) {
        double under = targetRpm - rpm;
        if (!mInShot) {
            if (gatesOpen && targetRpm > 0 && under > mDipRpm) {
                mInShot = true;
                mStartSeconds = nowSeconds;
                mStartTargetRpm = targetRpm;
                mLowestRpm = rpm;
            }
            return null;
        }
        mLowestRpm = Math.min(mLowestRpm, rpm);
        double seconds = nowSeconds - mStartSeconds;
        boolean recovered = (mStartTargetRpm - rpm) <= mRecoveredRpm;
        if (recovered || seconds >= mTimeoutSeconds) {
            mInShot = false;
            mShots = mShots + 1;
            return new Shot(mShots, mStartTargetRpm, mStartTargetRpm - mLowestRpm, seconds, recovered);
        }
        return null;
    }

    /** true while a ball's dip is going on. */
    public boolean InShot() {
        return mInShot;
    }

    /** How many shots so far. */
    public int Count() {
        return mShots;
    }
}
