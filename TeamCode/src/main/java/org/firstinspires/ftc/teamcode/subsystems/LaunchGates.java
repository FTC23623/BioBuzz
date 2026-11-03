package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The pollen and nectar launch servos. A ball launches when its servo opens.
 *
 * <p>Controls:
 * <ul>
 *   <li><b>Driver, right trigger (hold)</b>: BOTH servos open. Let go: both close.
 *   <li><b>Operator, right trigger (hold)</b>: override the camera lock below, for a camera
 *       that has died.
 * </ul>
 *
 * <p>Hold to open, never a toggle: the moment the driver lets go, the gates close. Both
 * open together for now; nectar is not part of the first competition (docs/decisions/0023).
 * The closed and open spots come from Test: Launch Servo Tuning (docs/decisions/0020).
 *
 * <p><b>Camera lock</b> (docs/decisions/0040). The trigger only opens the gates while the
 * camera is locked on our hive: a tag of ours in view, and the hive's middle within
 * {@code aimDoneDegrees} of straight ahead. The trigger also aims (0036), so pulling it with
 * the hive off to one side turns the robot first and opens the gates once it is facing the
 * hive. Pulling it with NO tag in view is a driver error: the gates stay shut and the
 * driver's gamepad buzzes once. If the camera has really died, the operator holds their
 * right trigger and the gates open anyway. {@code launchNeedsLock = false} turns the lock
 * off. An auto's LAUNCH ({@link #SetOpen}) is not checked here; the auto decides.
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
 * <p>A servo can't report where it is, it goes where it was last told. So nothing moves
 * during INIT. At START both go to closed.
 */
public class LaunchGates implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    /** The camera, asked once per launch how far the tag is, and for the lock. null when there is none. */
    private final Vision mVision;
    /** null when the servo is not in the robot configuration. */
    private final Servo mPollenServo;
    private final Servo mNectarServo;

    private boolean mOpen = false;
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
    private boolean mOverrideHeld = false;
    /** true once this pull has buzzed the driver, so one pull is one buzz. */
    private boolean mBuzzedThisPull = false;
    /** Why the gates are shut while the trigger is held, for the Driver Station. */
    private String mLockStatus = "";

    /**
     * @param vision the camera, for the lock and so each launch can note how far the tag
     *               was. null for none: then nothing ever locks, and only the override opens.
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
        // No setPosition here: the servos must not move during INIT.
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
        mTriggerHeld = mGamepad.right_trigger > mProperties.trgBtnThresh;
        mOverrideHeld = mOp.mOperatorGamepad.right_trigger > mProperties.trgBtnThresh;
        if (!mTriggerHeld) {
            // Let go: the next pull may buzz again.
            mBuzzedThisPull = false;
        }
    }

    @Override
    public void Process() {
        if (mFromGamepad) {
            mOpen = DecideFromTrigger();
        }
        // Start the clock the loop the open position is first sent, and note what the camera
        // sees right now: this is the distance the launch was made from.
        if (mOpen && !mOpenSent) {
            mSinceOpened.reset();
            RememberLaunch();
        }
        mOpenSent = mOpen;
        if (mOpen) {
            SetPosition(mPollenServo, mProperties.pollenServoOpen);
            SetPosition(mNectarServo, mProperties.nectarServoOpen);
        } else {
            SetPosition(mPollenServo, mProperties.pollenServoClosed);
            SetPosition(mNectarServo, mProperties.nectarServoClosed);
        }
        String state = "closed";
        if (IsFullyOpen()) {
            state = "OPEN (launching)";
        } else if (mOpen) {
            state = String.format("opening, %.2f of %.2f s", mSinceOpened.seconds(), OpenSeconds(mProperties));
        } else if (mTriggerHeld) {
            state = mLockStatus;
        }
        mOp.mTelemetry.addData("Launch gates (gamepad 1: hold right trigger)", state);
        mOp.mTelemetry.addData("Last launch (camera)", mLastLaunch);
        mOp.mTelemetry.addData("Last launch range (in)", "%.0f", mLastLaunchRangeIn);
    }

    /**
     * TeleOp: should the gates be open this loop? The trigger must be held, and the camera
     * locked on our hive, unless the operator overrides or the lock is switched off.
     */
    private boolean DecideFromTrigger() {
        if (!mTriggerHeld) {
            return false;
        }
        if (!mProperties.launchNeedsLock) {
            return true;
        }
        if (mOverrideHeld) {
            mLockStatus = "OVERRIDE: operator's right trigger, camera not checked";
            return true;
        }
        if (mVision == null || !mVision.SeesTag()) {
            // A driver error: the hive is not in view. Shut, and say so through the gamepad.
            mLockStatus = "NO LOCK: no tag of ours in view. Face the hive, or operator holds right trigger";
            if (!mBuzzedThisPull) {
                mBuzzedThisPull = true;
                mGamepad.rumble(mProperties.launchNoLockRumbleMs);
            }
            return false;
        }
        if (!CameraLocked()) {
            // The hive is in view but off to one side. The trigger is turning the robot
            // toward it (launchTriggerAims); the gates open once it is facing the hive.
            mLockStatus = String.format("turning to the hive, %.1f deg off", mVision.GetGroupBearing());
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
        return Math.abs(mVision.GetGroupBearing()) <= mProperties.aimDoneDegrees;
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
    }

    public boolean IsOpen() {
        return mOpen;
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
