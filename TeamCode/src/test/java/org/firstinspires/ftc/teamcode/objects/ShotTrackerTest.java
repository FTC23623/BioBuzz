package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Each ball found by the dip it makes in the pollen speed (docs/decisions/0052). */
@DisplayName("ShotTracker")
class ShotTrackerTest {
    private static final double EPS = 1e-9;
    private final ShotTracker tracker = new ShotTracker(100, 30, 1.5);

    @Test
    @DisplayName("a dip with the gates open is a shot: how deep, and how long to come back")
    void oneShot() {
        assertNull(tracker.Update(0.00, true, 3000, 2990));
        assertNull(tracker.Update(0.05, true, 3000, 2860));   // 140 under: a ball
        assertTrue(tracker.InShot());
        assertNull(tracker.Update(0.10, true, 3000, 2710));
        assertNull(tracker.Update(0.15, true, 3000, 2930));   // 70 under: not back yet
        ShotTracker.Shot shot = tracker.Update(0.25, true, 3000, 2975);
        assertNotNull(shot);
        assertEquals(1, shot.number);
        assertEquals(290, shot.dipRpm, EPS);
        assertEquals(0.20, shot.recoverySeconds, 1e-6);
        assertTrue(shot.recovered);
        assertEquals(3000, shot.targetRpm, EPS);
    }

    @Test
    @DisplayName("gates shut: a slow wheel is spinning up, not a shot")
    void gatesShut() {
        assertNull(tracker.Update(0.0, false, 3000, 2000));
        assertFalse(tracker.InShot());
    }

    @Test
    @DisplayName("never comes back: the shot ends at the time limit, marked not recovered")
    void timeout() {
        tracker.Update(0.0, true, 3000, 2800);
        assertNull(tracker.Update(1.0, true, 3000, 2850));
        ShotTracker.Shot shot = tracker.Update(1.5, true, 3000, 2850);
        assertNotNull(shot);
        assertFalse(shot.recovered);
        assertEquals(1, tracker.Count());
    }
}
