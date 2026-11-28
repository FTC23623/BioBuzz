package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TipTuning")
class TipTuningTest {
    private static final double EPS = 1e-9;

    private static TipTuning.Stats of(double... readings) {
        TipTuning.Stats stats = new TipTuning.Stats();
        for (double reading : readings) {
            stats.Add(reading);
        }
        return stats;
    }

    @Test
    @DisplayName("stats: count, lowest, highest, average; an unknown reading is not counted")
    void stats() {
        TipTuning.Stats stats = of(-40, -28, -34, Double.NaN);
        assertEquals(3, stats.Count());
        assertEquals(-40, stats.Min(), EPS);
        assertEquals(-28, stats.Max(), EPS);
        assertEquals(-34, stats.Mean(), EPS);
        stats.Clear();
        assertEquals(0, stats.Count());
        assertTrue(Double.isNaN(stats.Mean()));
    }

    @Test
    @DisplayName("gap: how far apart UP and DOWN stay, either way round; negative when they overlap; NaN when empty")
    void gap() {
        assertEquals(48, TipTuning.GapDeg(of(-40, -28), of(20, 31)), EPS);
        assertEquals(48, TipTuning.GapDeg(of(20, 31), of(-40, -28)), EPS);
        assertTrue(TipTuning.GapDeg(of(-10, 10), of(5, 30)) < 0);
        assertTrue(Double.isNaN(TipTuning.GapDeg(of(1), new TipTuning.Stats())));
    }

    @Test
    @DisplayName("margin: a third of the gap, never more than the most allowed, never below 0")
    void margin() {
        assertEquals(10, TipTuning.MarginDeg(48, 10), EPS);
        assertEquals(4, TipTuning.MarginDeg(12, 10), EPS);
        assertEquals(0, TipTuning.MarginDeg(-5, 10), EPS);
    }

    @Test
    @DisplayName("window: the readings' lowest and highest, a margin out each side, in whole degrees rounded outward")
    void window() {
        double[] window = TipTuning.Window(of(-40.4, -28.2), 10);
        assertEquals(-51, window[0], EPS);
        assertEquals(-18, window[1], EPS);
    }

    @Test
    @DisplayName("the coach's readings (roll -34 up, 25 down) leave windows that stay apart with a buffer")
    void coachReadings() {
        TipTuning.Stats up = of(-36, -34, -32);
        TipTuning.Stats down = of(23, 25, 27);
        double gap = TipTuning.GapDeg(up, down);
        double margin = TipTuning.MarginDeg(gap, 10);
        double[] upWindow = TipTuning.Window(up, margin);
        double[] downWindow = TipTuning.Window(down, margin);
        assertTrue(downWindow[0] - upWindow[1] >= 15, "buffer between the windows");
        assertFalse(upWindow[1] >= downWindow[0]);
    }

    @Test
    @DisplayName("enough: both sets need the minimum count")
    void enough() {
        assertFalse(TipTuning.Enough(of(1, 2, 3), of(1, 2, 3), 4));
        assertTrue(TipTuning.Enough(of(1, 2, 3, 4), of(1, 2, 3, 4), 4));
        assertFalse(TipTuning.Enough(of(1, 2, 3, 4), new TipTuning.Stats(), 4));
    }
}
