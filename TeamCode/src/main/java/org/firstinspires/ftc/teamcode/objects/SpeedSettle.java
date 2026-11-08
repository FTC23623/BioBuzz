package org.firstinspires.ftc.teamcode.objects;

import java.util.Locale;

/**
 * Waits for a spinning wheel to settle on its target speed: inside a band around the target,
 * without a break, for a hold time. If that never happens, it gives up after a timeout, so
 * nothing waits forever (docs/decisions/0056).
 *
 * <pre>
 *     band 40, hold 0.15 s, timeout 1 s:
 *     t 0.00  error -120  -> settling
 *     t 0.05  error  -30  -> in the band, hold starts
 *     t 0.10  error  +45  -> out again, hold starts over
 *     t 0.15  error  +20  -> in the band
 *     t 0.30  error  -10  -> 0.15 s in the band: SETTLED
 * </pre>
 *
 * <p>The owner calls {@link #Start} on the command, {@link #Update} every loop, and reads
 * {@link #IsDone}. It knows nothing about motors: it is given the time and the speeds.
 */
public class SpeedSettle {
    private boolean mActive = false;
    private boolean mDone = false;
    private boolean mTimedOut = false;
    private double mStartSeconds = 0;
    /** When the speed last came into the band. NaN while it is outside. */
    private double mInBandSinceSeconds = Double.NaN;
    private double mLastSeconds = 0;
    private double mLastErrorRpm = 0;

    /** Start waiting, from this time. Forgets any earlier wait. */
    public void Start(double nowSeconds) {
        mActive = true;
        mDone = false;
        mTimedOut = false;
        mStartSeconds = nowSeconds;
        mLastSeconds = nowSeconds;
        mInBandSinceSeconds = Double.NaN;
        mLastErrorRpm = 0;
    }

    /** Stop waiting. IsActive() and IsDone() are false until the next Start(). */
    public void Cancel() {
        mActive = false;
        mDone = false;
        mTimedOut = false;
        mInBandSinceSeconds = Double.NaN;
    }

    /** Count it as settled right away: for a wheel that is not there to wait on. */
    public void Finish() {
        mActive = true;
        mDone = true;
        mTimedOut = false;
    }

    /**
     * One check. Does nothing unless started.
     *
     * @param bandRpm        how close to the target counts as "on it", either way
     * @param holdSeconds    how long it must stay in the band without a break
     * @param timeoutSeconds after this long since Start(), it is done anyway
     * @return true once done (settled or timed out)
     */
    public boolean Update(double nowSeconds, double measuredRpm, double targetRpm,
                          double bandRpm, double holdSeconds, double timeoutSeconds) {
        if (!mActive) {
            return false;
        }
        if (mDone) {
            return true;
        }
        mLastSeconds = nowSeconds;
        mLastErrorRpm = measuredRpm - targetRpm;
        if (Math.abs(mLastErrorRpm) <= bandRpm) {
            if (Double.isNaN(mInBandSinceSeconds)) {
                mInBandSinceSeconds = nowSeconds;
            }
            if (nowSeconds - mInBandSinceSeconds >= holdSeconds) {
                mDone = true;
                return true;
            }
        } else {
            // Out of the band: the hold starts over next time it comes in.
            mInBandSinceSeconds = Double.NaN;
        }
        if (nowSeconds - mStartSeconds >= timeoutSeconds) {
            mDone = true;
            mTimedOut = true;
        }
        return mDone;
    }

    /** true from Start() until Cancel(), settled or not. */
    public boolean IsActive() {
        return mActive;
    }

    /** true once settled, or timed out. */
    public boolean IsDone() {
        return mActive && mDone;
    }

    /** true when it was done by the timeout, not by settling. */
    public boolean TimedOut() {
        return mActive && mTimedOut;
    }

    /** Measured minus target at the last check, in RPM. */
    public double LastErrorRpm() {
        return mLastErrorRpm;
    }

    /** Seconds since Start(), as of the last check. */
    public double WaitedSeconds() {
        return mLastSeconds - mStartSeconds;
    }

    /** One line for the Driver Station. */
    public String Status() {
        if (!mActive) {
            return "not settling";
        }
        if (mTimedOut) {
            return String.format(Locale.US, "TIMED OUT after %.2f s, %+.0f RPM off", WaitedSeconds(), mLastErrorRpm);
        }
        if (mDone) {
            return String.format(Locale.US, "SETTLED in %.2f s, %+.0f RPM off", WaitedSeconds(), mLastErrorRpm);
        }
        return String.format(Locale.US, "settling, %+.0f RPM off, %.2f s", mLastErrorRpm, WaitedSeconds());
    }
}
