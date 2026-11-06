package org.firstinspires.ftc.teamcode.objects;

/**
 * Picks a motor power that holds a wheel at a target speed.
 *
 * <p>Power is made of three parts, added together:
 * <ul>
 *   <li><b>Feed forward (kF)</b>: a guess from the target alone. "To spin at 2000 RPM, this
 *       wheel usually needs about 0.5 power." This part does most of the work.
 *   <li><b>Proportional (kP)</b>: fixes what is wrong right now. The further below the
 *       target, the more extra power.
 *   <li><b>Integral (kI)</b>: fixes what has been wrong for a while. If the wheel stays a
 *       little slow, this part slowly grows until it catches up.
 * </ul>
 *
 * <p>This is the same math Decode's launcher used (FTCLib's PIDFController, which adds
 * {@code kF x target}), without the D part, which Decode set to 0. No hardware here, so
 * tests can check it on a laptop.
 */
public class VelocityController {
    private double mKF;
    private double mKP;
    private double mKI;

    private double mTargetRpm = 0;
    /** Error added up over time (RPM x seconds). Starts over when the target changes. */
    private double mErrorOverTime = 0;

    // The three parts of the last power, kept so the screen and the dashboard can show
    // which part is doing the work. Tuning is mostly reading these.
    private double mLastError = 0;
    private double mLastFeedForward = 0;
    private double mLastProportional = 0;
    private double mLastIntegral = 0;

    public VelocityController(double kF, double kP, double kI) {
        mKF = kF;
        mKP = kP;
        mKI = kI;
    }

    /**
     * Change the gains. Called every update, so a gain changed on the dashboard counts on
     * the next update.
     */
    public void SetGains(double kF, double kP, double kI) {
        mKF = kF;
        mKP = kP;
        mKI = kI;
    }

    /** Change the target speed. The integral starts over, so old error doesn't carry. */
    public void SetTargetRpm(double targetRpm) {
        if (targetRpm != mTargetRpm) {
            mTargetRpm = targetRpm;
            mErrorOverTime = 0;
        }
    }

    public double GetTargetRpm() {
        return mTargetRpm;
    }

    /** Forget the error added up so far. Call when the motor starts again after a stop. */
    public void ClearIntegral() {
        mErrorOverTime = 0;
    }

    /**
     * Work out the power for this update.
     *
     * @param measuredRpm   how fast the wheel is spinning now
     * @param secondsPassed time since the last update
     * @return motor power from 0 to 1. Never negative: a launcher only spins one way, and
     *     braking it hard would waste battery and wear out the gears.
     */
    public double Calculate(double measuredRpm, double secondsPassed) {
        double error = mTargetRpm - measuredRpm;
        mErrorOverTime += error * secondsPassed;

        double feedForward = mKF * mTargetRpm;
        double proportional = mKP * error;
        double integral = mKI * mErrorOverTime;
        // Like Decode: the integral part can never add or remove more than full power.
        integral = Math.max(-1, Math.min(integral, 1));

        mLastError = error;
        mLastFeedForward = feedForward;
        mLastProportional = proportional;
        mLastIntegral = integral;

        double power = feedForward + proportional + integral;
        return Math.max(0, Math.min(power, 1));
    }

    /** Target minus measured, at the last update. Plus means too slow. */
    public double GetLastError() {
        return mLastError;
    }

    /** The kF part of the last power. */
    public double GetLastFeedForward() {
        return mLastFeedForward;
    }

    /** The kP part of the last power. */
    public double GetLastProportional() {
        return mLastProportional;
    }

    /** The kI part of the last power. */
    public double GetLastIntegral() {
        return mLastIntegral;
    }
}
