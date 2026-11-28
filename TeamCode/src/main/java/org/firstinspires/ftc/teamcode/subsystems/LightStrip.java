package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.util.ElapsedTime;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;
import org.firstinspires.ftc.teamcode.drivers.PrismLedDriver;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.StripSnake;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * The goBILDA Prism RGB LED driver and the LED strip plugged into it, over I2C.
 *
 * <p>The Prism does the animating. We send it short messages: "layer 1, animation 1 (solid),
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
 * ({@code underglowFirstLed} to {@code underglowLastLed}, 0 to 35: the whole strip) show our alliance, red
 * or blue, from INIT to the end of the match. Nothing about them changes at START. The
 * alliance is on layer 1.
 *
 * <p><b>Pollen</b> (docs/decisions/0026, 0033, 0034, 0035): only after START. During INIT
 * the strip always shows the alliance, even with pollen loaded, so the drive team can check
 * the color before the auto starts. After START, the moment pollen is detected,
 * EVERY layer is cleared and the whole strip ({@code pollenLedsFirst} to
 * {@code pollenLedsLast}) turns yellow on layer 2. Clearing first means nothing else shows,
 * whichever order the Prism draws its layers in. "Detected" means the switch was pressed
 * within the last {@code pollenHoldMs} (half a second), so the holes in a ball don't make it
 * flicker. Half a second after the last ball leaves, every layer is cleared again and the
 * alliance underglow is drawn back.
 *
 * <p><b>Snake during INIT</b> ({@code stripInitSnake}): instead of the plain underglow, an
 * alliance-colored snake slides along the underglow LEDs by itself and grows each time it
 * eats a yellow pollen LED ({@link StripSnake}). It is still the alliance color, so the
 * drive team can check it. The moment START is pressed, every layer is cleared and the
 * snake is gone for good: the normal underglow and pollen take over.
 *
 * <p><b>No gamepad.</b> The strip reads nothing from the drivers: the underglow and the
 * pollen yellow run by themselves. Gamepad 1's D-pad used to paint demo colors here; since
 * docs/decisions/0039 the drive uses it to arc around the hive. {@code Test: Light Strip}
 * still talks to the Prism driver directly for a bench check.
 *
 * <p>Two things the guide does not pin down, marked CHECK below: which number each
 * animation after Solid Color has, and the byte order of 4-byte values like a blink period.
 * The first test on the strip settles both. See docs/decisions/0017.
 */
public class LightStrip implements Subsystem {
    /** A spare layer, used only by {@link #Blink}. The Prism has ten, 0 to 9, drawn on top of each other. */
    static final int LAYER = 0;
    /** The alliance color's layer. */
    static final int ALLIANCE_LAYER = 1;
    /** The pollen LEDs' layer: yellow over the whole strip while the pollen sensor is pressed. */
    static final int POLLEN_LAYER = 2;
    /** INIT snake: its body, or the part from the tail to the end of the strip when it wraps. */
    static final int SNAKE_LAYER = 3;
    /** INIT snake: the part from the start of the strip to the head, only while it wraps. */
    static final int SNAKE_WRAP_LAYER = 4;
    /** INIT snake: the one yellow pollen LED it is chasing. */
    static final int SNAKE_POLLEN_LAYER = 5;

    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final PollenSensor mPollenSensor;
    /** null when no strip is in the robot configuration. */
    private final PrismLedDriver mStrip;

    private boolean mStarted = false;
    private int mDeviceId = -1;
    private byte[] mStatus = new byte[0];
    private String mShowing = "nothing sent yet";
    /** The alliance the strip shows now, so it is only told when that changes. null = none yet. */
    private Alliance mAllianceShown = null;
    /** true while the pollen LEDs are yellow, so the strip is only told when that changes. */
    private boolean mPollenShown = false;
    /**
     * false during INIT: pollen is ignored, so the strip shows the alliance color for the
     * drive team to check. true from the first loop after START.
     */
    private boolean mMatchStarted = false;
    /**
     * What each layer is lit with now (color and brightness packed in one number), or -1 for
     * cleared. A run in the same color only moves its LEDs, so nothing blinks.
     */
    private final int[] mLayerLit = NewLayerLit();
    /** The first LED each lit layer's run starts on, so a move can shrink before it grows. */
    private final int[] mLayerFirst = new int[PrismLedDriver.LAYERS];
    /** The INIT snake. null when it isn't playing. */
    private StripSnake mSnake = null;
    private final ElapsedTime mSnakeTimer = new ElapsedTime();
    /** true while the snake's second run (SNAKE_WRAP_LAYER) is lit, so it is cleared once. */
    private boolean mSnakeWrapDrawn = false;
    /** The LED the pollen was last drawn on, so it is sent only when it moves. -1 = none. */
    private int mSnakePollenDrawn = -1;

