package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The push back to a held spot. kP 0.05, max 0.4, done within 0.5 in. */
@DisplayName("HoldPosition: push back to the spot")
class HoldPositionTest {
    private static final double EPS = 1e-9;
    private static final double KP = 0.05;
    private static final double MAX = 0.4;
    private static final double DONE = 0.5;

    @Test
    @DisplayName("on the spot: no push")
    void onTheSpot() {
        HoldPosition.Push p = HoldPosition.Toward(10, 5, 10, 5, 0, KP, MAX, DONE);
        assertEquals(0, p.drive, EPS);
        assertEquals(0, p.strafe, EPS);
        assertEquals(0, p.offIn, EPS);
    }

    @Test
    @DisplayName("a tiny drift inside the done distance: no push, so it doesn't buzz")
    void insideDone() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, -0.4, 0, 0, KP, MAX, DONE);
        assertEquals(0, p.drive, EPS);
        assertEquals(0, p.strafe, EPS);
    }

    @Test
    @DisplayName("facing 0, pushed 4 in backward: drive forward (negative) at 0.2")
    void pushedBackDrivesForward() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, -4, 0, 0, KP, MAX, DONE);
        assertEquals(-0.2, p.drive, EPS);
        assertEquals(0, p.strafe, EPS);
        assertEquals(4, p.offIn, EPS);
    }

    @Test
    @DisplayName("facing 0, pushed 4 in to the right: strafe left (positive) at 0.2")
    void pushedRightStrafesLeft() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, 0, -4, 0, KP, MAX, DONE);
        assertEquals(0, p.drive, EPS);
        assertEquals(0.2, p.strafe, EPS);
    }

    @Test
    @DisplayName("facing 90 left, pushed 4 in toward field -x: that's the robot's left, so strafe right")
    void turnedRobotStrafes() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, -4, 0, 90, KP, MAX, DONE);
        assertEquals(0, p.drive, EPS);
        assertEquals(-0.2, p.strafe, EPS);
    }

    @Test
    @DisplayName("facing 90 left, pushed 4 in toward field -y: that's the robot's back, so drive forward")
    void turnedRobotDrives() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, 0, -4, 90, KP, MAX, DONE);
        assertEquals(-0.2, p.drive, EPS);
        assertEquals(0, p.strafe, EPS);
    }

    @Test
    @DisplayName("pushed far: the push stops at the max power")
    void clipsAtMax() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, -20, 0, 0, KP, MAX, DONE);
        assertEquals(-MAX, p.drive, EPS);
        assertEquals(20, p.offIn, EPS);
    }

    @Test
    @DisplayName("a broken reading (NaN): no push")
    void nanPushesNothing() {
        HoldPosition.Push p = HoldPosition.Toward(0, 0, Double.NaN, 0, 0, KP, MAX, DONE);
        assertEquals(0, p.drive, EPS);
        assertEquals(0, p.strafe, EPS);
    }
}
