package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;
import org.firstinspires.ftc.teamcode.objects.AimAssist;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The pollen and nectar launch servos. A ball launches when its servo opens.
 *
 * <p>Controls (operator, gamepad 2, {@link LaunchChoice}; left and right swap with
 * {@code operatorSwapsLaunchers}):
 * <ul>
 *   <li><b>Left trigger (hold)</b>: the POLLEN servo opens. Let go: it closes.
 *   <li><b>Right trigger (hold)</b>: the NECTAR servo opens. Let go: it closes.
 *   <li>Both triggers: BOTH servos open together (aiming between the two launchers). With
 *       {@code launchBothTogether} off, pollen wins instead and only the pollen servo opens.
 * </ul>
 *
 * <p>Hold to open, never a toggle: the moment the operator lets go, the gate closes. The
 * closed and open spots come from Test: Launch Servo Tuning (docs/decisions/0020). An
 * auto's LAUNCH ({@link #SetOpen}) opens both.
 *
 * <p><b>Camera lock</b> (docs/decisions/0040, {@code launchNeedsLock}, OFF today: a
 * dashboard setting). On, a trigger only opens its gate while the camera is locked on our
 * hive for that launcher: a tag of ours in view, and the hive's middle within
 * {@code aimDoneDegrees} of that launcher's aim (nectar aims the opposite side of the middle
 * from pollen). The trigger also aims (0036), so the robot turns first and the gate opens
 * once it faces the hive. With NO tag in view the gate stays shut and the operator's
 * gamepad buzzes once. There is no override button any more: turn the lock off on the
 * dashboard if the camera has died. An auto's LAUNCH is not checked here; the auto decides.
 *
 * <p><b>Settle first</b> (docs/decisions/0056, {@code teleopLaunchSettles}, on today): once a
 * trigger passes the camera lock above, that launcher's speed is LOCKED and its gate waits
 * for the wheel to settle on it ({@link Launcher#BeginSettle}, and for nectar
 * {@link Launcher#BeginNectarSettle} with {@code teleopNectarSettles}), at most
 * {@code launchSettleTimeoutSeconds}. Each gate waits for its OWN wheel, so with both
 * triggers one may open a moment before the other. Settled once per pull: the gate then
 * stays open while the trigger is held. Letting go, or losing the lock while still settling,
 * unlocks the speed so the distance moves it again. Nectar settles too because the single
 * transfer presses a ball against the closed nectar gate: it goes the moment the gate opens.
 *
 * <p><b>Out of the way first.</b> A servo can't say when it got there, so the time it needs
 * is worked out from its speed and how far it turns ({@link #OpenSeconds}). Until then
 * {@link #IsFullyOpen()} is false, and the transfer waits before pushing balls in
 * (docs/decisions/0028).
 *
 * <p><b>Every launch is noted</b> (docs/decisions/0036): the loop the gates are told to
 * open, the camera's distance to the nearest tag and the bearing to the hive's middle are
 * remembered and shown as "Last launch (camera)", and the distance is graphed on the
 * dashboard. That is how the launcher speed gets matched to distance.
 *
 * <p>A servo can't report where it is, it goes where it was last told. With
 * {@code launchGatesCloseAtInit} on (today), both are sent to closed once, at INIT, so a
 * gate left open by the last OpMode or by hand is reset before the match. Off: nothing moves
 * during INIT, and both go to closed at START.
 */
public class LaunchGates implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    /** The camera, asked once per launch how far the tag is, and for the lock. null when there is none. */
    private final Vision mVision;
    /** The launcher, to settle on the speed before opening. null until the Bot hands it over, or none. */
    private Launcher mLauncher = null;
    /** null when the servo is not in the robot configuration. */
    private final Servo mPollenServo;
    private final Servo mNectarServo;

    private boolean mOpen = false;
    /** true once INIT has sent both gates to closed, so it is sent only once. */
    private boolean mClosedAtInit = false;
    /** Which gates are open: both for an auto, whichever have settled for a TeleOp pull. */
    private boolean mPollenOpens = true;
    private boolean mNectarOpens = true;
    /** What each gate was last loop, to tell when one has just started to open. */
    private boolean mPrevPollenOpen = false;
    private boolean mPrevNectarOpen = false;
    /** true once the open position has been sent, so the timer starts only once per launch. */
    private boolean mOpenSent = false;
    private final ElapsedTime mSinceOpened = new ElapsedTime();
    /** How many times the gates have been told to open since START. */
    private int mLaunchCount = 0;
    /** What the camera saw at the last launch, for the Driver Station. */
    private String mLastLaunch = "none yet";
    /** Inches to the nearest tag at the last launch, for the dashboard graph. 0 = no tag. */
    private double mLastLaunchRangeIn = 0;

    // Read from the gamepads in HandleUserInput, used in Process. An auto never calls
    // HandleUserInput, so mFromGamepad stays false there and SetOpen() decides alone.
    private boolean mFromGamepad = false;
    private boolean mTriggerHeld = false;
    /** Which launch trigger is held this loop (pollen wins). */
    private LaunchChoice mChoice = LaunchChoice.NONE;
    /** true once this pull has buzzed the driver, so one pull is one buzz. */
    private boolean mBuzzedThisPull = false;
    /** true once this pull has settled each wheel (or timed out), so a pull settles only once. */
    private boolean mPollenSettled = false;
    private boolean mNectarSettled = false;
    /** Why the gates are shut while the trigger is held, for the Driver Station. */
    private String mLockStatus = "";
    /** What each wheel is waiting on, while it settles. */
    private String mPollenWait = "";
    private String mNectarWait = "";

    /**
     * @param gamepad the operator's gamepad: the launch triggers, and the no-lock buzz
     * @param vision  the camera, for the lock and so each launch can note how far the tag
     *                was. null for none: then nothing ever locks.
     */
    public LaunchGates(HydraOpMode op, BotProperties properties, Gamepad gamepad, Vision vision) {
        mOp = op;
        mProperties = properties;
        mGamepad = gamepad;
        mVision = vision;
        mPollenServo = op.mHardwareMap.tryGet(Servo.class, properties.cfgPollenServo);
        mNectarServo = op.mHardwareMap.tryGet(Servo.class, properties.cfgNectarServo);
    }

    @Override
    public boolean Init() {
        // Once: both gates to closed, so INIT resets them (launchGatesCloseAtInit).
        if (!mClosedAtInit && mProperties.launchGatesCloseAtInit) {
            mClosedAtInit = true;
            SetPosition(mPollenServo, mProperties.pollenServoClosed);
            SetPosition(mNectarServo, mProperties.nectarServoClosed);
        }
        if (mPollenServo == null) {
            mOp.mTelemetry.addLine("Pollen launch servo: MISSING. Nothing named '"
                    + mProperties.cfgPollenServo + "' in the robot configuration.");
        }
        if (mNectarServo == null) {
            mOp.mTelemetry.addLine("Nectar launch servo: MISSING. Nothing named '"
                    + mProperties.cfgNectarServo + "' in the robot configuration.");
        }
        return true;
    }

    @Override
    public void HandleUserInput() {
        mFromGamepad = true;
        LaunchChoice choice = LaunchChoice.Launching(mGamepad, mProperties);
        if (choice != mChoice) {
            // A launcher no longer asked for: stop settling it, and settle it again next pull.
            if (mChoice.UsesPollen() && !choice.UsesPollen()) {
                mPollenSettled = false;
                if (mLauncher != null) {
                    mLauncher.CancelSettle();
                }
            }
            if (mChoice.UsesNectar() && !choice.UsesNectar()) {
                mNectarSettled = false;
                if (mLauncher != null) {
                    mLauncher.CancelNectarSettle();
                }
            }
            if (mChoice != LaunchChoice.NONE) {
                // Changed launchers mid-pull: the driver may buzz again.
                mBuzzedThisPull = false;
            }
        }
        mChoice = choice;
        mTriggerHeld = choice != LaunchChoice.NONE;
        if (!mTriggerHeld) {
            // Let go: the next pull may buzz again.
            mBuzzedThisPull = false;
        }
    }

    @Override
    public void Process() {
        if (mFromGamepad) {
            DecideFromTrigger();
        }
        // Start the clock the loop a gate first starts to open (a second gate opening later
        // starts it again, so the transfer waits for that one too), and note what the camera
        // sees right now: this is the distance the launch was made from. One pull is one
        // launch, however many gates it opens.
        boolean pollenNow = IsPollenOpen();
        boolean nectarNow = IsNectarOpen();
        boolean newlyOpen = (pollenNow && !mPrevPollenOpen) || (nectarNow && !mPrevNectarOpen);
        if (newlyOpen) {
            mSinceOpened.reset();
            boolean anyBefore = mPrevPollenOpen || mPrevNectarOpen;
            if (!anyBefore || !(pollenNow && nectarNow)) {
                RememberLaunch();
            }
        }
        mPrevPollenOpen = pollenNow;
        mPrevNectarOpen = nectarNow;
        mOpenSent = mOpen;
        if (IsPollenOpen()) {
            SetPosition(mPollenServo, mProperties.pollenServoOpen);
        } else {
            SetPosition(mPollenServo, mProperties.pollenServoClosed);
        }
        if (IsNectarOpen()) {
            SetPosition(mNectarServo, mProperties.nectarServoOpen);
        } else {
            SetPosition(mNectarServo, mProperties.nectarServoClosed);
        }
        if (!mOp.Shows(TelemetryGroup.LAUNCHER)) {
            return;
        }
        String state = "closed";
        String which = WhichOpen();
        if (IsFullyOpen()) {
            state = which + " OPEN (launching)";
        } else if (mOpen) {
            state = String.format("%s opening, %.2f of %.2f s", which, mSinceOpened.seconds(), OpenSeconds(mProperties));
        } else if (mTriggerHeld) {
            state = mLockStatus;
        }
        if (mProperties.operatorSwapsLaunchers) {
            mOp.mTelemetry.addData("Launch gates (gamepad 2, SWAPPED: hold left trigger nectar, right trigger pollen)", state);
        } else {
            mOp.mTelemetry.addData("Launch gates (gamepad 2: hold left trigger pollen, right trigger nectar)", state);
        }
        mOp.mTelemetry.addData("Last launch (camera)", mLastLaunch);
        mOp.mTelemetry.addData("Last launch range (in)", "%.0f", mLastLaunchRangeIn);
    }

    /**
     * The launcher, so a pull can settle on the speed before the gates open (0056). The Bot
     * calls this once, after building both.
     */
    public void SetLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    /**
     * TeleOp: which gates should be open this loop? The trigger and the camera lock must
     * allow it, and then each launcher's speed must have settled (or timed out) once this
     * pull. Each gate waits only for its own wheel.
     */
    private void DecideFromTrigger() {
        if (!TriggerAllows()) {
            if (mLauncher != null) {
                // Let go, or lost the lock before the launch: unlock the speeds again.
                if (!mPollenSettled) {
                    mLauncher.CancelSettle();
                }
                if (!mNectarSettled) {
                    mLauncher.CancelNectarSettle();
                }
            }
            mPollenOpens = false;
            mNectarOpens = false;
            mOpen = false;
            return;
        }
        mPollenWait = "";
        mNectarWait = "";
        boolean pollenReady = mChoice.UsesPollen() && PollenSettled();
        boolean nectarReady = mChoice.UsesNectar() && NectarSettled();
        mPollenOpens = pollenReady;
        mNectarOpens = nectarReady;
        mOpen = pollenReady || nectarReady;
        if (!mOpen) {
            mLockStatus = (mPollenWait + " " + mNectarWait).trim();
        }
    }

    /**
     * Settle pollen once per pull: lock the speed, wait for it, then let its gate open. With
     * teleopLaunchSettles off, or no launcher, there is nothing to wait for.
     */
    private boolean PollenSettled() {
        if (mPollenSettled || mLauncher == null || !mProperties.teleopLaunchSettles) {
            mPollenSettled = true;
            return true;
        }
        if (!mLauncher.IsSettling()) {
            mLauncher.BeginSettle();
        }
        if (mLauncher.SettleDone()) {
            mPollenSettled = true;
            // Unlock the settle. With launchLocksRpm on, the open gate keeps it locked.
            mLauncher.CancelSettle();
            return true;
        }
        mPollenWait = "pollen LOCKED, " + mLauncher.SettleStatus();
        return false;
    }

    /**
     * The same for nectar (teleopNectarSettles). The single transfer has already pressed a
     * ball against the closed nectar gate, so the wheel must be ready before the gate opens.
     */
    private boolean NectarSettled() {
        if (mNectarSettled || mLauncher == null || !mProperties.teleopLaunchSettles
                || !mProperties.teleopNectarSettles) {
            mNectarSettled = true;
            return true;
        }
        if (!mLauncher.IsNectarSettling()) {
            mLauncher.BeginNectarSettle();
        }
        if (mLauncher.NectarSettleDone()) {
            mNectarSettled = true;
            mLauncher.CancelNectarSettle();
            return true;
        }
        mNectarWait = "nectar LOCKED, " + mLauncher.NectarSettleStatus();
        return false;
    }

    /**
     * A trigger must be held, and the camera locked on our hive for that launcher, unless the
     * lock is switched off.
     */
    private boolean TriggerAllows() {
        if (!mTriggerHeld) {
            return false;
        }
        if (!mProperties.launchNeedsLock) {
            return true;
        }
        if (mVision == null || !mVision.SeesTag()) {
            // The hive is not in view. Shut, and say so through the gamepad.
            mLockStatus = "NO LOCK: no tag of ours in view. Face the hive, or turn launchNeedsLock off";
            if (!mBuzzedThisPull) {
                mBuzzedThisPull = true;
                mGamepad.rumble(mProperties.launchNoLockRumbleMs);
            }
            return false;
        }
        if (!CameraLocked()) {
            // The hive is in view but off to one side. The trigger is turning the robot
            // toward it (launchTriggerAims); the gates open once it is facing the hive.
            mLockStatus = String.format("turning to the hive, %.1f deg off", AimErrorNow());
            return false;
        }
        mLockStatus = "";
        return true;
    }

    /** true when a tag of ours is in view and the hive's middle is within aimDoneDegrees. */
    public boolean CameraLocked() {
        if (mVision == null || !mVision.SeesTag()) {
            return false;
        }
        return Math.abs(AimErrorNow()) <= mProperties.aimDoneDegrees;
    }

    /**
     * Degrees still to turn to the hive for the launcher being pulled, with the offset for the
     * side the robot is on. Nectar aims the opposite side of the middle from pollen.
     */
    private double AimErrorNow() {
        double aimAt = mChoice.AimAtDeg(mVision.GetSideDegrees(), mVision.GetRange(), mProperties);
        return AimAssist.AimError(mVision.GetGroupBearing(), aimAt);
    }

    /** The camera's view the moment the gates were told to open. */
    private void RememberLaunch() {
        mLaunchCount = mLaunchCount + 1;
        if (mVision != null && mVision.SeesTag()) {
            mLastLaunchRangeIn = mVision.GetRange();
            double hiveBearing = mVision.GetGroupBearing();
            String side = "left";
            if (hiveBearing < 0) {
                side = "right";
            }
            mLastLaunch = String.format("#%d: %.0f in from tag %d, hive middle %.1f deg to the %s",
                    mLaunchCount, mLastLaunchRangeIn, mVision.GetTagId(), Math.abs(hiveBearing), side);
        } else {
            mLastLaunchRangeIn = 0;
            mLastLaunch = String.format("#%d: no tag in view", mLaunchCount);
        }
    }

    /** How many times the gates have been told to open since START. */
    public int LaunchCount() {
        return mLaunchCount;
    }

    /** Inches from the camera to the nearest tag at the last launch. 0 when no tag was in view. */
    public double LastLaunchRangeIn() {
        return mLastLaunchRangeIn;
    }

    /**
     * true: open both gates. false: close them. An auto calls this at a LAUNCH step. The
     * camera lock is not checked here: the auto has already waited for its tag.
     */
    public void SetOpen(boolean open) {
        mOpen = open;
        mPollenOpens = true;
        mNectarOpens = true;
    }

    /**
     * Open or close just the gates asked for (an auto's LAUNCH_NECTAR: nectar only). Closing
     * ignores the two flags.
     */
    public void SetOpen(boolean open, boolean pollen, boolean nectar) {
        mOpen = open;
        mPollenOpens = pollen;
        mNectarOpens = nectar;
    }

    /** true while either gate has been told to open. */
    public boolean IsOpen() {
        return mOpen;
    }

    /** true while the pollen gate has been told to open. */
    public boolean IsPollenOpen() {
        return mOpen && mPollenOpens;
    }

    /** true while the nectar gate has been told to open. */
    public boolean IsNectarOpen() {
        return mOpen && mNectarOpens;
    }

    /** "pollen", "nectar" or "both", for the Driver Station. */
    private String WhichOpen() {
        if (mPollenOpens && mNectarOpens) {
            return "both";
        }
        return mPollenOpens ? "pollen" : "nectar";
    }

    /** true once the gates have had time to swing all the way open: balls can go through. */
    public boolean IsFullyOpen() {
        return mOpen && mOpenSent && mSinceOpened.seconds() >= OpenSeconds(mProperties);
    }

    /**
     * How long the slower of the two launch servos takes to open, in seconds:
     *
     * <pre>
     *     degrees it turns = |open - closed| x the servo's range (300)
     *     seconds = degrees / 60 x seconds per 60 degrees + extra
     * </pre>
     *
     * Today: 0.267 x 300 = 80 degrees, 80 / 60 x 0.11 = 0.15 s, plus 0.05 = 0.20 s.
     */
    public static double OpenSeconds(BotProperties properties) {
        double pollenDeg = Math.abs(properties.pollenServoOpen - properties.pollenServoClosed)
                * properties.launchServoRangeDeg;
        double nectarDeg = Math.abs(properties.nectarServoOpen - properties.nectarServoClosed)
                * properties.launchServoRangeDeg;
        double degrees = Math.max(pollenDeg, nectarDeg);
        return degrees / 60 * properties.launchServoSecondsPer60Deg + properties.launchServoExtraSeconds;
    }

    private static void SetPosition(Servo servo, double position) {
        if (servo != null) {
            servo.setPosition(position);
        }
    }
}
