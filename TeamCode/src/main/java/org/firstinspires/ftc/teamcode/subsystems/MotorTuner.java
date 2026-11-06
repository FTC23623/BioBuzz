package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * Spin the intake, transfer and both launchers at speeds picked from the gamepad, to find
 * the right RPM for each one. Only Test: Motor Tuning uses this.
 *
 * <p>Controls (gamepad 1). Each button turns its motor on, then off on the next press, and
 * picks that motor for the D-pad:
 * <ul>
 *   <li><b>Triangle / Y</b>: pollen launcher. <b>Circle / B</b>: nectar launcher.
 *   <li><b>Cross / A</b>: intake. <b>Square / X</b>: transfer.
 *   <li><b>D-pad up / down</b>: the picked motor's target up / down (50 RPM for a launcher,
 *       50 for the intake or transfer).
 * </ul>
 *
 * <p>Every motor has its own speed (docs/decisions/0023). The speeds ARE the fields in
 * BotProperties ({@code pollenLauncherRpm}, {@code nectarLauncherRpm}, {@code intakeRpm},
 * {@code transferRpm}), so the dashboard shows each change, and a number typed on the
 * dashboard reaches the motor too.
 *
 * <p>How each motor holds its speed (docs/decisions/0019):
 * <ul>
 *   <li><b>Launchers</b>: our own control loop, through {@link LauncherWheel}, the same as
 *       {@link Launcher} (docs/decisions/0009).
 *   <li><b>Intake and transfer</b>: the hub's built-in speed control, through {@link Roller}.
 * </ul>
 */
public class MotorTuner implements Subsystem {
    // Which motor the D-pad changes.
    static final int POLLEN = 0;
    static final int NECTAR = 1;
    static final int INTAKE = 2;
    static final int TRANSFER = 3;
    private static final String[] NAMES = {"Pollen launcher", "Nectar launcher", "Intake", "Transfer"};

    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    private final ElapsedTime mTimeSinceUpdate = new ElapsedTime();

    private final LauncherWheel mPollen;
    private final LauncherWheel mNectar;
    private final Roller mIntake;
    private final Roller mTransfer;

    private final Debouncer mPollenButton;
    private final Debouncer mNectarButton;
    private final Debouncer mIntakeButton;
    private final Debouncer mTransferButton;
    private final Debouncer mFasterButton;
    private final Debouncer mSlowerButton;

    private int mPicked = POLLEN;
    private boolean mIntakeOn = false;
    private boolean mTransferOn = false;

    public MotorTuner(HydraOpMode op, BotProperties properties, Gamepad gamepad) {
        mOp = op;
        mProperties = properties;
        mGamepad = gamepad;

        // Both launchers are set up exactly like Launcher does it.
        mNectar = new LauncherWheel("Nectar launcher", op, properties, properties.cfgLaunchNectar,
                LauncherWheel.Direction(properties.nectarLauncherReversed));
        mPollen = new LauncherWheel("Pollen launcher", op, properties, properties.cfgLaunchPollen,
                LauncherWheel.Direction(properties.pollenLauncherReversed));

        mIntake = new Roller("Intake", op, properties, properties.cfgIntake, properties.intakeReversed);
        mTransfer = new Roller("Transfer", op, properties, properties.cfgTransfer, properties.transferReversed);

        mPollenButton = new Debouncer(properties.debounce);
        mNectarButton = new Debouncer(properties.debounce);
        mIntakeButton = new Debouncer(properties.debounce);
        mTransferButton = new Debouncer(properties.debounce);
        mFasterButton = new Debouncer(properties.debounce);
        mSlowerButton = new Debouncer(properties.debounce);
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
        // Each press flips one motor on or off, however long it is held, and picks it.
        if (Pressed(mPollenButton, mGamepad.triangle)) {
            mPollen.SetOn(!mPollen.IsOn());
            mPicked = POLLEN;
        }
        if (Pressed(mNectarButton, mGamepad.circle)) {
            mNectar.SetOn(!mNectar.IsOn());
            mPicked = NECTAR;
        }
        if (Pressed(mIntakeButton, mGamepad.cross)) {
            mIntakeOn = !mIntakeOn;
            mPicked = INTAKE;
        }
        if (Pressed(mTransferButton, mGamepad.square)) {
            mTransferOn = !mTransferOn;
            mPicked = TRANSFER;
        }

        if (Pressed(mFasterButton, mGamepad.dpad_up)) {
            ChangePickedRpm(1);
        }
        if (Pressed(mSlowerButton, mGamepad.dpad_down)) {
            ChangePickedRpm(-1);
        }
    }

