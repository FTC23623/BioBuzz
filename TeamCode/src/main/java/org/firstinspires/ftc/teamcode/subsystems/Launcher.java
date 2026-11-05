package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.Locale;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The two launcher wheels, pollen and nectar, each held at its own target speed.
 *
 * <p>The launchers spin from START to the end: HyDrive and the autos call
 * {@code Bot.SpinLaunchers(true)} the moment START is pressed, so the first shot is ready,
 * and there is no button to turn them off (coach, 2026-11-02 and 2026-11-03). A launcher
 * stopped for spinning the wrong way (docs/decisions/0029) therefore stays stopped until
 * the OpMode is restarted.
 *
 * <p>Controls (operator, gamepad 2):
 * <ul>
 *   <li><b>D-pad up / down</b>: pollen launcher 50 RPM faster / slower. With range tuning
 *       on (below) this trims the distance table instead, by the same 50.
 *   <li><b>D-pad right / left</b>: nectar launcher target up / down 50 RPM.
 * </ul>
 *
 * <p><b>Speed from distance</b> (docs/decisions/0038): in TeleOp, while the driver holds aim
 * (right bumper) or launch (right trigger) and the camera sees our hive, the
 * pollen launcher's target follows a curve of inches to RPM in 2-inch
 * steps (2750 at 110 in, 3225 from 140 in, bending upward between), plus {@code launcherRangeTrimRpm}. The
 * wheel is then already at the right speed when the trigger is pulled. With no tag in view
 * the target stays where it was. {@code launcherRangeTuning = false} turns this off.
 *
 * <p>The targets ARE {@code pollenLauncherRpm} and {@code nectarLauncherRpm} in
 * BotProperties. The D-pad and the distance table change them there, so the dashboard shows
 * the change, and a change on the dashboard reaches the wheel. Every 50 ms each
 * {@link LauncherWheel} measures its speed and picks a new power. See docs/decisions/0009
 * and 0023.
 *
 * <p>The balls are let go by {@link LaunchGates}, on the driver's right trigger.
 */
