package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The heading the auto hands to TeleOp (docs/decisions/0046). */
@DisplayName("HeadingHandoff")
class HeadingHandoffTest {
    private static final double EPS = 1e-9;

    @BeforeEach
    void setUp() {
        HeadingHandoff.Clear();
    }

    @Test
    @DisplayName("TeleOp heading: the start facing plus Pedro's turn")
    void teleOpYaw() {
        // Starting at the driver's right (-90), a 90 left turn faces away from the driver.
        assertEquals(0, HeadingHandoff.TeleOpYaw(-90, 90), EPS);
        assertEquals(-150, HeadingHandoff.TeleOpYaw(90, 120), EPS);
        assertEquals(90, HeadingHandoff.TeleOpYaw(90, 0), EPS);
    }

    @Test
    @DisplayName("TeleOp heading: always between -180 and 180")
    void teleOpYawWraps() {
        assertEquals(85, HeadingHandoff.TeleOpYaw(-90, -185), EPS);
        assertEquals(-170, HeadingHandoff.TeleOpYaw(90, 100), EPS);
        assertEquals(10, HeadingHandoff.TeleOpYaw(0, 730), EPS);
    }

    @Test
    @DisplayName("nothing saved: NaN")
    void nothingSaved() {
        assertTrue(Double.isNaN(HeadingHandoff.Take(100, 120)));
    }

    @Test
    @DisplayName("saved just now: TeleOp gets it, once")
    void takeOnce() {
        HeadingHandoff.Save(-60, 100);
        assertEquals(-60, HeadingHandoff.Take(110, 120), EPS);
        assertTrue(Double.isNaN(HeadingHandoff.Take(111, 120)), "used once only");
    }

    @Test
    @DisplayName("the last save wins")
    void lastSaveWins() {
        HeadingHandoff.Save(10, 100);
        HeadingHandoff.Save(20, 101);
        assertEquals(20, HeadingHandoff.Take(102, 120), EPS);
    }

    @Test
    @DisplayName("too old: the robot may have been moved, NaN")
    void tooOld() {
        HeadingHandoff.Save(45, 100);
        assertTrue(Double.isNaN(HeadingHandoff.Take(221, 120)));
    }

    @Test
    @DisplayName("an auto's INIT clears an old one")
    void clear() {
        HeadingHandoff.Save(45, 100);
        HeadingHandoff.Clear();
        assertTrue(Double.isNaN(HeadingHandoff.Take(101, 120)));
    }
}
