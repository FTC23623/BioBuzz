package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The launch wiggle: 1 deg left, then right, switching every 1/6 s. */
@DisplayName("Wiggle")
class WiggleTest {
    private static final double EPS = 1e-9;
    private static final double SWITCH = 10.0 / 60.0;

    @Test
    @DisplayName("left first, then right, then left again, a side per ball")
    void sides() {
        assertEquals(1, Wiggle.Side(0, SWITCH));
        assertEquals(1, Wiggle.Side(0.16, SWITCH));
        assertEquals(-1, Wiggle.Side(0.17, SWITCH));
        assertEquals(-1, Wiggle.Side(0.33, SWITCH));
        assertEquals(1, Wiggle.Side(0.34, SWITCH));
    }

    @Test
    @DisplayName("no switch time: stays left, nothing divides by zero")
    void zeroSwitch() {
        assertEquals(1, Wiggle.Side(5, 0));
    }

    @Test
    @DisplayName("turns toward 1 deg left of the center, then 1 deg right")
    void turnsToEachSide() {
        assertEquals(0.1, Wiggle.Turn(0, 0, 1, 1, 0.1, 0.3), EPS);
        assertEquals(-0.1, Wiggle.Turn(0, 0, -1, 1, 0.1, 0.3), EPS);
        // Already 1 deg left and told to go right: 2 deg to turn.
        assertEquals(-0.2, Wiggle.Turn(1, 0, -1, 1, 0.1, 0.3), EPS);
        assertEquals(0, Wiggle.Turn(-1, 0, -1, 1, 0.1, 0.3), EPS);
    }

    @Test
    @DisplayName("never turns harder than the limit")
    void limited() {
        assertEquals(0.3, Wiggle.Turn(-10, 0, 1, 1, 0.1, 0.3), EPS);
    }

    @Test
    @DisplayName("facing backward, across 180 / -180: takes the short way")
    void acrossBackward() {
        // Center 179.5, aim 1 deg left = 180.5 = -179.5. Robot at 179.5: 1 deg left to go.
        assertEquals(0.1, Wiggle.Turn(179.5, 179.5, 1, 1, 0.1, 0.3), EPS);
        assertEquals(-0.1, Wiggle.Turn(-179.5, -179.5, -1, 1, 0.1, 0.3), EPS);
    }
}