public class Launcher implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    /** The camera, for the speed-from-distance table. null when there is none. */
    private final Vision mVision;
    private final LauncherWheel mPollen;
    private final LauncherWheel mNectar;
    private final Debouncer mPollenFasterButton;
    private final Debouncer mPollenSlowerButton;
    private final Debouncer mNectarFasterButton;
    private final Debouncer mNectarSlowerButton;
    private final ElapsedTime mTimeSinceUpdate;

    private boolean mSpinning = false;
    // TeleOp only: the camera sets the speed only while the driver holds aim or launch
    // (coach, 2026-11-03). An auto never calls HandleUserInput, so it keeps following.
    private boolean mFromGamepad = false;
    private boolean mDriverAiming = false;
    /** What the distance table last said, for the Driver Station. */
    private String mRangeStatus = "no tag seen yet";

    /**
     * @param vision the camera, so the pollen launcher's speed can follow the distance to the
     *               tag. null for a robot with no camera: the table is then never used.
     */
    public Launcher(HydraOpMode op, BotProperties properties, Gamepad gamepad, Vision vision) {
        mOp = op;
        mProperties = properties;
        mGamepad = gamepad;
        mVision = vision;

        // FORWARD, unless BotProperties says a motor is reversed.
        mPollen = new LauncherWheel("Pollen launcher", op, properties, properties.cfgLaunchPollen,
                LauncherWheel.Direction(properties.pollenLauncherReversed));
        mNectar = new LauncherWheel("Nectar launcher", op, properties, properties.cfgLaunchNectar,
                LauncherWheel.Direction(properties.nectarLauncherReversed));

        mPollenFasterButton = new Debouncer(properties.debounce);
        mPollenSlowerButton = new Debouncer(properties.debounce);
        mNectarFasterButton = new Debouncer(properties.debounce);
        mNectarSlowerButton = new Debouncer(properties.debounce);
        mTimeSinceUpdate = new ElapsedTime();
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
        mFromGamepad = true;
        Gamepad driver = mOp.mDriverGamepad;
        mDriverAiming = driver.right_bumper || driver.right_trigger > mProperties.trgBtnThresh;
        // Each press counts once, however long it is held. With range tuning on, up / down
        // nudge the whole distance table; the table then sets the target. Off, they set the
        // target itself.
        if (Pressed(mPollenFasterButton, mGamepad.dpad_up)) {
            if (mProperties.launcherRangeTuning) {
                mProperties.launcherRangeTrimRpm = mProperties.launcherRangeTrimRpm + mProperties.launcherRpmStep;
            } else {
                ChangePollenRpm(mProperties.launcherRpmStep);
            }
        }
        if (Pressed(mPollenSlowerButton, mGamepad.dpad_down)) {
            if (mProperties.launcherRangeTuning) {
                mProperties.launcherRangeTrimRpm = mProperties.launcherRangeTrimRpm - mProperties.launcherRpmStep;
            } else {
                ChangePollenRpm(-mProperties.launcherRpmStep);
            }
        }
        if (Pressed(mNectarFasterButton, mGamepad.dpad_right)) {
            ChangeNectarRpm(mProperties.launcherRpmStep);
        }
        if (Pressed(mNectarSlowerButton, mGamepad.dpad_left)) {
            ChangeNectarRpm(-mProperties.launcherRpmStep);
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
        FollowDistance();
        // Update every 50 ms, like Decode. Measuring more often than that gives noisy speeds.
        double secondsPassed = mTimeSinceUpdate.seconds();
        if (secondsPassed * 1000 >= mProperties.launcherUpdateMs) {
            mTimeSinceUpdate.reset();
            Update(secondsPassed);
        }
        if (!mOp.mQuiet) {
            ShowStatus();
        } else if (mProperties.launcherOnlyTelemetry) {
            // Everything else is hushed; the launchers keep their numbers, for tuning.
            ShowStats();
        }
    }

    /**
     * The pollen launcher's target from the distance to the tag, while a tag of ours is in
     * view. With no tag the target stays where it was, so a shot can still be taken.
     */
    private void FollowDistance() {
        if (!mProperties.launcherRangeTuning) {
            mRangeStatus = "OFF (launcherRangeTuning): D-pad sets the speed";
            return;
        }
        if (mFromGamepad && !mDriverAiming) {
            // TeleOp, no aim button down: the camera is not tracking, so the speed holds.
            mRangeStatus = String.format(Locale.US, "not tracking (hold aim or launch), holding %.0f",
                    mProperties.pollenLauncherRpm);
            return;
        }
        if (mVision == null || !mVision.SeesTag()) {
            mRangeStatus = String.format(Locale.US, "no tag in view, holding %.0f", mProperties.pollenLauncherRpm);
            return;
        }
        double rangeIn = mVision.GetRange();
        if (rangeIn <= 0) {
            // The camera sees the tag but could not work out how far it is.
            mRangeStatus = String.format(Locale.US, "tag seen, no distance, holding %.0f", mProperties.pollenLauncherRpm);
            return;
        }
        double tableRpm = RpmForRange(rangeIn, mProperties);
        mProperties.pollenLauncherRpm = KeepInRange(tableRpm + mProperties.launcherRangeTrimRpm);
        mRangeStatus = String.format(Locale.US, "%.0f in -> %.0f RPM, trim %+.0f = %.0f",
                rangeIn, tableRpm, mProperties.launcherRangeTrimRpm, mProperties.pollenLauncherRpm);
    }

    /**
     * The RPM for a distance, from the curve through the two measured shots in BotProperties,
     * in steps of launchBandIn inches:
     *
     * <pre>
     *     step start = nearIn + whole steps past nearIn
     *     rpm = near + (far - near) x ((step start - nearIn) / (farIn - nearIn)) ^ curve
     *
     *     near 110 in 2750, far 140 in 3225, curve 1.1, steps of 2 in:
     *     110: 2750   112: 2774   120: 2892   130: 3054   139: 3190   140 and up: 3225
     * </pre>
     *
     * Nearer than nearIn: the near speed. Farther than farIn: the far speed.
     *
     * @return whole RPM
     */
    public static double RpmForRange(double rangeIn, BotProperties p) {
        if (rangeIn <= p.launchNearIn) {
            return p.launchNearRpm;
        }
        if (rangeIn >= p.launchFarIn) {
            return p.launchFarRpm;
        }
        double stepStart = rangeIn;
        if (p.launchBandIn > 0) {
            double steps = Math.floor((rangeIn - p.launchNearIn) / p.launchBandIn);
            stepStart = p.launchNearIn + steps * p.launchBandIn;
        }
        double fraction = (stepStart - p.launchNearIn) / (p.launchFarIn - p.launchNearIn);
        double rpm = p.launchNearRpm + (p.launchFarRpm - p.launchNearRpm) * Math.pow(fraction, p.launchCurve);
        return Math.round(rpm);
    }

    /** Measure both speeds and set both powers. Process() calls this every 50 ms. */
    void Update(double secondsPassed) {
        mPollen.Update(mProperties.pollenLauncherRpm, secondsPassed);
        mNectar.Update(mProperties.nectarLauncherRpm, secondsPassed);
    }

    /**
     * true: both wheels spin at their targets. false: both off. HyDrive and the autos call
     * this with true at START. Nothing on a gamepad calls it with false.
     */
    public void SetSpinning(boolean spinning) {
        mSpinning = spinning;
        mPollen.SetOn(spinning);
        mNectar.SetOn(spinning);
    }

    public boolean IsSpinning() {
        return mSpinning;
    }

    /** Raise or lower the pollen target, staying between 0 and the maximum. */
    public void ChangePollenRpm(double change) {
        mProperties.pollenLauncherRpm = KeepInRange(mProperties.pollenLauncherRpm + change);
    }

    /** Raise or lower the nectar target, staying between 0 and the maximum. */
    public void ChangeNectarRpm(double change) {
        mProperties.nectarLauncherRpm = KeepInRange(mProperties.nectarLauncherRpm + change);
    }

    private double KeepInRange(double rpm) {
        return Math.max(0, Math.min(rpm, mProperties.launcherMaxRpm));
    }

    public double GetPollenTargetRpm() {
        return mProperties.pollenLauncherRpm;
    }

    public double GetNectarTargetRpm() {
        return mProperties.nectarLauncherRpm;
    }

    /** true when the pollen wheel spins at least this fast, or is not in the configuration. */
    public boolean PollenAtLeast(double rpm) {
        if (!mPollen.IsPresent()) {
            return true;
        }
        return mPollen.GetMeasuredRpm() >= rpm;
    }

    public double GetPollenMeasuredRpm() {
        return mPollen.GetMeasuredRpm();
    }

    public double GetNectarMeasuredRpm() {
        return mNectar.GetMeasuredRpm();
    }

    /**
     * true when every wheel on the robot is spinning within launcherAtSpeedRpm of its target.
     * A wheel missing from the robot configuration does not count, so it can't hold up a launch.
     * With neither wheel there, false.
     */
    public boolean AtSpeed() {
        if (!mPollen.IsPresent() && !mNectar.IsPresent()) {
            return false;
        }
        boolean pollenReady = !mPollen.IsPresent() || mPollen.AtSpeed();
        boolean nectarReady = !mNectar.IsPresent() || mNectar.AtSpeed();
        return pollenReady && nectarReady;
    }

    private void ShowStatus() {
        mOp.mTelemetry.addLine("Launchers (gamepad 2): always on from START. D-pad up/down pollen, right/left nectar");
        ShowStats();
    }

    /** The numbers only: speed from distance, and each wheel's speeds and power. */
    private void ShowStats() {
        mOp.mTelemetry.addData("Launcher speed from distance", mRangeStatus);
        mPollen.ShowStatus();
        mNectar.ShowStatus();
    }
}
