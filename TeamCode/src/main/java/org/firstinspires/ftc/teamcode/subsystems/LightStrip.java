package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.drivers.PrismLedDriver;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * The goBILDA Prism RGB LED driver and the LED strip plugged into it, over I2C.
 *
 * <p>The Prism does the animating. We send it short messages: "layer 0, animation 1 (solid),
 * color red". It keeps showing that until told otherwise, so nothing is sent every loop.
 * Everything here comes from goBILDA's user guide for SKU 3118-2855-0001 (Nov 2025): each
 * of the Prism's ten layers has sub-registers (0x00 which animation, 0x01 brightness, 0x02
 * start pixel, 0x03 stop pixel, 0x04 color). {@link PrismLedDriver} moves the bytes; it is
 * what the robot configuration calls "goBILDA Prism LED Driver".
 *
 * <p><b>At INIT</b> (docs/decisions/0027): first every layer is cleared, so nothing left over
 * from the last OpMode stays lit. Then the strip is drawn fresh.
 *
 * <p><b>Alliance color</b> (docs/decisions/0021 and 0026): the underglow LEDs
 * ({@code underglowFirstLed} to {@code underglowLastLed}, 12 to 35) show our alliance, red
 * or blue, from INIT to the end of the match. Nothing about them changes at START. The
 * alliance is on layer 1.
 *
 * <p><b>Pollen</b> (docs/decisions/0026, 0033, 0034, 0035): the moment pollen is detected,
 * EVERY layer is cleared and the whole strip ({@code pollenLedsFirst} to
 * {@code pollenLedsLast}) turns yellow on layer 2. Clearing first means nothing else shows,
 * whichever order the Prism draws its layers in. "Detected" means the switch was pressed
 * within the last {@code pollenHoldMs} (half a second), so the holes in a ball don't make it
 * flicker. Half a second after the last ball leaves, every layer is cleared again and the
 * alliance underglow is drawn back. A D-pad color is lost at that point; press it again.
 *
 * <p>Controls, for {@code Test: Light Strip} and HyDrive (gamepad 1's D-pad is free). They
 * light the LEDs before the underglow:
 * <ul>
 *   <li><b>D-pad up</b> red, <b>right</b> green, <b>down</b> blue, all solid.
 *   <li><b>D-pad left</b>: CLEAR. Every layer is cleared, whatever the code thinks is
 *       showing. The underglow, and the pollen yellow if a ball is in, come straight back on
 *       the next loop (docs/decisions/0036).
 * </ul>
 *
 * <p>Two things the guide does not pin down, marked CHECK below: which number each
 * animation after Solid Color has, and the byte order of 4-byte values like a blink period.
 * The first test on the strip settles both. See docs/decisions/0017.
 */
public class LightStrip implements Subsystem {
    /** The one layer used so far. The Prism has ten, 0 to 9, drawn on top of each other. */
    static final int LAYER = 0;
    /** The alliance color's layer. */
    static final int ALLIANCE_LAYER = 1;
    /** The pollen LEDs' layer: yellow over the whole strip while the pollen sensor is pressed. */
    static final int POLLEN_LAYER = 2;

    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Gamepad mGamepad;
    private final PollenSensor mPollenSensor;
    /** null when no strip is in the robot configuration. */
    private final PrismLedDriver mStrip;
    private final Debouncer mRedButton;
    private final Debouncer mGreenButton;
    private final Debouncer mBlueButton;
    private final Debouncer mOffButton;

    private boolean mStarted = false;
    private int mDeviceId = -1;
    private byte[] mStatus = new byte[0];
    private String mShowing = "nothing sent yet";
    /** The alliance the strip shows now, so it is only told when that changes. null = none yet. */
    private Alliance mAllianceShown = null;
    /** true while the pollen LEDs are yellow, so the strip is only told when that changes. */
    private boolean mPollenShown = false;

    public LightStrip(HydraOpMode op, BotProperties properties, Gamepad gamepad,
                      PollenSensor pollenSensor) {
        mOp = op;
        mProperties = properties;
        mGamepad = gamepad;
        mPollenSensor = pollenSensor;
        mStrip = op.mHardwareMap.tryGet(PrismLedDriver.class, properties.cfgStrip);
        mRedButton = new Debouncer(properties.debounce);
        mGreenButton = new Debouncer(properties.debounce);
        mBlueButton = new Debouncer(properties.debounce);
        mOffButton = new Debouncer(properties.debounce);
    }

    /** Read who is on the other end, once, so the Driver Station can show it. */
    @Override
    public boolean Init() {
        if (mStrip == null) {
            mOp.mTelemetry.addLine("No light strip: nothing named '" + mProperties.cfgStrip
                    + "' in the robot configuration (a goBILDA Prism LED Driver).");
            return true;
        }
        if (!mStarted) {
            mStarted = true;
            mDeviceId = mStrip.ReadDeviceId();
            mStatus = mStrip.ReadStatus();
            ClearEverything();
        }
        ShowStatus();
        return true;
    }

    /** D-pad: up red, right green, down blue, left off. Each press is one message. */
    @Override
    public void HandleUserInput() {
        mRedButton.In(mGamepad.dpad_up);
        if (mRedButton.Out()) {
            mRedButton.Used();
            Solid(255, 0, 0);
        }
        mGreenButton.In(mGamepad.dpad_right);
        if (mGreenButton.Out()) {
            mGreenButton.Used();
            Solid(0, 255, 0);
        }
        mBlueButton.In(mGamepad.dpad_down);
        if (mBlueButton.Out()) {
            mBlueButton.Used();
            Solid(0, 0, 255);
        }
        mOffButton.In(mGamepad.dpad_left);
        if (mOffButton.Out()) {
            mOffButton.Used();
            Off();
        }
    }

    /** Same as before START: keep the underglow and the pollen LEDs up to date. */
    @Override
    public void Process() {
        ShowStatus();
    }

    /**
     * Every layer cleared: the whole strip dark. The Prism keeps showing what the last OpMode
     * sent, so INIT starts with this. The alliance and pollen LEDs are then sent fresh.
     */
    void ClearEverything() {
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            mStrip.ClearLayer(layer);
        }
        mAllianceShown = null;
        mPollenShown = false;
        mShowing = "cleared";
    }

    /** One color on LEDs first to last (both lit, counted from 0), on one layer. */
    private void Run(int layer, int first, int last, int red, int green, int blue) {
        mStrip.SolidColor(layer, first, PrismStop(last), mProperties.stripBrightness, red, green, blue);
    }

    /** The "stop" number the Prism wants for a run that ends on this LED. */
    int PrismStop(int lastLed) {
        if (mProperties.prismStopIsOnePastLast) {
            return lastLed + 1;
        }
        return lastLed;
    }

    /** The LEDs before the underglow, one color. Red, green and blue are 0 to 255. */
    public void Solid(int red, int green, int blue) {
        if (mStrip != null) {
            Run(LAYER, 0, mProperties.underglowFirstLed - 1, red, green, blue);
        }
        mShowing = "solid " + red + "," + green + "," + blue;
    }

    /** Our alliance's color on the underglow LEDs, on the alliance layer. */
    private void ShowAlliance() {
        mAllianceShown = mOp.mAlliance;
        if (mStrip == null) {
            return;
        }
        int first = mProperties.underglowFirstLed;
        int last = mProperties.underglowLastLed;
        if (mAllianceShown == Alliance.RED) {
            Run(ALLIANCE_LAYER, first, last, 255, 0, 0);
            mShowing = "underglow RED on LEDs " + first + " to " + last;
        } else if (mAllianceShown == Alliance.BLUE) {
            Run(ALLIANCE_LAYER, first, last, 0, 0, 255);
            mShowing = "underglow BLUE on LEDs " + first + " to " + last;
        } else {
            mStrip.ClearLayer(ALLIANCE_LAYER);
            mShowing = "no underglow: alliance UNKNOWN";
        }
    }

    /**
     * Pollen detected: every layer cleared, then the pollen LEDs yellow. Pollen gone: every
     * layer cleared again; ShowStatus() then draws the underglow back.
     */
    private void ShowPollen(boolean detected) {
        if (mStrip != null) {
            ClearEverything();
            if (detected) {
                Run(POLLEN_LAYER, mProperties.pollenLedsFirst, mProperties.pollenLedsLast, 255, 200, 0);
                mShowing = "POLLEN: whole strip yellow";
            } else {
                mShowing = "pollen gone: every layer cleared, underglow back next loop";
            }
        }
        mPollenShown = detected;
    }

    /**
     * The whole strip blinking between two colors. CHECK: ANIMATION_BLINK's number and the
     * byte order of the period are the guide's best reading, not yet seen on a strip.
     */
    public void Blink(int red, int green, int blue, int red2, int green2, int blue2, long periodMs) {
        WriteLayer(PrismLedDriver.SUB_ANIMATION, new byte[] {(byte) PrismLedDriver.ANIMATION_BLINK});
        WriteLayer(PrismLedDriver.SUB_BRIGHTNESS, new byte[] {(byte) mProperties.stripBrightness});
        WriteLayer(PrismLedDriver.SUB_START, new byte[] {0});
        WriteLayer(PrismLedDriver.SUB_STOP, new byte[] {(byte) PrismStop(mProperties.underglowFirstLed - 1)});
        WriteLayer(PrismLedDriver.SUB_PRIMARY_COLOR, new byte[] {(byte) red, (byte) green, (byte) blue});
        WriteLayer(PrismLedDriver.SUB_SECONDARY_COLOR, new byte[] {(byte) red2, (byte) green2, (byte) blue2});
        WriteLayer(PrismLedDriver.SUB_PERIOD, Bytes32(periodMs));
        WriteLayer(PrismLedDriver.SUB_PRIMARY_PERIOD, Bytes32(periodMs / 2));
        mShowing = "blink " + red + "," + green + "," + blue + " and " + red2 + "," + green2 + "," + blue2
                + " every " + periodMs + " ms";
    }

    /**
     * Every layer cleared, whatever the code believes is showing. The next ShowStatus() draws
     * the underglow back, and the pollen yellow if a ball is in. D-pad left.
     */
    public void Off() {
        if (mStrip != null) {
            ClearEverything();
        }
        mShowing = "CLEARED by D-pad left; redrawing what should be there";
    }

    /** One setting to our layer, if there is a strip. */
    private void WriteLayer(int subRegister, byte[] data) {
        if (mStrip == null) {
            return;
        }
        mStrip.WriteLayer(LAYER, subRegister, data);
    }

    /**
     * A 4-byte value, lowest byte first (little-endian), which is how most small chips store
     * numbers. CHECK on the strip: if a 2000 ms blink comes out wildly wrong, flip this order.
     */
    static byte[] Bytes32(long value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (value & 0xFF);
        bytes[1] = (byte) ((value >> 8) & 0xFF);
        bytes[2] = (byte) ((value >> 16) & 0xFF);
        bytes[3] = (byte) ((value >> 24) & 0xFF);
        return bytes;
    }

    /** A byte as 0 to 255 instead of -128 to 127. */
    static int Unsigned(byte b) {
        return b & 0xFF;
    }

    public void ShowStatus() {
        // Each is sent only when it changes. Each message to the strip takes loop time.
        boolean pollen = mPollenSensor != null && mPollenSensor.IsDetected();
        if (pollen != mPollenShown) {
            ShowPollen(pollen);
        }
        // The underglow is drawn only while no pollen is shown: pollen owns the whole strip.
        // After pollen clears every layer, mAllianceShown is null, so it is drawn back here.
        if (!mPollenShown && mOp.mAlliance != mAllianceShown) {
            ShowAlliance();
        }
        if (mStrip == null) {
            return;
        }
        mOp.mTelemetry.addData("Light strip", "showing " + mShowing);
        mOp.mTelemetry.addData("Strip device id", "0x%02X (register 0x00)", mDeviceId);
        if (mStatus.length == 4) {
            // Guide: bits 0 to 7 of the status register are the number of LEDs, 8 to 23 the FPS.
            // Lowest byte first means byte 0 is the LED count.
            mOp.mTelemetry.addData("Strip status bytes", "%02X %02X %02X %02X (LEDs %d if lowest byte first)",
                    Unsigned(mStatus[0]), Unsigned(mStatus[1]), Unsigned(mStatus[2]), Unsigned(mStatus[3]),
                    Unsigned(mStatus[0]));
        }
        mOp.mTelemetry.addLine("D-pad: up red, right green, down blue, left off");
    }
}
