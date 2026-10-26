package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.objects.VelocityController;

/**
 * The nectar launcher: one wheel, held at a target speed.
 *
 * <p>Controls:
 * <ul>
 *   <li><b>Hold right trigger</b>: spin the launcher at the target speed. Let go: power off.
 *   <li><b>Right bumper</b>: target speed up 50 RPM. <b>Left bumper</b>: down 50 RPM.
 * </ul>
 *
 * <p>The target normally starts at 1000 RPM. Every 50 ms (while the trigger is held), the
 * launcher measures the wheel's speed and asks the {@link VelocityController} for a new
 * power. Every number here is a launcher setting in BotProperties.
 * See docs/decisions/0009-launcher-velocity-control.md.
 *
 * <p>Ported from Decode subsystems/Launcher.java and objects/LaunchMotor.java: the speed
 * loop only. Turret, vision, auto-range, the feed servo and Road Runner actions come later.
 */
public class Launcher implements Subsystem {
    private final HydraOpMode mOp;
    private final String mLabel;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    private final DcMotorEx mMotor;
    private final VelocityController mSpeedControl;
    private final Debouncer mFasterButton;
    private final Debouncer mSlowerButton;
    private final ElapsedTime mTimeSinceUpdate;

    private boolean mTriggerHeld = false;
    private boolean mWasRunning = false;
    private double mMeasuredRpm = 0;
    private double mPower = 0;

    /** The nectar launcher. */
    public Launcher(HydraOpMode op, BotProperties properties, Gamepad gamepad) {
        this(op, properties, gamepad, "Launcher", properties.cfgLaunchNectar,
                properties.launcherStartRpm, true);
    }

    /**
     * Any launcher wheel: same speed control and gains, its own motor name, label on the
     * Driver Station and starting speed. The pollen launcher is built this way.
     *
     * @param label    shown before each telemetry line, for example "Pollen launcher"
     * @param reversed true: the motor spins the other way, as the nectar launcher's does
     */
    public Launcher(HydraOpMode op, BotProperties properties, Gamepad gamepad,
                    String label, String motorName, double startRpm, boolean reversed) {
        mOp = op;
        mLabel = label;
        mProperties = properties;
        mGamepad = gamepad;
        mMotor = op.mHardwareMap.get(DcMotorEx.class, motorName);
        // Same set-up as Decode: our code picks the power, the encoder only measures speed.
        mMotor.setDirection(reversed ? DcMotor.Direction.REVERSE : DcMotor.Direction.FORWARD);
        mMotor.setPower(0);
        mMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        mMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        // FLOAT: when the power stops, the wheel coasts down instead of braking hard.
        mMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        mSpeedControl = new VelocityController(
                mProperties.launcherKF, mProperties.launcherKP, mProperties.launcherKI);
        mSpeedControl.SetTargetRpm(startRpm);
        mFasterButton = new Debouncer(mProperties.debounce);
        mSlowerButton = new Debouncer(mProperties.debounce);
        mTimeSinceUpdate = new ElapsedTime();
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
        mTriggerHeld = mGamepad.right_trigger_pressed;

        // Each bumper press changes the target once, however long it is held.
        mFasterButton.In(mGamepad.right_bumper);
        if (mFasterButton.Out()) {
            mFasterButton.Used();
            ChangeTargetRpm(mProperties.launcherRpmStep);
        }
        mSlowerButton.In(mGamepad.left_bumper);
        if (mSlowerButton.Out()) {
            mSlowerButton.Used();
            ChangeTargetRpm(-mProperties.launcherRpmStep);
        }
    }

    @Override
    public void Process() {
        // Update every 50 ms, like Decode. Measuring more often than that gives noisy speeds.
        double secondsPassed = mTimeSinceUpdate.seconds();
        if (secondsPassed * 1000 >= mProperties.launcherUpdateMs) {
            mTimeSinceUpdate.reset();
            Update(secondsPassed);
        }
        ShowStatus();
    }

    /** Measure the speed and set the power. Process() calls this every 50 ms. */
    void Update(double secondsPassed) {
        mMeasuredRpm = ReadRpm();

        if (mTriggerHeld) {
            if (!mWasRunning) {
                // Starting again: don't carry over error from the last time it ran.
                mSpeedControl.ClearIntegral();
            }
            mPower = mSpeedControl.Calculate(mMeasuredRpm, secondsPassed);
        } else {
            mPower = 0;
        }
        mWasRunning = mTriggerHeld;
        mMotor.setPower(mPower);
    }

    /** Raise or lower the target speed, staying between 0 and the maximum. */
    public void ChangeTargetRpm(double change) {
        double newTarget = mSpeedControl.GetTargetRpm() + change;
        newTarget = Math.max(0, Math.min(newTarget, mProperties.launcherMaxRpm));
        mSpeedControl.SetTargetRpm(newTarget);
    }

    public double GetTargetRpm() {
        return mSpeedControl.GetTargetRpm();
    }

    public double GetMeasuredRpm() {
        return mMeasuredRpm;
    }

    /** true when the wheel is spinning within 50 RPM of the target. */
    public boolean AtSpeed() {
        double difference = Math.abs(GetTargetRpm() - mMeasuredRpm);
        return difference <= mProperties.launcherAtSpeedRpm;
    }

    /** The encoder counts ticks per second. Turn that into turns per minute (RPM). */
    private double ReadRpm() {
        double ticksPerSecond = mMotor.getVelocity();
        double turnsPerSecond = ticksPerSecond / mProperties.launcherTicksPerRev;
        return turnsPerSecond * 60;
    }

    private void ShowStatus() {
        mOp.mTelemetry.addData(mLabel + " target RPM", "%.0f", GetTargetRpm());
        mOp.mTelemetry.addData(mLabel + " actual RPM", "%.0f", mMeasuredRpm);
        mOp.mTelemetry.addData(mLabel + " power", "%.2f", mPower);
        mOp.mTelemetry.addData(mLabel + " at speed", AtSpeed());
    }
}
