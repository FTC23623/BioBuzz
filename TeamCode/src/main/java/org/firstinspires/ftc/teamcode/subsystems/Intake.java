package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
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
 * still swinging open, the transfer STOPS, even if cross had it running, so no ball is
 * pushed against a half-open gate (docs/decisions/0034). The intake keeps doing what it
 * was. Once the gates are fully open, the intake AND the transfer run forward even if they
 * were off, each at a share of its speed ({@code launchFeedIntakeScale} half,
 * {@code launchFeedTransferScale} 30%), so balls go into the launcher one at a time: the
 * transfer pushes the balls into the launchers, the intake pushes along any ball stuck
 * lower down. Launching wins over the bumper: the feed speed is
 * about the launcher, not the operator (docs/decisions/0037). Square still wins over both:
 * reverse is for jams.
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

    /**
     * @param launchGates the launch gates, so the transfer can feed the launchers while they
     *                    are open. null when there are none.
     */
    public Intake(HydraOpMode op, BotProperties properties, Gamepad gamepad, LaunchGates launchGates) {
        mOp = op;
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
        mOnOffButton.In(mGamepad.left_bumper);
        if (mOnOffButton.Out()) {
            mOnOffButton.Used();
            mOn = !mOn;
        }
        // Held triggers need no debounce: on the moment it is pulled, off when let go.
        mTriggerHeld = mGamepad.left_trigger > mProperties.trgBtnThresh;
        // Reverse only while held: no debounce, so it stops the moment it is let go.
        mReverseHeld = mGamepad.square;
    }

    @Override
    public void Process() {
        if (mReverseHeld) {
            mIntake.SetRpm(-mProperties.intakeReverseRpm);
            mTransfer.SetRpm(-mProperties.transferReverseRpm);
        } else if (IsGateOpening()) {
            // The driver just pulled the trigger and the gate is still swinging open. Stop
            // the transfer so it is not pushing a ball against the gate. The intake carries
            // on as it was. Once the gate is fully open, the next branch feeds the launch.
            if (IsRunning()) {
                mIntake.SetRpm(mProperties.intakeRpm);
            } else {
                mIntake.SetRpm(0);
            }
            mTransfer.SetRpm(0);
        } else if (IsLaunching()) {
            // Feeding the launch: slower, so balls go into the launcher one at a time at a
            // steady rate. The transfer feeds, the intake pushes up any ball stuck below it.
            mIntake.SetRpm(mProperties.intakeRpm * mProperties.launchFeedIntakeScale);
            mTransfer.SetRpm(mProperties.transferRpm * mProperties.launchFeedTransferScale);
        } else if (IsRunning()) {
            // Intaking on purpose (cross or the left trigger): full speed.
            mIntake.SetRpm(mProperties.intakeRpm);
            mTransfer.SetRpm(mProperties.transferRpm);
        } else {
            mIntake.SetRpm(0);
            mTransfer.SetRpm(0);
        }
        ShowStatus();
    }

    /** true when the left bumper has them on, or the left trigger is held. */
    public boolean IsRunning() {
        return mOn || mTriggerHeld;
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
        } else if (IsRunning()) {
            state = "ON, full speed";
        }
        mOp.mTelemetry.addData("Intake + transfer (gamepad 2: left bumper on/off, hold left trigger, hold square reverse)", state);
        mIntake.ShowStatus();
        mTransfer.ShowStatus();
    }
}
