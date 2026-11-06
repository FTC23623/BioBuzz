package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.VelocityController;

/**
 * One launcher motor, held at a speed by our own control loop (docs/decisions/0009).
 * {@link Launcher} has two of these, and so does {@link MotorTuner}, so a speed found in
 * one works in the other.
 *
 * <p>Not a {@link org.firstinspires.ftc.teamcode.objects.Subsystem} by itself: the part
 * that owns it calls {@link #Update} every 50 ms and {@link #ShowStatus} every loop.
 *
 * <p>A robot with no motor by this name still runs: the wheel does nothing, and the Driver
 * Station says it is missing.
 *
 * <p><b>Wrong way.</b> Our speed control never pushes backward, so a motor set the wrong way
 * would climb to full power. A wheel that stays backward ({@code launcherWrongWayRpm}) for
 * {@code launcherWrongWaySeconds} while powered is stopped. A short reverse, like a ball
 * kicking the wheel, is fine. Turning the wheel off and on again clears the stop
 * (docs/decisions/0029).
 */
public class LauncherWheel {

    private final String mName;
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    /** null when the motor is not in the robot configuration. */
    private final DcMotorEx mMotor;
    private final String mConfigName;
    private final VelocityController mSpeedControl;

    private boolean mOn = false;
    private boolean mWasOn = false;
    private boolean mWrongWay = false;
    /** How long the wheel has been spinning backward without a break while powered. */
    private double mBackwardSeconds = 0;
    private double mMeasuredRpm = 0;
    private double mPower = 0;

    public LauncherWheel(String name, HydraOpMode op, BotProperties properties,
                         String configName, DcMotor.Direction direction) {
        mName = name;
        mOp = op;
        mProperties = properties;
        mConfigName = configName;
        mMotor = op.mHardwareMap.tryGet(DcMotorEx.class, configName);
        if (mMotor != null) {
            // Our code picks the power, the encoder only measures speed. Coast when off.
            mMotor.setDirection(direction);
            mMotor.setPower(0);
            mMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            mMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            mMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        }
        mSpeedControl = new VelocityController(
                properties.launcherKF, properties.launcherKP, properties.launcherKI);
    }

    /**
     * Which way to set a launcher motor: a "reversed" setting in BotProperties means REVERSE,
     * otherwise the normal way, FORWARD. The same as the intake and transfer.
     */
    public static DcMotor.Direction Direction(boolean reversed) {
        if (reversed) {
            return DcMotor.Direction.REVERSE;
        }
        return DcMotor.Direction.FORWARD;
    }

    /** true: spin at the target. false: power off, coast down, and clear any wrong-way stop. */
    public void SetOn(boolean on) {
        mOn = on;
        if (!on) {
            mWrongWay = false;
            mBackwardSeconds = 0;
        }
    }

    public boolean IsOn() {
        return mOn;
    }

    /**
     * Measure the speed and set the power. The owner calls this every 50 ms.
     *
     * @param targetRpm     the speed to hold, read fresh each time so a D-pad press or a
     *                      dashboard change counts right away
     * @param secondsPassed time since the last update
     */
    public void Update(double targetRpm, double secondsPassed) {
        if (mMotor == null) {
            return;
        }
        double ticksPerSecond = mMotor.getVelocity();
        mMeasuredRpm = ticksPerSecond / mProperties.launcherTicksPerRev * 60;
        // Backward while powered: count how long. Off, or forward again: start over.
        if (mOn && mMeasuredRpm < mProperties.launcherWrongWayRpm) {
            mBackwardSeconds = mBackwardSeconds + secondsPassed;
        } else {
            mBackwardSeconds = 0;
        }
        if (mBackwardSeconds >= mProperties.launcherWrongWaySeconds) {
            // Backward for too long: it is set the wrong way. Stop before it runs away.
            mWrongWay = true;
        }

        // Read the gains every time, so a change on the dashboard counts right away.
        mSpeedControl.SetGains(mProperties.launcherKF, mProperties.launcherKP, mProperties.launcherKI);
        mSpeedControl.SetTargetRpm(targetRpm);
        if (mOn && !mWrongWay) {
            if (!mWasOn) {
                // Starting again: don't carry over error from the last time it ran.
                mSpeedControl.ClearIntegral();
            }
            mPower = mSpeedControl.Calculate(mMeasuredRpm, secondsPassed);
        } else {
            mPower = 0;
        }
        mWasOn = mOn;
        mMotor.setPower(mPower);
    }

    public double GetMeasuredRpm() {
        return mMeasuredRpm;
    }

    public double GetPower() {
        return mPower;
    }

    /** true when the motor is in the robot configuration. */
    public boolean IsPresent() {
        return mMotor != null;
    }

    /** true once the wheel was backward too long. Turning it off clears this. */
    public boolean IsStoppedWrongWay() {
        return mWrongWay;
    }

    /** true when on and spinning within launcherAtSpeedRpm of the target. */
    public boolean AtSpeed() {
        if (!mOn || mMotor == null) {
            return false;
        }
        double difference = Math.abs(mSpeedControl.GetTargetRpm() - mMeasuredRpm);
        return difference <= mProperties.launcherAtSpeedRpm;
    }

    /**
     * Speed, power and the three parts of the power, one line each so the dashboard can
     * graph every one. Graph "target" against "actual" to tune.
     */
    public void ShowStatus() {
        if (mMotor == null) {
            mOp.mTelemetry.addLine(mName + ": MISSING. Nothing named '" + mConfigName
                    + "' in the robot configuration.");
            return;
        }
        if (mWrongWay) {
            mOp.mTelemetry.addLine(mName.toUpperCase() + " SPUN THE WRONG WAY FOR "
                    + mProperties.launcherWrongWaySeconds + " s. Stopped. Turn the launchers off "
                    + "and on to try again. If it keeps happening, flip its direction in BotProperties.");
        } else if (mBackwardSeconds > 0) {
            mOp.mTelemetry.addData(mName + " backward for", "%.2f s (allowed %.2f)",
                    mBackwardSeconds, mProperties.launcherWrongWaySeconds);
        }
        String state = "off";
        if (mOn) {
            state = "ON";
        }
        mOp.mTelemetry.addData(mName, state);
        mOp.mTelemetry.addData(mName + " target RPM", "%.0f", mSpeedControl.GetTargetRpm());
        mOp.mTelemetry.addData(mName + " actual RPM", "%.0f", mMeasuredRpm);
        mOp.mTelemetry.addData(mName + " error RPM", "%.0f", mSpeedControl.GetLastError());
        mOp.mTelemetry.addData(mName + " power", "%.3f", mPower);
        mOp.mTelemetry.addData(mName + " power parts F / P / I", "%.3f / %.3f / %.3f",
                mSpeedControl.GetLastFeedForward(), mSpeedControl.GetLastProportional(),
                mSpeedControl.GetLastIntegral());
        mOp.mTelemetry.addData(mName + " at speed", AtSpeed());
    }
}
