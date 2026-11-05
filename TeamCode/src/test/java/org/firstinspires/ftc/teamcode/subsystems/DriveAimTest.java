package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Aim assist through the real drive code: a pretend camera says where the tag is, and the
 * four pretend motors show which way the robot would turn. Full speed, heading 0, so the
 * numbers are easy to read: a left turn is left wheels forward, right wheels backward.
 */
@DisplayName("Drive_Manual aim assist")
class DriveAimTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private final Map<String, double[]> power = new LinkedHashMap<>();
    private HydraOpMode op;
    private Gamepad gamepad;
    private Vision vision;
    private Drive_Manual drive;
    private Imu imu;

    private DcMotorEx motor(String name) {
        double[] last = {0};
        power.put(name, last);
        DcMotorEx m = mock(DcMotorEx.class);
        doAnswer(inv -> {
            last[0] = inv.getArgument(0);
            return null;
        }).when(m).setPower(anyDouble());
        when(m.getPower()).thenAnswer(inv -> last[0]);
        return m;
    }

    @BeforeEach
    void setUp() {
        HardwareMap hardwareMap = mock(HardwareMap.class);
        for (String name : new String[] {
                properties.cfgDriveFrontLeft, properties.cfgDriveFrontRight,
                properties.cfgDriveBackLeft, properties.cfgDriveBackRight}) {
            DcMotorEx m = motor(name);   // build the mock first: no stubbing inside a stubbing
            when(hardwareMap.get(DcMotorEx.class, name)).thenReturn(m);
        }
        gamepad = mock(Gamepad.class);
        imu = mock(Imu.class);
        when(imu.Ready()).thenReturn(true);
        when(imu.GetYaw()).thenReturn(0.0);
        vision = mock(Vision.class);
        op = new HydraOpMode(mock(Telemetry.class), hardwareMap, gamepad, mock(Gamepad.class));
        op.mSafeMode = false;
        // These tests check the aim itself, dead center. The pollen offset has its own test.
        properties.aimHiveBearingDeg = 0;
        drive = new Drive_Manual(op, properties, imu, vision);
    }

    /** Enough loops for the slew rate to reach any target. */
    private void runLoops() {
        for (int i = 0; i < 50; i++) {
            drive.HandleUserInput();
            drive.Process();
        }
    }

    private double frontLeft() {
        return power.get(properties.cfgDriveFrontLeft)[0];
    }

    private double frontRight() {
        return power.get(properties.cfgDriveFrontRight)[0];
    }

    private double backLeft() {
        return power.get(properties.cfgDriveBackLeft)[0];
    }

    private double backRight() {
        return power.get(properties.cfgDriveBackRight)[0];
    }

    /** A hive in view, its middle this many degrees left of center (the nearest tag may be elsewhere). */
    private void tagAt(double bearing) {
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetGroupBearing()).thenReturn(bearing);
        when(vision.GetBearing()).thenReturn(bearing);
        when(vision.GetTagId()).thenReturn(21);
    }

    @Test
    @DisplayName("aims at the MIDDLE of the hive's tags, not the nearest tag")
    void aimsAtTheGroupMiddle() {
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetBearing()).thenReturn(20.0);          // nearest tag is to the left...
        when(vision.GetGroupBearing()).thenReturn(-20.0);    // ...but the hive's middle is to the right
        gamepad.right_bumper = true;
        runLoops();
        assertEquals(-0.4, frontLeft(), EPS);   // turns right
        assertEquals(0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("launch trigger held: the robot snaps to the hive too, like an auto")
    void launchTriggerAims() {
        tagAt(20);
        gamepad.right_trigger = 1;
        runLoops();
        assertEquals(0.4, frontLeft(), EPS);
        assertEquals(-0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("launchTriggerAims off: the trigger only launches, the robot stays still")
    void launchTriggerAimOff() {
        properties.launchTriggerAims = false;
        tagAt(20);
        gamepad.right_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("right bumper held, tag 20 degrees to the left: the robot turns left by itself")
    void turnsLeftTowardTag() {
        gamepad.right_bumper = true;
        tagAt(20);
        runLoops();
        // kP 0.02 x 20 degrees = 0.4, with nothing on the sticks.
        assertEquals(0.4, frontLeft(), EPS);
        assertEquals(0.4, backLeft(), EPS);
        assertEquals(-0.4, frontRight(), EPS);
        assertEquals(-0.4, backRight(), EPS);
    }

    @Test
    @DisplayName("tag to the right: turns right")
    void turnsRightTowardTag() {
        gamepad.right_bumper = true;
        tagAt(-20);
        runLoops();
        assertEquals(-0.4, frontLeft(), EPS);
        assertEquals(0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("a right turn from aim assist is the same direction as pushing the stick right")
    void sameDirectionAsTheStick() {
        gamepad.right_stick_x = 1;
        runLoops();
        double stickFrontLeft = frontLeft();
        assertTrue(stickFrontLeft < 0, "stick right should give front left " + stickFrontLeft);

        gamepad.right_stick_x = 0;
        gamepad.right_bumper = true;
        tagAt(-20);
        runLoops();
        assertTrue(frontLeft() < 0, "tag on the right should turn the same way as stick right");
    }

    @Test
    @DisplayName("bumper held but no tag in view: the stick still turns the robot")
    void noTagStickStillWorks() {
        gamepad.right_bumper = true;
        when(vision.SeesTag()).thenReturn(false);
        gamepad.right_stick_x = 1;
        runLoops();
        assertTrue(frontLeft() < 0);
        assertTrue(frontRight() > 0);
    }

    @Test
    @DisplayName("bumper not held: the tag is ignored and the robot stays still")
    void notHeldIgnoresTag() {
        gamepad.right_bumper = false;
        tagAt(20);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("tag almost centered: no turning, so the robot does not wobble")
    void centeredTagNoTurn() {
        gamepad.right_bumper = true;
        tagAt(0.5);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    // ---- Aim only while a button is down (0040) ----

    @Test
    @DisplayName("bumper let go between reading and driving: no aim that loop")
    void releasedBeforeProcessDoesNotAim() {
        tagAt(20);
        gamepad.right_bumper = true;
        drive.HandleUserInput();
        gamepad.right_bumper = false;   // let go before the drive acts
        for (int i = 0; i < 50; i++) {
            drive.Process();
        }
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("no fresh gamepad reading: a bumper seen once does not keep the robot aiming")
    void staleInputDoesNotAim() {
        tagAt(20);
        gamepad.right_bumper = true;
        drive.HandleUserInput();
        drive.Process();
        // HandleUserInput stops being called, the bumper still reads down: aim must stop.
        for (int i = 0; i < 50; i++) {
            drive.Process();
        }
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("aim, then let go: the robot stops turning, with the tag still in view")
    void letGoStopsAiming() {
        tagAt(20);
        gamepad.right_bumper = true;
        runLoops();
        assertEquals(0.4, frontLeft(), EPS);
        gamepad.right_bumper = false;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    // ---- Arcing around the hive: D-pad left / right while aiming (0039) ----

    /** Aiming at a centered hive this many inches away, with round arc settings. */
    private void aimingAtHive(double rangeIn) {
        properties.arcStrafePower = 0.4;
        properties.arcRangeKP = 0.02;
        properties.arcMaxDrive = 0.4;
        gamepad.right_bumper = true;
        tagAt(0);
        when(vision.GetRange()).thenReturn(rangeIn);
    }

    @Test
    @DisplayName("aiming, D-pad left: the robot strafes left at the arc power, no turn")
    void arcLeftStrafesLeft() {
        aimingAtHive(100);
        gamepad.dpad_left = true;
        runLoops();
        // A pure strafe left: front left and back right forward, the other two backward.
        assertEquals(0.4, frontLeft(), EPS);
        assertEquals(0.4, backRight(), EPS);
        assertEquals(-0.4, frontRight(), EPS);
        assertEquals(-0.4, backLeft(), EPS);
    }

    @Test
    @DisplayName("aiming, D-pad right: strafes right")
    void arcRightStrafesRight() {
        aimingAtHive(100);
        gamepad.dpad_right = true;
        runLoops();
        assertEquals(-0.4, frontLeft(), EPS);
        assertEquals(0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("the arc holds its starting distance: drifted 10 in farther, it drives in at 0.2")
    void arcHoldsDistance() {
        aimingAtHive(100);
        gamepad.dpad_left = true;
        runLoops();   // the arc starts at 100 in
        when(vision.GetRange()).thenReturn(110.0);
        runLoops();
        // Strafe 0.4 plus 0.2 forward (negative): front left 0.4 - 0.2, front right -0.4 - 0.2.
        assertEquals(0.2, frontLeft(), EPS);
        assertEquals(-0.6, frontRight(), EPS);

        when(vision.GetRange()).thenReturn(90.0);   // now too near: back off
        runLoops();
        assertEquals(0.6, frontLeft(), EPS);
        assertEquals(-0.2, frontRight(), EPS);
    }

    @Test
    @DisplayName("let go and press again: the new arc holds the distance it starts at")
    void newArcNewDistance() {
        aimingAtHive(100);
        gamepad.dpad_left = true;
        runLoops();
        gamepad.dpad_left = false;
        runLoops();
        when(vision.GetRange()).thenReturn(120.0);
        gamepad.dpad_left = true;
        runLoops();
        // 120 is the new distance to hold, so no drive correction: a pure strafe.
        assertEquals(0.4, frontLeft(), EPS);
        assertEquals(-0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("D-pad without the aim button: nothing happens")
    void dpadAloneDoesNothing() {
        properties.arcStrafePower = 0.4;
        tagAt(0);
        when(vision.GetRange()).thenReturn(100.0);
        gamepad.dpad_left = true;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("aiming with no tag in view: the D-pad does nothing")
    void arcNeedsATag() {
        properties.arcStrafePower = 0.4;
        gamepad.right_bumper = true;
        when(vision.SeesTag()).thenReturn(false);
        gamepad.dpad_right = true;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("left and right pressed together: neither")
    void bothWaysIsNeither() {
        aimingAtHive(100);
        gamepad.dpad_left = true;
        gamepad.dpad_right = true;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }

    @Test
    @DisplayName("the arc is robot-centric: turned 90 degrees, D-pad left still strafes the robot's left")
    void arcIsRobotCentric() {
        Imu turned = mock(Imu.class);
        when(turned.Ready()).thenReturn(true);
        when(turned.GetYaw()).thenReturn(90.0);
        drive = new Drive_Manual(op, properties, turned, vision);
        aimingAtHive(100);
        gamepad.dpad_left = true;
        runLoops();
        assertEquals(0.4, frontLeft(), EPS);
        assertEquals(-0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("left of the hive: with the hive dead ahead, the robot turns right until the hive sits 1 deg left")
    void pollenOffsetTurnsRight() {
        properties.aimHiveBearingDeg = 1;
        properties.aimDoneDegrees = 0.1;
        tagAt(0);
        when(vision.GetSideDegrees()).thenReturn(20.0);
        gamepad.right_bumper = true;
        runLoops();
        // 1 degree to go, to the right: 0.02 x -1 = -0.02.
        assertEquals(-0.02, frontLeft(), EPS);
        assertEquals(0.02, frontRight(), EPS);
        tagAt(1);   // now aimed
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }

    @Test
    @DisplayName("right of the hive: the offset flips, the robot turns left until the hive sits 1 deg right")
    void rightSideTurnsLeft() {
        properties.aimHiveBearingDeg = 1;
        properties.aimDoneDegrees = 0.1;
        tagAt(0);
        when(vision.GetSideDegrees()).thenReturn(-20.0);
        gamepad.right_bumper = true;
        runLoops();
        // 1 degree to go, to the left: 0.02 x 1 = 0.02.
        assertEquals(0.02, frontLeft(), EPS);
        assertEquals(-0.02, frontRight(), EPS);
        tagAt(-1);   // now aimed
        when(vision.GetSideDegrees()).thenReturn(-20.0);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }

    @Test
    @DisplayName("wiggle: square with the launch trigger turns left first, toward 1 deg left of the start")
    void wiggleStartsLeft() {
        properties.wiggleDeg = 1;
        properties.wiggleKP = 0.1;
        properties.wiggleMaxRotate = 0.3;
        properties.wiggleSwitchSeconds = 60;   // long, so the test stays on the left side
        properties.driveNormal = 1;
        gamepad.square = true;
        gamepad.right_trigger = 1;
        runLoops();
        // 1 degree to go, left: 0.1 x 1 = 0.1, a left turn (positive rotate).
        assertEquals(0.1, frontLeft(), EPS);
        assertEquals(-0.1, frontRight(), EPS);
    }

    @Test
    @DisplayName("wiggle: overrides aim while held; square alone (no trigger) does nothing")
    void wiggleNeedsTheTrigger() {
        properties.wiggleSwitchSeconds = 60;
        properties.driveNormal = 1;
        gamepad.square = true;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        tagAt(-20);   // aim alone would turn right hard
        gamepad.right_trigger = 1;
        runLoops();
        assertEquals(0.1, frontLeft(), EPS);   // the wiggle's gentle left wins
    }

    @Test
    @DisplayName("wiggle: no heading from the IMU, no wiggle")
    void wiggleNeedsTheImu() {
        when(imu.Ready()).thenReturn(false);
        properties.wiggleSwitchSeconds = 60;
        gamepad.square = true;
        gamepad.right_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }
}
