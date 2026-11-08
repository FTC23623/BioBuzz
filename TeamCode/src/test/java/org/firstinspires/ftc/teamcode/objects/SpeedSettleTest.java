package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Settle before launch (docs/decisions/0056): band 40 RPM, hold 0.15 s, timeout 1 s. */
@DisplayName("Speed settle: in the band long enough, or a timeout")
class SpeedSettleTest {
    private static final double BAND = 40;
    private static final double HOLD = 0.15;
    private static final double TIMEOUT = 1.0;
    private static final double TARGET = 3000;

    private static boolean Check(SpeedSettle settle, double now, double measured) {
        return settle.Update(now, measured, TARGET, BAND, HOLD, TIMEOUT);
    }

    @Test
    @DisplayName("not started: never done, never locked")
    void notStarted() {
        SpeedSettle settle = new SpeedSettle();
        assertFalse(Check(settle, 0, TARGET));
        assertFalse(settle.IsActive());
        assertFalse(settle.IsDone());
    }

    @Test
    @DisplayName("in the band for the hold time: settled, not timed out")
    void settles() {
        SpeedSettle settle = new SpeedSettle();
        settle.Start(0);
        assertFalse(Check(settle, 0.00, 2880));   // 120 off
        assertFalse(Check(settle, 0.05, 2970));   // in: hold starts
        assertFalse(Check(settle, 0.10, 3020));
        assertFalse(Check(settle, 0.15, 2990));   // 0.10 s in
        assertTrue(Check(settle, 0.20, 3010));    // 0.15 s in
        assertTrue(settle.IsDone());
        assertFalse(settle.TimedOut());
        assertEquals(10, settle.LastErrorRpm(), 1e-9);
    }

    @Test
    @DisplayName("leaving the band starts the hold over")
    void holdStartsOver() {
        SpeedSettle settle = new SpeedSettle();
        settle.Start(0);
        assertFalse(Check(settle, 0.00, 3000));   // in
        assertFalse(Check(settle, 0.10, 3045));   // out, by 45
        assertFalse(Check(settle, 0.15, 3000));   // in again: hold from 0.15
        assertFalse(Check(settle, 0.25, 3000));
        assertTrue(Check(settle, 0.31, 3000));
    }

    @Test
    @DisplayName("never settles: done anyway at the timeout, and says so")
    void timesOut() {
        SpeedSettle settle = new SpeedSettle();
        settle.Start(2);
        assertFalse(Check(settle, 2.5, 2900));
        assertFalse(Check(settle, 2.99, 3100));
        assertTrue(Check(settle, 3.0, 2900));
        assertTrue(settle.TimedOut());
    }

    @Test
    @DisplayName("cancel unlocks; start again begins a fresh wait")
    void cancelAndRestart() {
        SpeedSettle settle = new SpeedSettle();
        settle.Start(0);
        Check(settle, 1.0, 2000);
        assertTrue(settle.TimedOut());
        settle.Cancel();
        assertFalse(settle.IsActive());
        assertFalse(settle.IsDone());
        settle.Start(5);
        assertFalse(Check(settle, 5.0, 3000));
        assertFalse(settle.TimedOut());
    }

    @Test
    @DisplayName("finish: done at once, for a wheel that is not there")
    void finish() {
        SpeedSettle settle = new SpeedSettle();
        settle.Finish();
        assertTrue(settle.IsActive());
        assertTrue(settle.IsDone());
        assertFalse(settle.TimedOut());
    }
}
