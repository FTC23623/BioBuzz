package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.TipTuning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** What Test: Tip Detection says. */
@DisplayName("Test: Tip Detection")
class TipDetectionTestTest {
    private static TipTuning.Stats of(int times, double... readings) {
        TipTuning.Stats stats = new TipTuning.Stats();
        for (int i = 0; i < times; i++) {
            for (double reading : readings) {
                stats.Add(reading);
            }
        }
        return stats;
    }

    @Test
    @DisplayName("too few readings: it says how many more it needs, and suggests nothing")
    void tooFew() {
        String line = TipDetectionTest.Suggest("roll", true, of(1, -34), of(1, 25));
        assertTrue(line.contains("need 10 readings"), line);
        assertTrue(!line.contains("autoTagUpMinDeg"), line);
    }

    @Test
    @DisplayName("UP and DOWN that overlap: it says the angle can't tell them apart")
    void overlap() {
        String line = TipDetectionTest.Suggest("pitch", false, of(10, -10, 10), of(10, 5, 30));
        assertTrue(line.contains("OVERLAP"), line);
    }

    @Test
    @DisplayName("UP and DOWN that stay apart: the numbers to put in BotProperties, with a margin")
    void separate() {
        String line = TipDetectionTest.Suggest("roll", true, of(10, -36, -34, -32), of(10, 23, 25, 27));
        assertTrue(line.contains("autoTipFromRoll = true"), line);
        // Gap 55, so the margin is the most allowed, 10: -36 less 10, -32 plus 10, 23 less 10, 27 plus 10.
        assertTrue(line.contains("autoTagUpMinDeg = -46"), line);
        assertTrue(line.contains("autoTagUpMaxDeg = -22"), line);
        assertTrue(line.contains("autoTagDownMinDeg = 13"), line);
        assertTrue(line.contains("autoTagDownMaxDeg = 37"), line);
    }

    @Test
    @DisplayName("classify: UP, DOWN, or between, by today's windows; no angle when unknown")
    void classify() {
        BotProperties p = new BotProperties();
        assertEquals("UP", TipDetectionTest.Classify(-34, p));
        assertEquals("DOWN", TipDetectionTest.Classify(25, p));
        assertTrue(TipDetectionTest.Classify(-5, p).startsWith("between"));
        assertEquals("no angle", TipDetectionTest.Classify(Double.NaN, p));
    }
}
