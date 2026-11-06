package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The intake (picks balls up off the floor) and the transfer (carries them up to the
 * launchers). They run together.
 *
 * <p>Controls (operator, gamepad 2):
 * <ul>
 *   <li><b>Left bumper</b>: intake and transfer on. Press again: off. It sits above the
 *       left trigger, which runs the same rollers while held.
 *   <li><b>Left trigger (hold)</b>: intake and transfer on while held, the same as the
 *       bumper. Let go: back to what the bumper says.
 *   <li><b>Square / X (hold)</b>: both run BACKWARD, slower than forward, to spit balls out
 *       one at a time when the robot has picked up too many. Let go and they go back to
 *       what they were doing.
 * </ul>
 *
 * <p><b>Launching</b> (driver's right trigger, {@link LaunchGates}): while the gates are
 * still swinging open, the transfer STOPS, even if it was running, so no ball is pushed
 * against a half-open gate (docs/decisions/0034). The intake, if it was running, drops to
 * its launch-feed speed the moment the trigger is pulled. Once the gates are fully open, the intake AND the transfer run forward even if they
 * were off, each at a share of its speed ({@code launchFeedIntakeScale} half,
 * {@code launchFeedTransferScale} 30%), so balls go into the launcher one at a time: the
 * transfer pushes the balls into the launchers, the intake pushes along any ball stuck
 * lower down. Launching wins over the bumper: the feed speed is
 * about the launcher, not the operator (docs/decisions/0037). Square still wins over both:
 * reverse is for jams.
 *
 * <p><b>Auto-reject</b> (docs/decisions/0040): the pollen switch only sees a ball once the
 * robot is full. While the operator is intaking, pollen detected for
 * {@code pollenRejectAfterSeconds} (1 s) means one ball too many: the intake runs backward
 * to spit it out while the transfer keeps the rest moving up, for {@code pollenRejectSeconds}
 * (0.75 s). Then the operator's intake and transfer stop, and stay stopped until the left
 * bumper and the left trigger are both let go. Launching and square are never interrupted.
 *
 * <p>Each roller has its own speed in BotProperties: {@code intakeRpm}, {@code transferRpm},
 * and the backward ones {@code intakeReverseRpm}, {@code transferReverseRpm}. They are read
 * every loop, so a change on the dashboard counts right away. See docs/decisions/0023.
 */
public class Intake implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    private final Roller mIntake;
    private final Roller mTransfer;
    private final Debouncer mOnOffButton;
    /** null when the launch gates are switched off in this OpMode. */
    private final LaunchGates mLaunchGates;

    private boolean mOn = false;
    private boolean mTriggerHeld = false;
    private boolean mReverseHeld = false;
    private boolean mBumperHeld = false;

    /** null when there is no pollen sensor: then nothing is ever rejected. */
    private final PollenSensor mPollenSensor;
    /** true while pollen has been detected, without a break, during intaking. */
    private boolean mPollenSeen = false;
    private final ElapsedTime mPollenSince = new ElapsedTime();
    /** true while the intake is spitting an extra ball out. */
    private boolean mRejecting = false;
    private final ElapsedTime mRejectTimer = new ElapsedTime();
    /** true after a reject, until the operator lets go of both intake buttons. */
    private boolean mBlocked = false;
    /**
     * false: the intake roller stays off and only the transfer runs, whatever else says.
     * TeleOp leaves it true. Autos turn it off between FLOWER steps (docs/decisions/0043).
     */
    private boolean mIntakeRollerOn = true;

    /** No pollen sensor: never rejects. Tests and robots without the switch use this. */
    public Intake(HydraOpMode op, BotProperties properties, Gamepad gamepad, LaunchGates launchGates) {
        this(op, properties, gamepad, launchGates, null);
    }

    /**
     * @param launchGates  the launch gates, so the transfer can feed the launchers while they
     *                     are open. null when there are none.
     * @param pollenSensor the pollen switch, to reject an extra ball. null when there is none.
     */
    public Intake(HydraOpMode op, BotProperties properties, Gamepad gamepad, LaunchGates launchGates,
                  PollenSensor pollenSensor) {
        mOp = op;
        mPollenSensor = pollenSensor;
        mProperties = properties;
        mGamepad = gamepad;
        mLaunchGates = launchGates;
        mIntake = new Roller("Intake", op, properties, properties.cfgIntake, properties.intakeReversed);
        mTransfer = new Roller("Transfer", op, properties, properties.cfgTransfer, properties.transferReversed);
        mOnOffButton = new Debouncer(properties.debounce);
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
        mBumperHeld = mGamepad.left_bumper;
        mOnOffButton.In(mBumperHeld);
        if (mOnOffButton.Out()) {
            mOnOffButton.Used();
            // After a reject the bumper does nothing until it has been let go.
            if (!mBlocked) {
                mOn = !mOn;
            }
        }
        // Held triggers need no debounce: on the moment it is pulled, off when let go.
        mTriggerHeld = mGamepad.left_trigger > mProperties.trgBtnThresh;
        // Reverse only while held: no debounce, so it stops the moment it is let go.
        mReverseHeld = mGamepad.square;
    }

    @Override
    public void Process() {
        WatchForAnExtraBall();
        if (mReverseHeld) {
            mIntake.SetRpm(-mProperties.intakeReverseRpm);
//            mTransfer.SetRpm(-mProperties.transferReverseRpm);
        } else if (IsGateOpening()) {
            // The driver just pulled the trigger and the gate is still swinging open. Stop
            // the transfer so it is not pushing a ball against the gate. The intake drops to
            // its launch-feed speed right away, not when the gate is open, so balls are
            // already slowing when the launch starts (coach, 2026-11-04). Once the gate is
            // fully open, the next branch feeds the launch.
            if (IsRunning()) {
                mIntake.SetRpm(IntakeRoller(mProperties.intakeRpm * mProperties.launchFeedIntakeScale));
            } else {
                mIntake.SetRpm(0);
            }
            mTransfer.SetRpm(0);
        } else if (IsLaunching()) {
            // Feeding the launch: slower, so balls go into the launcher one at a time at a
            // steady rate. The transfer feeds, the intake pushes up any ball stuck below it.
            mIntake.SetRpm(IntakeRoller(mProperties.intakeRpm * mProperties.launchFeedIntakeScale));
            mTransfer.SetRpm(mProperties.transferRpm * mProperties.launchFeedTransferScale);
        } else if (mRejecting) {
            // One ball too many: spit it out the bottom, keep the rest moving up.
            mIntake.SetRpm(-mProperties.intakeReverseRpm);
            mTransfer.SetRpm(mProperties.transferRpm);
        } else if (IsRunning()) {
            // Intaking on purpose (the left bumper or trigger, or an auto): full speed.
            mIntake.SetRpm(IntakeRoller(mProperties.intakeRpm));
            mTransfer.SetRpm(mProperties.transferRpm);
        } else {
            mIntake.SetRpm(0);
            mTransfer.SetRpm(0);
        }
        if (!mOp.mQuiet) {
            ShowStatus();
        }
    }

    /**
     * Auto-reject: count how long pollen has been detected while intaking, start a reject
     * past pollenRejectAfterSeconds, end it after pollenRejectSeconds, and lift the block
     * once the operator lets go of both intake buttons.
     */
    private void WatchForAnExtraBall() {
        if (mRejecting && mRejectTimer.seconds() >= mProperties.pollenRejectSeconds) {
            mRejecting = false;
            mOn = false;
            mBlocked = true;
        }
        if (mBlocked && !mBumperHeld && !mTriggerHeld) {
            mBlocked = false;
        }
        // Only while the intake roller itself is picking balls up: with it off (an auto between
        // FLOWER steps), nothing is coming in, and a reject would stop the transfer too.
        boolean intakingOnPurpose = IsRunning() && mIntakeRollerOn && !mReverseHeld && !IsLaunching()
                && !IsGateOpening();
        boolean watching = mProperties.pollenAutoReject && mPollenSensor != null && !mRejecting
                && intakingOnPurpose;
        if (!watching || !mPollenSensor.IsDetected()) {
            mPollenSeen = false;
            return;
        }
        if (!mPollenSeen) {
            mPollenSeen = true;
            mPollenSince.reset();
        }
        if (mPollenSince.seconds() >= mProperties.pollenRejectAfterSeconds) {
            mPollenSeen = false;
            mRejecting = true;
            mRejectTimer.reset();
        }
    }

    /**
     * true when the left bumper has them on, or the left trigger is held, and no reject has
     * stopped them (they stay stopped until both buttons are let go).
     */
    public boolean IsRunning() {
        return (mOn || mTriggerHeld) && !mBlocked;
    }

    /** The intake roller's forward speed, or 0 while the roller is switched off. */
    private double IntakeRoller(double rpm) {
        if (!mIntakeRollerOn) {
            return 0;
        }
        return rpm;
    }

    /**
     * true: the intake roller runs whenever the rollers run (normal, TeleOp). false: only the
     * transfer runs; the intake roller stays off. An auto calls this.
     */
    public void SetIntakeRollerOn(boolean on) {
        mIntakeRollerOn = on;
    }

    /** true while the intake is spitting an extra ball out. */
    public boolean IsRejecting() {
        return mRejecting;
    }

    /** true once the launch gates are all the way open, so balls can go through. */
    private boolean IsLaunching() {
        return mLaunchGates != null && mLaunchGates.IsFullyOpen();
    }

    /** true while the launch gates have been told to open but are still swinging. */
    private boolean IsGateOpening() {
        return mLaunchGates != null && mLaunchGates.IsOpen() && !mLaunchGates.IsFullyOpen();
    }

    /** true: intake and transfer run forward. false: off. An auto calls this. */
    public void SetOn(boolean on) {
        mOn = on;
    }

    public boolean IsOn() {
        return mOn;
    }

    /** true while square is held: both rollers run backward. */
    /** The intake roller, for the test log and test telemetry (0050). */
    public Roller GetIntakeRoller() {
        return mIntake;
    }

    /** The transfer, for the test log and test telemetry (0050). */
    public Roller GetTransferRoller() {
        return mTransfer;
    }

    public boolean IsReversing() {
        return mReverseHeld;
    }

    private void ShowStatus() {
        String state = "off";
        if (mReverseHeld) {
            state = "REVERSE (spitting out)";
        } else if (IsGateOpening()) {
            state = "transfer PAUSED while the gate opens";
        } else if (IsLaunching()) {
            state = String.format("feeding the launch: intake %.0f%%, transfer %.0f%%",
                    mProperties.launchFeedIntakeScale * 100, mProperties.launchFeedTransferScale * 100);
        } else if (mRejecting) {
            state = "FULL: spitting an extra ball out";
        } else if (mBlocked) {
            state = "STOPPED after a reject: let go of left bumper and left trigger";
        } else if (IsRunning() && !mIntakeRollerOn) {
            state = "transfer ON, intake roller off";
        } else if (IsRunning()) {
            state = "ON, full speed";
        }
        mOp.mTelemetry.addData("Intake + transfer (gamepad 2: left bumper on/off, hold left trigger, hold square reverse)", state);
        mIntake.ShowStatus();
        mTransfer.ShowStatus();
    }
}
