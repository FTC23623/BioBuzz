package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Characterization tests for {@link MecanumMath}.
 *
 * <p>Sign convention reminder (see MecanumMath): drive is raw left stick Y, so "forward"
 * on the stick is NEGATIVE drive. With left motors FORWARD and right motors REVERSE, a
 * wheel power's sign here means "roll forward" (positive) or "roll backward" (negative)
 * after the motor direction flips it -- for all four wheels the same way.
 */
@DisplayName("MecanumMath")
class MecanumMathTest {
    private static final double EPS = 1e-9;
    private MecanumMath.WheelPowers p;

    @BeforeEach
    void setUp() {
        p = new MecanumMath.WheelPowers();
    }

    private void assertWheels(double fl, double fr, double bl, double br) {
        assertEquals(fl, p.frontLeft, EPS, "front left");
        assertEquals(fr, p.frontRight, EPS, "front right");
        assertEquals(bl, p.backLeft, EPS, "back left");
        assertEquals(br, p.backRight, EPS, "back right");
    }

    @Nested
    @DisplayName("robot-centric")
    class RobotCentric {

        @Test
        @DisplayName("no input: every wheel stops")
        void idle() {
            MecanumMath.RobotCentric(0, 0, 0, p);
            assertWheels(0, 0, 0, 0);
        }

        @Test
        @DisplayName("drive only: all four wheels get the same power")
        void driveOnly() {
            MecanumMath.RobotCentric(0.5, 0, 0, p);
            assertWheels(0.5, 0.5, 0.5, 0.5);
        }

        @Test
        @DisplayName("strafe only: diagonal pairs oppose (FL/BR one way, FR/BL the other)")
        void strafeOnly() {
            MecanumMath.RobotCentric(0, 0.5, 0, p);
            assertWheels(0.5, -0.5, -0.5, 0.5);
        }

        @Test
        @DisplayName("rotate only: left side one way, right side the other")
        void rotateOnly() {
            MecanumMath.RobotCentric(0, 0, 0.5, p);
            assertWheels(0.5, -0.5, 0.5, -0.5);
        }

        @Test
        @DisplayName("inputs adding up to 1 or less are not scaled")
        void noScalingAtOrBelowOne() {
            MecanumMath.RobotCentric(0.5, 0.3, 0.2, p);
            assertWheels(0.5 + 0.3 + 0.2, 0.5 - 0.3 - 0.2, 0.5 - 0.3 + 0.2, 0.5 + 0.3 - 0.2);
        }

        @Test
        @DisplayName("inputs adding up past 1 are divided by their sum")
        void scalesBySum() {
            // sum = 1 + 1 + 1 = 3
            MecanumMath.RobotCentric(1, 1, 1, p);
            assertWheels(3.0 / 3, -1.0 / 3, 1.0 / 3, 1.0 / 3);
        }

        @Test
        @DisplayName("full stick plus 1.1 strafe gain still stays within [-1, 1]")
        void strafeGainStaysInRange() {
            MecanumMath.RobotCentric(-1, -1.1, 0, p);
            assertEquals(-1.0, p.frontLeft, EPS);
            assertEquals(-1.0, p.backRight, EPS);
        }
    }

    @Nested
    @DisplayName("field-centric")
    class FieldCentric {

        @ParameterizedTest(name = "drive {0}")
        @ValueSource(doubles = {0.0, 0.25, -0.7, 1.0})
        @DisplayName("at yaw 0 it matches robot-centric exactly")
        void yawZeroMatchesRobotCentric(double v) {
            MecanumMath.WheelPowers robot = new MecanumMath.WheelPowers();
            MecanumMath.RobotCentric(v, -v / 2, v / 3, robot);
            MecanumMath.FieldCentric(v, -v / 2, v / 3, 0, p);
            assertWheels(robot.frontLeft, robot.frontRight, robot.backLeft, robot.backRight);
        }

        @Test
        @DisplayName("robot turned 90 degrees: a drive command becomes a strafe")
        void ninetyDegreesTurnsDriveIntoStrafe() {
            // Worked by hand: yaw 90 -> angle -pi/2, cos 0, sin -1.
            // rotX = strafe*0 - drive*(-1) = drive; rotY = strafe*(-1) + drive*0 = -strafe.
            // With drive 0.5, strafe 0: rotX = 0.5, rotY = 0.
            MecanumMath.FieldCentric(0.5, 0, 0, 90, p);
            assertWheels(0.5, -0.5, -0.5, 0.5);
        }

        @Test
        @DisplayName("robot turned 180 degrees: drive is reversed")
        void oneEightyReversesDrive() {
            MecanumMath.FieldCentric(0.5, 0, 0, 180, p);
            assertWheels(-0.5, -0.5, -0.5, -0.5);
        }

        @Test
        @DisplayName("rotate is not affected by heading")
        void rotateIgnoresYaw() {
            MecanumMath.FieldCentric(0, 0, 0.4, 137, p);
            assertWheels(0.4, -0.4, 0.4, -0.4);
        }

        @Test
        @DisplayName("yaw 360 is the same as yaw 0")
        void fullTurnWraps() {
            MecanumMath.WheelPowers zero = new MecanumMath.WheelPowers();
            MecanumMath.FieldCentric(0.3, 0.6, -0.1, 0, zero);
            MecanumMath.FieldCentric(0.3, 0.6, -0.1, 360, p);
            assertWheels(zero.frontLeft, zero.frontRight, zero.backLeft, zero.backRight);
        }

        @Test
        @DisplayName("current behavior: full drive at 45 degrees asks a wheel for about 1.41")
        void diagonalOverdriveIsCurrentBehavior() {
            // Worked by hand: yaw 45 -> angle -pi/4, cos = 1/sqrt(2), sin = -1/sqrt(2).
            // rotX = 0 - 1*(-1/sqrt(2)) = 1/sqrt(2); rotY = 0 + 1*(1/sqrt(2)) = 1/sqrt(2).
            // Scaling uses the pre-rotation inputs: sum = 1, so nothing is scaled down.
            // FL = rotY + rotX = sqrt(2). The hub clips this to 1. See 0002-drive-port.md.
            MecanumMath.FieldCentric(1, 0, 0, 45, p);
            assertEquals(Math.sqrt(2), p.frontLeft, EPS);
        }

        @Disabled("possible bug: field-centric can ask for |power| > 1 (up to ~1.41). "
                + "Kept to match Decode; coach decision pending. See docs/decisions/0002-drive-port.md")
        @Test
        @DisplayName("every wheel power stays within [-1, 1] at every heading")
        void powersStayInRangeAtAllHeadings() {
            for (int yaw = -180; yaw <= 180; yaw += 5) {
                MecanumMath.FieldCentric(1, 0, 0, yaw, p);
                for (double w : new double[] {p.frontLeft, p.frontRight, p.backLeft, p.backRight}) {
                    assertEquals(0, Math.max(0, Math.abs(w) - 1), EPS, "yaw " + yaw);
                }
            }
        }
    }
}
