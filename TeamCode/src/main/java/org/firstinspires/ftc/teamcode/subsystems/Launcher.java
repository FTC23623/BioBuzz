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
 * <p>The launchers spin from START: HyDrive and the autos call {@code Bot.SpinLaunchers(true)}
 * the moment START is pressed, so the first shot is ready (coach, 2026-11-02).
 *
 * <p>Controls (operator, gamepad 2):
 * <ul>
 *   <li><b>Right bumper</b>: both launchers off. Press again: both back on.
 *   <li><b>D-pad up / down</b>: pollen launcher 50 RPM faster / slower. With range tuning
 *       on (below) this trims the distance table instead, by the same 50.
 *   <li><b>D-pad right / left</b>: nectar launcher target up / down 50 RPM.
 * </ul>
 *
 * <p><b>Speed from distance</b> (docs/decisions/0038): while the camera sees our hive, the
 * pollen launcher's target follows {@code launchRpmByRangeIn}, a table of inches to RPM in
 * 10-inch bands (2900 at 110 in, 3300 from 140 in), plus {@code launcherRangeTrimRpm}. The
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
    private final Debouncer mSpinButton;
    private final Debouncer mPollenFasterButton;
    private final Debouncer mPollenSlowerButton;
    private final Debouncer mNectarFasterButton;
    private final Debouncer mNectarSlowerButton;
    private final ElapsedTime mTimeSinceUpdate;

    private boolean mSpinning = false;
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

        mSpinButton = new Debouncer(properties.debounce);
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
        // Each press counts once, however long it is held.
        if (Pressed(mSpinButton, mGamepad.right_bumper)) {
            SetSpinning(!mSpinning);
        }
        // With range tuning on, up / down nudge the whole distance table; the table then sets
        // the target. Off, they set the target itself.
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
        ShowStatus();
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
        double tableRpm = RpmForRange(rangeIn, mProperties.launchRpmByRangeIn);
        mProperties.pollenLauncherRpm = KeepInRange(tableRpm + mProperties.launcherRangeTrimRpm);
        mRangeStatus = String.format(Locale.US, "%.0f in -> %.0f RPM, trim %+.0f = %.0f",
                rangeIn, tableRpm, mProperties.launcherRangeTrimRpm, mProperties.pollenLauncherRpm);
    }

    /**
     * The RPM for a distance, from a table of {inches, RPM} rows in order of distance. The
     * row for the band the robot is in: the last row whose distance is not more than the
     * range. Nearer than the first row: the first row. Farther than the last: the last.
     *
     * <pre>
     *     table {110, 2900}, {120, 3030}, {130, 3170}, {140, 3300}
     *     100 in: 2900     115 in: 2900     120 in: 3030     139 in: 3170     200 in: 3300
     * </pre>
     */
    public static double RpmForRange(double rangeIn, double[][] table) {
        double rpm = table[0][1];
        for (double[] row : table) {
            if (rangeIn >= row[0]) {
                rpm = row[1];
            }
        }
        return rpm;
    }

    /** Measure both speeds and set both powers. Process() calls this every 50 ms. */
    void Update(double secondsPassed) {
        mPollen.Update(mProperties.pollenLauncherRpm, secondsPassed);
        mNectar.Update(mProperties.nectarLauncherRpm, secondsPassed);
    }

    /** true: both wheels spin at their targets. false: both off. An auto calls this too. */
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
        mOp.mTelemetry.addLine("Launchers (gamepad 2): right bumper off / on, D-pad up/down pollen, right/left nectar");
        mOp.mTelemetry.addData("Launcher speed from distance", mRangeStatus);
        mPollen.ShowStatus();
        mNectar.ShowStatus();
    }
}
