package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The ball switches' pure rules: what counts as a ball, and how long the robot has been empty. */
@DisplayName("Ball sensors")
class BallSensorsTest {
    @Test
    @DisplayName("pressed is a ball; inverted flips it")
    void ballOn() {
        assertTrue(BallSensors.BallOn(true, false));
        assertFalse(BallSensors.BallOn(false, false));
        assertFalse(BallSensors.BallOn(true, true));
        assertTrue(BallSensors.BallOn(false, true));
    }

    @Test
    @DisplayName("empty time: 0 with a ball or with no switch; else the time since the last ball")
    void emptySeconds() {
        assertEquals(0, BallSensors.EmptySeconds(true, true, 5), 1e-9);
        assertEquals(0, BallSensors.EmptySeconds(false, false, 5), 1e-9);
        assertEquals(5, BallSensors.EmptySeconds(true, false, 5), 1e-9);
    }
}