    /** true once per press: the button was held long enough, and this press is now used up. */
    private static boolean Pressed(Debouncer button, boolean down) {
        button.In(down);
        if (button.Out()) {
            button.Used();
            return true;
        }
        return false;
    }

    @Override
    public void Process() {
        // Update every 50 ms, like Launcher. Measuring more often gives noisy speeds.
        double secondsPassed = mTimeSinceUpdate.seconds();
        if (secondsPassed * 1000 >= mProperties.launcherUpdateMs) {
            mTimeSinceUpdate.reset();
            Update(secondsPassed);
        }
        ShowStatus();
    }

    /** Measure every speed and set every motor. Process() calls this every 50 ms. */
    void Update(double secondsPassed) {
        mPollen.Update(mProperties.pollenLauncherRpm, secondsPassed);
        mNectar.Update(mProperties.nectarLauncherRpm, secondsPassed);
        if (mIntakeOn) {
            mIntake.SetRpm(mProperties.intakeRpm);
        } else {
            mIntake.SetRpm(0);
        }
        if (mTransferOn) {
            mTransfer.SetRpm(mProperties.transferRpm);
        } else {
            mTransfer.SetRpm(0);
        }
    }

    /**
     * One step up (+1) or down (-1) for the picked motor, staying between 0 and its maximum.
     * The step and the maximum are the launchers' or the rollers'.
     */
    void ChangePickedRpm(int direction) {
        if (mPicked == POLLEN) {
            mProperties.pollenLauncherRpm = KeepLauncherSafe(
                    mProperties.pollenLauncherRpm + direction * mProperties.launcherRpmStep);
        } else if (mPicked == NECTAR) {
            mProperties.nectarLauncherRpm = KeepLauncherSafe(
                    mProperties.nectarLauncherRpm + direction * mProperties.launcherRpmStep);
        } else if (mPicked == INTAKE) {
            mProperties.intakeRpm = KeepRollerSafe(
                    mProperties.intakeRpm + direction * mProperties.intakeRpmStep);
        } else {
            mProperties.transferRpm = KeepRollerSafe(
                    mProperties.transferRpm + direction * mProperties.intakeRpmStep);
        }
    }

    private double KeepLauncherSafe(double rpm) {
        return Math.max(0, Math.min(rpm, mProperties.launcherMaxRpm));
    }

    private double KeepRollerSafe(double rpm) {
        return Math.max(0, Math.min(rpm, mProperties.intakeMaxRpm));
    }

    /** The name of the motor the D-pad changes now. */
    public String GetPickedName() {
        return NAMES[mPicked];
    }

    private void ShowStatus() {
        mOp.mTelemetry.addData("D-pad up/down changes", GetPickedName());
        mOp.mTelemetry.addData("Speeds (pollen / nectar / intake / transfer)", "%.0f / %.0f / %.0f / %.0f RPM",
                mProperties.pollenLauncherRpm, mProperties.nectarLauncherRpm,
                mProperties.intakeRpm, mProperties.transferRpm);
        mPollen.ShowStatus();
        mNectar.ShowStatus();
        mIntake.ShowStatus();
        mTransfer.ShowStatus();
    }
}
