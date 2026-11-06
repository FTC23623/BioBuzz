package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The goBILDA RGB indicator light. It shows what the camera sees of OUR alliance's tags:
 * red when it sees none, blinking blue for one, solid blue for two, green for three or
 * more. When the only tags in view are the other alliance's, it BLINKS red: the camera
 * works, but the robot is set to the wrong alliance (check the magnet) or is pointed at the
 * wrong tags.
 *
 * <p>It runs the same way before and after START. During INIT the drive team points the
 * robot at our tags and checks the color. The Driver Station says the same thing in words
 * ("Indicator light: ...") and why, so the color can be checked against it.
 *
 * <p>The light is a servo as far as the hub is concerned: it plugs into a servo port and
 * each color is a servo position. 0 is off, 0.28 red, 0.5 green, 1 white; the other
 * colors are in goBILDA's table and in Decode's Indicator.java.
 *
 * <p>A robot with no light configured still runs: the Driver Station says so and this part
 * does nothing. Ported from Decode subsystems/Indicator.java, keeping only the tag-seen rule.
 */
public class Indicator implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Vision mVision;
    /** null when no light is in the robot configuration. */
    private final Servo mLight;
    /** The position sent last loop, so the light is only told when the color changes. */
    private double mLastPosition = -1;
    /** How many of our tags were in view the last time any were. */
    private int mLastCount = 0;
    private final ElapsedTime mBlinkTimer = new ElapsedTime();
    private boolean mBlinkOn = true;
    /** true while an auto is launching. Then the light blinks yellow and ignores the camera. */
    private boolean mLaunching = false;
    /** true while an auto waits to see a tag. Then the light blinks orange. */
    private boolean mWaitingForTag = false;

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
            mOp.mTelemetry.addLine("Indicator light: red = no tag, BLINKING red = only the other alliance's tags, "
                    + "blinking blue = 1 of ours, blue = 2, green = 3 or more");
        }
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /**
     * Blinking yellow while launching, blinking orange while waiting for a tag. Otherwise:
     * blinking red when the only tags in view are the other alliance's, solid red for no tag,
     * blinking blue for one of ours, solid blue for two, green for three or more. Every loop
     * the Driver Station gets one line saying which, and why.
     */
    @Override
    public void Process() {
        int ours = mVision.CountTags();
        if (ours > 0) {
            mLastCount = ours;
        }
        int theirs = mVision.CountIgnoredTags();
        if (mLaunching) {
            Blink(mProperties.indicatorYellow);
            Say("blinking yellow", "launching");
        } else if (mWaitingForTag) {
            Blink(mProperties.indicatorOrange);
            Say("blinking orange", "waiting for a tag");
        } else if (!mVision.SeesTag() && theirs > 0) {
            // The camera sees tags, but none of them are ours. Most likely the alliance
            // magnet is wrong, or the robot is pointed at the other alliance's tags.
            Blink(mProperties.indicatorRed);
            Say("BLINKING RED", Tags(theirs) + " in view, but the other alliance's. We are "
                    + mOp.mAlliance + ". Check the alliance magnet.");
        } else if (!mVision.SeesTag()) {
            SetPosition(mProperties.indicatorRed);
            mBlinkOn = true;
            mBlinkTimer.reset();
            Say("red", "no tag in view");
        } else if (mLastCount == 1) {
            Blink(mProperties.indicatorBlue);
            Say("blinking blue", "1 of our tags in view");
        } else if (mLastCount == 2) {
            SetPosition(mProperties.indicatorBlue);
            Say("blue", "2 of our tags in view");
        } else {
            SetPosition(mProperties.indicatorGreen);
            Say("green", mLastCount + " of our tags in view");
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

    /**
     * true: blink orange, whatever the camera sees. false: turn the light off and go back to
     * showing tags. An auto calls this at a LAUNCH_WHEN_TAG_SEEN step, until a tag is seen.
     */
    public void SetWaitingForTag(boolean waiting) {
        if (waiting == mWaitingForTag) {
            return;
        }
        mWaitingForTag = waiting;
        mBlinkOn = true;
        mBlinkTimer.reset();
        if (!waiting) {
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

    /**
     * One line on the Driver Station: the color the light shows, and why. With no light in
     * the robot configuration it says so, and what the color would have been.
     */
    private void Say(String color, String why) {
        if (mOp.mQuiet) {
            return;
        }
        if (mLight == null) {
            mOp.mTelemetry.addData("Indicator light", "NOT CONFIGURED: no Servo named '"
                    + mProperties.cfgIndicator + "'. Would be " + color + ": " + why);
        } else {
            mOp.mTelemetry.addData("Indicator light", color + ": " + why);
        }
    }

    /** "1 tag" or "3 tags". */
    private static String Tags(int count) {
        if (count == 1) {
            return "1 tag";
        }
        return count + " tags";
    }
}
