package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.drivers.PrismLedDriver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Test: Light Strip paints a rainbow in blocks of six, for counting LEDs. */
@DisplayName("Test: Light Strip blocks")
class StripTestTest {

    @Test
    @DisplayName("ROYGBIV in blocks of six: red 0-5, orange 6-11 ... violet 36-41")
    void blocks() {
        assertEquals(7, StripTest.COLORS.length);
        assertEquals(StripTest.COLORS.length, StripTest.NAMES.length);
        assertEquals(0, StripTest.FirstLed(0));
        assertEquals(5, StripTest.LastLed(0));
        assertEquals(6, StripTest.FirstLed(1));
        assertEquals(11, StripTest.LastLed(1));
        assertEquals(36, StripTest.FirstLed(6));
        assertEquals(41, StripTest.LastLed(6));
        assertEquals("violet", StripTest.NAMES[6]);
    }

    @Test
    @DisplayName("one Prism layer per color fits in the ten layers")
    void fitsTheLayers() {
        assertTrue(StripTest.COLORS.length <= PrismLedDriver.LAYERS);
    }

    @Test
    @DisplayName("the stop number follows prismStopIsOnePastLast, like LightStrip")
    void stopNumber() {
        BotProperties properties = new BotProperties();
        properties.prismStopIsOnePastLast = true;
        assertEquals(6, StripTest.PrismStop(5, properties));
        properties.prismStopIsOnePastLast = false;
        assertEquals(5, StripTest.PrismStop(5, properties));
    }
}