    public LightStrip(HydraOpMode op, BotProperties properties, PollenSensor pollenSensor) {
        mOp = op;
        mProperties = properties;
        mPollenSensor = pollenSensor;
        mStrip = op.mHardwareMap.tryGet(PrismLedDriver.class, properties.cfgStrip);
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

    /** Nothing: the strip has no buttons. */
    @Override
    public void HandleUserInput() {
    }

    /** After START: keep the underglow and the pollen LEDs up to date. Pollen counts from here on. */
    @Override
    public void Process() {
        mMatchStarted = true;
        ShowStatus();
    }

    /**
     * Every layer cleared: the whole strip dark. The Prism keeps showing what the last OpMode
     * sent, so INIT starts with this, and pollen coming or going does it too. The alliance
     * and pollen LEDs are then sent fresh.
     */
    void ClearEverything() {
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            ClearLayer(layer);
        }
        mAllianceShown = null;
        mPollenShown = false;
        mShowing = "cleared";
    }

    /**
     * One color on LEDs first to last (both lit, counted from 0), on one layer. If the layer
     * already shows this color, only its LEDs move: no new color, so no flash.
     */
    private void Run(int layer, int first, int last, int red, int green, int blue) {
        int lit = (mProperties.stripBrightness << 24) | (red << 16) | (green << 8) | blue;
        if (mLayerLit[layer] == lit) {
            // Moving: send the end that shrinks the run first. The Prism draws between the two
            // messages, so the other order stretched it for a moment across every LED between
            // the old spot and the new one (a long yellow streak when the snake's pollen jumped).
            boolean startFirst = first > mLayerFirst[layer];
            mStrip.MoveRun(layer, first, PrismStop(last), startFirst);
            mLayerFirst[layer] = first;
            return;
        }
        mStrip.SolidColor(layer, first, PrismStop(last), mProperties.stripBrightness, red, green, blue);
        mLayerLit[layer] = lit;
        mLayerFirst[layer] = first;
    }

    /** A layer dark, and remembered as dark, so the next Run sends its color fresh. */
    private void ClearLayer(int layer) {
        mStrip.ClearLayer(layer);
        mLayerLit[layer] = -1;
    }

    /** Every layer remembered as cleared. */
    private static int[] NewLayerLit() {
        int[] lit = new int[PrismLedDriver.LAYERS];
        Arrays.fill(lit, -1);
        return lit;
    }

