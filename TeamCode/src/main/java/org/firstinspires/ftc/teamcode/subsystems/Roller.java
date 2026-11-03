package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;

/**
 * One roller motor (the intake or the transfer), held at a speed by the hub's own speed
 * control. The gains (rollerKP, rollerKI, rollerKD, rollerKF) live in BotProperties and can
 * be changed on the dashboard while the robot runs (docs/decisions/0019 and 0025).
 * {@link Intake} has two, and so does {@link MotorTuner}.
 *
 * <p>A plus speed pulls balls in or up. A minus speed runs the roller backward, to spit
 * balls out.
 *
 * <p>A robot with no motor by this name still runs: the roller does nothing, and the Driver
 * Station says it is missing.
 */
public class Roller {
    private final String mName;
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final String mConfigName;
    /** null when the motor is not in the robot configuration. */
    private final DcMotorEx mMotor;
    /** The gains the hub had before we sent ours, read once at INIT, so they can be shown. */
    private final PIDFCoefficients mHubGains;
    /** The gains last sent to the hub. null until the first send. */
    private PIDFCoefficients mSentGains = null;

    private double mTargetRpm = 0;
    private boolean mEverSet = false;

    public Roller(String name, HydraOpMode op, BotProperties properties, String configName,
                  boolean reversed) {
        mName = name;
        mOp = op;
        mProperties = properties;
        mConfigName = configName;
        mMotor = op.mHardwareMap.tryGet(DcMotorEx.class, configName);
        if (mMotor == null) {
            mHubGains = null;
            return;
        }
        if (reversed) {
            mMotor.setDirection(DcMotor.Direction.REVERSE);
        } else {
            mMotor.setDirection(DcMotor.Direction.FORWARD);
        }
        mMotor.setPower(0);
        mMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        mMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        mMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        mHubGains = mMotor.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER);
        SendGains();
    }

    /**
     * Send the gains from BotProperties to the hub, but only when one has changed. Each
     * message to the hub takes time out of the loop. Called every loop, so a change on the
     * dashboard counts right away.
     */
    private void SendGains() {
        if (mMotor == null) {
            return;
        }
        PIDFCoefficients wanted = new PIDFCoefficients(mProperties.rollerKP,
                mProperties.rollerKI, mProperties.rollerKD, mProperties.rollerKF);
        if (mSentGains != null && wanted.p == mSentGains.p && wanted.i == mSentGains.i
                && wanted.d == mSentGains.d && wanted.f == mSentGains.f) {
            return;
        }
        mMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, wanted);
        mSentGains = wanted;
    }

    /** Spin at this speed. Minus is backward. 0 is off (coast). */
    public void SetRpm(double rpm) {
        SendGains();
        // Each message to the hub takes time out of the loop, so only send a change.
        if (mEverSet && rpm == mTargetRpm) {
            return;
        }
        mEverSet = true;
        mTargetRpm = rpm;
        if (mMotor == null) {
            return;
        }
        if (rpm == 0) {
            mMotor.setPower(0);
        } else {
            mMotor.setVelocity(RpmToTicksPerSecond(rpm));
        }
    }

    public double GetTargetRpm() {
        return mTargetRpm;
    }

    public double GetMeasuredRpm() {
        if (mMotor == null) {
            return 0;
        }
        return mMotor.getVelocity() / mProperties.intakeTicksPerRev * 60;
    }

    /** The hub counts speed in encoder ticks per second, not RPM. */
    private double RpmToTicksPerSecond(double rpm) {
        return rpm / 60 * mProperties.intakeTicksPerRev;
    }

    /** Target, actual and power, one line each so the dashboard can graph them. */
    public void ShowStatus() {
        if (mMotor == null) {
            mOp.mTelemetry.addLine(mName + ": MISSING. Nothing named '" + mConfigName
                    + "' in the robot configuration.");
            return;
        }
        mOp.mTelemetry.addData(mName + " target RPM", "%.0f", mTargetRpm);
        mOp.mTelemetry.addData(mName + " actual RPM", "%.0f", GetMeasuredRpm());
        mOp.mTelemetry.addData(mName + " power", "%.3f", mMotor.getPower());
        if (mSentGains != null) {
            mOp.mTelemetry.addData(mName + " gains P / I / D / F", "%.2f / %.2f / %.2f / %.2f",
                    mSentGains.p, mSentGains.i, mSentGains.d, mSentGains.f);
        }
        if (mHubGains != null) {
            mOp.mTelemetry.addData(mName + " hub started with P / I / D / F",
                    "%.2f / %.2f / %.2f / %.2f",
                    mHubGains.p, mHubGains.i, mHubGains.d, mHubGains.f);
        }
    }
}
