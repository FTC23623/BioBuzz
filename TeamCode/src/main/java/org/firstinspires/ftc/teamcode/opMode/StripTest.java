package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.drivers.PrismLedDriver;

/**
 * The LED strip, on its own, for counting LEDs: a rainbow in blocks of six. Red on LEDs 0-5,
 * orange 6-11, yellow 12-17, green 18-23, blue 24-29, indigo 30-35, violet 36-41, the rest
 * dark. Nothing moves. Count along the strip to find where each LED number is.
 *
 * <p>This OpMode talks straight to the Prism driver, so the whole program is in this one
 * file: change the colors or the block size below.
 *
 * <p>How it works: the Prism has ten layers. Each layer draws one animation on a run of
 * LEDs, from a start LED to a stop LED. Here each color gets its own layer, a Solid Color
 * one block long. LEDs that no layer paints stay dark. The stop number follows
 * {@code prismStopIsOnePastLast}, the same as the robot's own LightStrip.
 *
 * <p>The Driver Station shows what the Prism answered (its device id and status bytes),
 * which is the first thing to check if the strip stays dark.
 */
@TeleOp(name = "Test: Light Strip", group = "Test")
public class StripTest extends LinearOpMode {

    /** How many LEDs each color covers. */
    static final int LEDS_PER_COLOR = 6;

    /** One row per block, from the Prism outward: red, green, blue, each 0 to 255. */
    static final int[][] COLORS = {
            {255, 0, 0},      // red
            {255, 96, 0},     // orange
            {255, 255, 0},    // yellow
            {0, 255, 0},      // green
            {0, 0, 255},      // blue
            {75, 0, 130},     // indigo
            {148, 0, 211},    // violet
    };

    /** The color names, for the Driver Station, in the same order. */
    static final String[] NAMES = {"red", "orange", "yellow", "green", "blue", "indigo", "violet"};

    /** The first LED of a block: block 0 starts at 0, block 1 at 6, and so on. */
    static int FirstLed(int block) {
        return block * LEDS_PER_COLOR;
    }

    /** The last LED of a block: block 0 ends at 5, block 1 at 11, and so on. */
    static int LastLed(int block) {
        return FirstLed(block) + LEDS_PER_COLOR - 1;
    }

    /** The "stop" number the Prism wants for a run that ends on this LED. */
    static int PrismStop(int lastLed, BotProperties properties) {
        if (properties.prismStopIsOnePastLast) {
            return lastLed + 1;
        }
        return lastLed;
    }

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        PrismLedDriver strip = hardwareMap.get(PrismLedDriver.class, properties.cfgStrip);
        int deviceId = strip.ReadDeviceId();
        byte[] status = strip.ReadStatus();

        // One layer per color: layer 0 red on LEDs 0-5, layer 1 orange on 6-11, ...
        for (int block = 0; block < COLORS.length; block++) {
            int[] color = COLORS[block];
            strip.SolidColor(block, FirstLed(block), PrismStop(LastLed(block), properties),
                    properties.stripBrightness, color[0], color[1], color[2]);
        }
        // The layers after the colors stay empty.
        for (int layer = COLORS.length; layer < PrismLedDriver.LAYERS; layer++) {
            strip.ClearLayer(layer);
        }

        while (opModeInInit() || opModeIsActive()) {
            telemetry.addData("Strip device id", "0x%02X (register 0x00)", deviceId);
            telemetry.addData("Strip status bytes", "%02X %02X %02X %02X (LEDs %d if lowest byte first)",
                    status[0] & 0xFF, status[1] & 0xFF, status[2] & 0xFF, status[3] & 0xFF, status[0] & 0xFF);
            for (int block = 0; block < COLORS.length; block++) {
                telemetry.addData("LEDs " + FirstLed(block) + "-" + LastLed(block), NAMES[block]);
            }
            telemetry.addLine("Each color should cover 6 LEDs. 7 there = set prismStopIsOnePastLast false.");
            telemetry.update();
            idle();
        }
    }
}
