package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The turn toward a tag: kP 0.02 per degree, never past 0.6, done within 1 degree. */
@DisplayName("AimAssist")
class AimAssistTest {
    private static final double EPS = 1e-9;

    private double turn(double bearing) {
        return AimAssist.RotateToward(bearing, 0.02, 0.6, 1.0);
    }

    @Test
    @DisplayName("tag to the left (positive bearing): turn left (positive)")
    void leftTurnsLeft() {
        assertEquals(0.4, turn(20), EPS);
    }

    @Test
    @DisplayName("tag to the right: turn right")
    void rightTurnsRight() {
        assertEquals(-0.4, turn(-20), EPS);
    }

    @Test
    @DisplayName("never turns harder than the limit, either way")
    void limited() {
        assertEquals(0.6, turn(90), EPS);
        assertEquals(-0.6, turn(-90), EPS);
    }

    @Test
    @DisplayName("close enough to center: stop turning, so the robot does not wobble")
    void doneNearCenter() {
        assertEquals(0, turn(0.5), EPS);
        assertEquals(0, turn(-1.0), EPS);
        assertEquals(0, turn(0), EPS);
    }

    @Test
    @DisplayName("a broken number (NaN) turns nothing")
    void nanTurnsNothing() {
        assertEquals(0, turn(Double.NaN), EPS);
    }

    @Test
    @DisplayName("a negative kP flips the direction, for a camera that reads backwards")
    void negativeKpFlips() {
        assertEquals(-0.4, AimAssist.RotateToward(20, -0.02, 0.6, 1.0), EPS);
    }

    /** Holding a distance while arcing: kP 0.02 per inch, never past 0.4. */
    private double hold(double rangeIn, double wantedIn) {
        return AimAssist.HoldRange(rangeIn, wantedIn, 0.02, 0.4);
    }

    @Test
    @DisplayName("hold range: too far away drives forward (negative), too near backs off")
    void holdRangeDirection() {
        assertEquals(-0.2, hold(110, 100), EPS);
        assertEquals(0.2, hold(90, 100), EPS);
        assertEquals(0, hold(100, 100), EPS);
    }

    @Test
    @DisplayName("hold range: never drives harder than the limit")
    void holdRangeLimited() {
        assertEquals(-0.4, hold(200, 100), EPS);
        assertEquals(0.4, hold(10, 100), EPS);
    }

    @Test
    @DisplayName("hold range: an unknown distance (0) drives nothing")
    void holdRangeUnknown() {
        assertEquals(0, hold(0, 100), EPS);
        assertEquals(0, hold(100, 0), EPS);
    }

    private static final int[][] HIVES = {{30, 33}, {34, 37}, {38, 41}, {42, 45}};

    @Test
    @DisplayName("group middle: averages the tags in the nearest tag's group")
    void groupAverages() {
        List<Integer> ids = Arrays.asList(30, 31, 33);
        List<Double> bearings = Arrays.asList(10.0, 4.0, -2.0);
        assertEquals(4.0, AimAssist.GroupCenterBearing(ids, bearings, 31, HIVES), EPS);
    }

    @Test
    @DisplayName("group middle: tags from another hive are left out")
    void otherGroupIgnored() {
        List<Integer> ids = Arrays.asList(42, 45, 37);
        List<Double> bearings = Arrays.asList(6.0, 2.0, -30.0);
        assertEquals(4.0, AimAssist.GroupCenterBearing(ids, bearings, 42, HIVES), EPS);
    }

    @Test
    @DisplayName("group middle: a tag in no group aims at that tag alone")
    void noGroupAimsAtTag() {
        List<Integer> ids = Arrays.asList(7, 30);
        List<Double> bearings = Arrays.asList(-5.0, 20.0);
        assertEquals(-5.0, AimAssist.GroupCenterBearing(ids, bearings, 7, HIVES), EPS);
    }

    @Test
    @DisplayName("group average of distances: zeros (no distance) are left out")
    void groupAverageSkipsZeros() {
        List<Integer> ids = Arrays.asList(30, 31, 33, 42);
        List<Double> ranges = Arrays.asList(100.0, 104.0, 0.0, 300.0);
        assertEquals(102, AimAssist.GroupAverage(ids, ranges, 30, HIVES, true), EPS);
        // Kept in, the zero pulls the average down: that is why distances skip them.
        assertEquals(68, AimAssist.GroupAverage(ids, ranges, 30, HIVES, false), EPS);
        assertEquals(0, AimAssist.GroupAverage(Arrays.asList(30), Arrays.asList(0.0), 30, HIVES, true), EPS);
    }

    @Test
    @DisplayName("group middle: nothing in view, 0")
    void emptyIsZero() {
        assertEquals(0, AimAssist.GroupCenterBearing(Arrays.<Integer>asList(), Arrays.<Double>asList(), 31, HIVES), EPS);
    }

    @Test
    @DisplayName("aim error: the hive's bearing less where it should sit")
    void aimError() {
        assertEquals(4, AimAssist.AimError(5, 1), EPS);
        assertEquals(-1, AimAssist.AimError(0, 1), EPS);
        assertEquals(0, AimAssist.AimError(1, 1), EPS);
    }

    @Test
    @DisplayName("side offset: left of the hive aims further right, right of it further left")
    void sideOffsetFlips() {
        assertEquals(3, AimAssist.SideOffset(20, 3, 5), EPS);
        assertEquals(-3, AimAssist.SideOffset(-20, 3, 5), EPS);
    }

    @Test
    @DisplayName("side offset: straight in front, or side not known, aims at the middle")
    void sideOffsetCenter() {
        assertEquals(0, AimAssist.SideOffset(4.9, 3, 5), EPS);
        assertEquals(0, AimAssist.SideOffset(-5, 3, 5), EPS);
        assertEquals(0, AimAssist.SideOffset(0, 3, 5), EPS);
    }
}