    /** The "stop" number the Prism wants for a run that ends on this LED. */
    int PrismStop(int lastLed) {
        if (mProperties.prismStopIsOnePastLast) {
            return lastLed + 1;
        }
        return lastLed;
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
            ClearLayer(ALLIANCE_LAYER);
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
     * The LEDs before the underglow blinking between two colors. Nothing calls this yet.
     * CHECK: ANIMATION_BLINK's number and the byte order of the period are the guide's best
     * reading, not yet seen on a strip.
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

    /** One setting to the spare layer, if there is a strip. */
    private void WriteLayer(int subRegister, byte[] data) {
        if (mStrip == null) {
            return;
        }
        mStrip.WriteLayer(LAYER, subRegister, data);
        // Not a plain run any more: the next Run on this layer sends everything fresh.
        mLayerLit[LAYER] = -1;
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

    /** true while the INIT snake should play: before START, switched on, and a strip to play on. */
    private boolean SnakeTime() {
        return !mMatchStarted && mProperties.stripInitSnake && mStrip != null;
    }

    /** One loop of the INIT snake: start it, or step it when its time is up. */
    private void SnakeLoop() {
        if (mSnake == null) {
            int leds = mProperties.underglowLastLed - mProperties.underglowFirstLed + 1;
            mSnake = new StripSnake(leds, mProperties.snakeStartLength, new Random());
            mSnakeTimer.reset();
            DrawSnake();
            return;
        }
        if (mSnakeTimer.seconds() >= mProperties.snakeStepSeconds) {
            mSnakeTimer.reset();
            mSnake.Step();
            DrawSnake();
        }
    }

    /**
     * The snake in the alliance color (white if not known yet), one run or two when it wraps,
     * and its pollen LED in yellow. The pollen is sent only when it moves.
     */
    private void DrawSnake() {
        int first = mProperties.underglowFirstLed;
        int red = 255;
        int green = 255;
        int blue = 255;
        if (mOp.mAlliance == Alliance.RED) {
            green = 0;
            blue = 0;
        } else if (mOp.mAlliance == Alliance.BLUE) {
            red = 0;
            green = 0;
        }
        int head = first + mSnake.Head();
        int tail = first + mSnake.Tail();
        if (mSnake.Wraps()) {
            Run(SNAKE_LAYER, tail, first + mSnake.Leds() - 1, red, green, blue);
            Run(SNAKE_WRAP_LAYER, first, head, red, green, blue);
            mSnakeWrapDrawn = true;
        } else {
            Run(SNAKE_LAYER, tail, head, red, green, blue);
            if (mSnakeWrapDrawn) {
                ClearLayer(SNAKE_WRAP_LAYER);
                mSnakeWrapDrawn = false;
            }
        }
        int pollen = first + mSnake.Food();
        if (pollen != mSnakePollenDrawn) {
            Run(SNAKE_POLLEN_LAYER, pollen, pollen, 255, 200, 0);
            mSnakePollenDrawn = pollen;
        }
        mShowing = String.format(Locale.US, "INIT snake: %d long, pollen on LED %d (stops at START)",
                mSnake.Length(), pollen);
    }

    /** START: the snake is gone for good. Every layer cleared; the underglow is drawn fresh. */
    private void StopSnake() {
        mSnake = null;
        mSnakeWrapDrawn = false;
        mSnakePollenDrawn = -1;
        ClearEverything();
    }

    public void ShowStatus() {
        if (SnakeTime()) {
            SnakeLoop();
            ShowTelemetry(false);
            return;
        }
        if (mSnake != null) {
            StopSnake();
        }
        // Each is sent only when it changes. Each message to the strip takes loop time.
        boolean pollenSensed = mPollenSensor != null && mPollenSensor.IsDetected();
        // During INIT the alliance color wins, so the drive team sees the right color.
        boolean pollen = mMatchStarted && pollenSensed;
        if (pollen != mPollenShown) {
            ShowPollen(pollen);
        }
        // The underglow is drawn only while no pollen is shown: pollen owns the whole strip.
        // After pollen clears every layer, mAllianceShown is null, so it is drawn back here.
        if (!mPollenShown && mOp.mAlliance != mAllianceShown) {
            ShowAlliance();
        }
        ShowTelemetry(pollenSensed);
    }

    /** What the strip is showing, and what it said about itself at INIT. */
    private void ShowTelemetry(boolean pollenSensed) {
        if (mStrip == null || !mOp.Shows(TelemetryGroup.LIGHTS)) {
            return;
        }
        mOp.mTelemetry.addData("Light strip", "showing " + mShowing);
        if (!mMatchStarted && pollenSensed) {
            mOp.mTelemetry.addLine("Pollen sensed: the strip turns yellow after START");
        }
        mOp.mTelemetry.addData("Strip device id", "0x%02X (register 0x00)", mDeviceId);
        if (mStatus.length == 4) {
            // Guide: bits 0 to 7 of the status register are the number of LEDs, 8 to 23 the FPS.
            // Lowest byte first means byte 0 is the LED count.
            mOp.mTelemetry.addData("Strip status bytes", "%02X %02X %02X %02X (LEDs %d if lowest byte first)",
                    Unsigned(mStatus[0]), Unsigned(mStatus[1]), Unsigned(mStatus[2]), Unsigned(mStatus[3]),
                    Unsigned(mStatus[0]));
        }
    }
}
