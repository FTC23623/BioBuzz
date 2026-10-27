package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The goBILDA RGB indicator light. Red when the camera sees no AprilTag, blinking blue
 * when it sees one, solid blue for two, green for three or more.
 *
 * <p>The light is a servo as far as the hub is concerned: it plugs into a servo port and
 * each color is a servo position. 0 is off, 0.28 red, 0.5 green, 1 white; the other
 * colors are in goBILDA's table and in Decode's Indicator.java.
 *
 * <p>A robot with no light configured still runs: the Driver Station says so once at INIT
 * and this part does nothing. Ported from Decode subsystems/Indicator.java, keeping only
 * the tag-seen rule.
 */
public class Indicator implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Vision mVision;
    /** null when no light is in the robot configuration. */
    private final Servo mLight;
    /** The position sent last loop, so the light is only told when the color changes. */
    private double mLastPosition = -1;
    /** How many tags were in view the last time any were. */
    private int mLastCount = 0;
    private final ElapsedTime mBlinkTimer = new ElapsedTime();
    private boolean mBlinkOn = true;
    /** true while an auto is launching. Then the light blinks yellow and ignores the camera. */
    private boolean mLaunching = false;

    public Indicator(HydraOpMode op, BotProperties properties, Vision vision) {
        mOp = op;
        mProperties = properties;
        mVision = vision;
        mLight = op.mHardwareMap.tryGet(Servo.class, properties.cfgIndicator);
        SetPosition(properties.indicatorOff);
    }

    @Override
    public boolean Init() {
        if (mLight == null) {
            mOp.mTelemetry.addLine("No indicator light: nothing named '" + mProperties.cfgIndicator
                    + "' in the robot configuration (a Servo).");
        } else {
            mOp.mTelemetry.addLine("Indicator light: red = no tag, blinking blue = 1, blue = 2, green = 3 or more");
        }
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /**
     * Blinking yellow while launching. Otherwise red for no tag, blinking blue for one, solid
     * blue for two, green for three or more.
     */
    @Override
    public void Process() {
        if (mVision.CountTags() > 0) {
            mLastCount = mVision.CountTags();
        }
        if (mLaunching) {
            Blink(mProperties.indicatorYellow);
        } else if (!mVision.SeesTag()) {
            SetPosition(mProperties.indicatorRed);
            mBlinkOn = true;
            mBlinkTimer.reset();
        } else if (mLastCount == 1) {
            Blink(mProperties.indicatorBlue);
        } else if (mLastCount == 2) {
            SetPosition(mProperties.indicatorBlue);
        } else {
            SetPosition(mProperties.indicatorGreen);
        }
    }

    /**
     * true: blink yellow, whatever the camera sees. false: turn the light off and go back to
     * showing tags. An auto calls this at a LAUNCH step.
     */
    public void SetLaunching(boolean launching) {
        if (launching == mLaunching) {
            return;
        }
        mLaunching = launching;
        mBlinkOn = true;
        mBlinkTimer.reset();
        if (!launching) {
            SetPosition(mProperties.indicatorOff);
        }
    }

    /** A color and off, taking turns every indicatorBlinkMs. */
    private void Blink(double color) {
        if (mBlinkTimer.milliseconds() >= mProperties.indicatorBlinkMs) {
            mBlinkOn = !mBlinkOn;
            mBlinkTimer.reset();
        }
        if (mBlinkOn) {
            SetPosition(color);
        } else {
            SetPosition(mProperties.indicatorOff);
        }
    }

    /** Send a color to the light, if there is one, and only when it changes. */
    private void SetPosition(double position) {
        if (mLight == null) {
            return;
        }
        if (position != mLastPosition) {
            mLight.setPosition(position);
            mLastPosition = position;
        }
    }
}
