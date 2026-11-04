package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * A switch that tells when pollen is in place. The LED strip turns yellow while pollen is
 * detected, and the operator's gamepad rumbles once when it arrives (docs/decisions/0026,
 * 0033, 0034, 0035).
 *
 * <p>Two questions it answers. {@link #IsPressed()}: is there pollen on the switch right
 * now? {@link #IsDetected()}: was there within the last {@code pollenHoldMs}? The holes in
 * a ball open and close the switch as the ball turns, so "pressed" flickers and "detected"
 * does not. The strip uses "detected".
 *
 * <p>The switch as wired reads the other way round: closed with NO pollen, open when a ball
 * pushes it. {@code pollenSensorInverted} flips the reading, so "pressed" here always
 * means pollen.
 *
 * <p>Hardware: a switch configured as a "REV Touch Sensor". A robot with no sensor by this
 * name still runs: it reads "no pollen", and the Driver Station says it is missing.
 */
public class PollenSensor implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    /** null when the sensor is not in the robot configuration. */
    private final TouchSensor mSensor;
    /** Time since the switch last read pressed. */
    private final ElapsedTime mSincePressed = new ElapsedTime();
    private boolean mEverPressed = false;
    /** What IsDetected() said last loop, so the rumble happens once, when pollen arrives. */
    private boolean mWasDetected = false;

    public PollenSensor(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
        mSensor = op.mHardwareMap.tryGet(TouchSensor.class, properties.cfgPollenSensor);
    }

    /** true while there is pollen on the switch right now. false with no pollen, or no switch. */
    public boolean IsPressed() {
        if (mSensor == null) {
            return false;
        }
        boolean closed = mSensor.isPressed();
        if (mProperties.pollenSensorInverted) {
            // Wired the other way round: closed means empty.
            return !closed;
        }
        return closed;
    }

    /**
     * true while the switch is pressed, and for {@code pollenHoldMs} after it was last
     * pressed. Call it every loop: that is what keeps the clock right.
     */
    public boolean IsDetected() {
        if (IsPressed()) {
            mEverPressed = true;
            mSincePressed.reset();
            return true;
        }
        return mEverPressed && mSincePressed.milliseconds() < mProperties.pollenHoldMs;
    }

    public void ShowStatus() {
        if (mSensor == null) {
            mOp.mTelemetry.addLine("Pollen sensor: MISSING. Nothing named '"
                    + mProperties.cfgPollenSensor + "' in the robot configuration.");
            return;
        }
        mOp.mTelemetry.addData("Pollen sensor", "pressed %s, detected %s (held %.1f s)",
                IsPressed(), IsDetected(), mProperties.pollenHoldMs / 1000);
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Every loop after START: a rumble on the operator's gamepad the moment pollen arrives. */
    @Override
    public void Process() {
        boolean detected = IsDetected();
        if (detected && !mWasDetected && mProperties.pollenRumbleMs > 0) {
            mOp.mOperatorGamepad.rumble(mProperties.pollenRumbleMs);
        }
        mWasDetected = detected;
        if (!mOp.mQuiet) {
            ShowStatus();
        }
    }
}
