package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Aim assist through the real drive code: a pretend camera says where the tag is, and the
 * four pretend motors show which way the robot would turn. Full speed, heading 0, so the
 * numbers are easy to read: a left turn is left wheels forward, right wheels backward.
 *
 * <p>Aim and launch are the OPERATOR's (gamepad 2, {@link LaunchChoice}): left bumper / trigger
 * pollen, right bumper / trigger nectar. The driver only drives.
 */
@DisplayName("Drive_Manual aim assist")
class DriveAimTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private final Map<String, double[]> power = new LinkedHashMap<>();
    private HydraOpMode op;
    private Gamepad gamepad;
    /** Gamepad 2: aims and launches. */
    private Gamepad operator;
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
        operator = mock(Gamepad.class);
        imu = mock(Imu.class);
        when(imu.Ready()).thenReturn(true);
        when(imu.GetYaw()).thenReturn(0.0);
        vision = mock(Vision.class);
        op = new HydraOpMode(mock(Telemetry.class), hardwareMap, gamepad, operator);
        op.mSafeMode = false;
        // These tests check the aim itself, dead center. The side offset has its own tests;
        // the launcher offset is off unless a test turns it on.
        properties.aimHiveBearingDeg = 0;
        properties.launcherSideOffsetIn = 0;
        properties.launcherOffsetNoRangeDeg = 0;
        // The aim tests check aim alone; the wiggle tests turn it back on.
        properties.launchWiggle = false;
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
        operator.left_bumper = true;
        runLoops();
        assertEquals(-0.4, frontLeft(), EPS);   // turns right
        assertEquals(0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("launch trigger held: the robot snaps to the hive too, like an auto")
    void launchTriggerAims() {
        tagAt(20);
        operator.left_trigger = 1;
        runLoops();
        assertEquals(0.4, frontLeft(), EPS);
        assertEquals(-0.4, frontRight(), EPS);
    }

    @Test
    @DisplayName("launchTriggerAims off: the trigger only launches, the robot stays still")
    void launchTriggerAimOff() {
        properties.launchTriggerAims = false;
        tagAt(20);
        operator.left_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("operator's left bumper (aim pollen) held, tag 20 degrees to the left: the robot turns left by itself")
    void turnsLeftTowardTag() {
        operator.left_bumper = true;
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
        operator.left_bumper = true;
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
        operator.left_bumper = true;
        tagAt(-20);
        runLoops();
        assertTrue(frontLeft() < 0, "tag on the right should turn the same way as stick right");
    }

    @Test
    @DisplayName("bumper held but no tag in view: the stick still turns the robot")
    void noTagStickStillWorks() {
        operator.left_bumper = true;
        when(vision.SeesTag()).thenReturn(false);
        gamepad.right_stick_x = 1;
        runLoops();
        assertTrue(frontLeft() < 0);
        assertTrue(frontRight() > 0);
    }

    @Test
    @DisplayName("bumper not held: the tag is ignored and the robot stays still")
    void notHeldIgnoresTag() {
        operator.left_bumper = false;
        tagAt(20);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("driving with a tag in view, no aim button: the wheels match the sticks alone")
    void drivingPastATagDoesNotSteer() {
        // While driving, the camera may change the launcher's speed (always-track), but never
        // the heading: only the aim buttons let a tag turn the robot.
        gamepad.left_stick_y = -0.6f;
        gamepad.left_stick_x = 0.3f;
        when(vision.SeesTag()).thenReturn(false);
        runLoops();
        double[] sticksAlone = {frontLeft(), frontRight(), backLeft(), backRight()};

        tagAt(25);
        when(vision.GetRange()).thenReturn(120.0);
        runLoops();
        assertEquals(sticksAlone[0], frontLeft(), EPS);
        assertEquals(sticksAlone[1], frontRight(), EPS);
        assertEquals(sticksAlone[2], backLeft(), EPS);
        assertEquals(sticksAlone[3], backRight(), EPS);
    }

    @Test
    @DisplayName("aiming turns the robot, but never rewrites the heading: the Pinpoint measures the turn")
    void aimNeverRewritesHeading() {
        operator.left_bumper = true;
        tagAt(20);
        runLoops();
        verify(imu, never()).SetYaw(anyDouble());
        verify(imu, never()).ResetYaw();
    }

    // ---- Hold position while launching (launchHoldsPosition) ----

    /** The Pinpoint says the robot is here. */
    private void robotAt(double x, double y) {
        when(imu.HasPosition()).thenReturn(true);
        when(imu.GetXIn()).thenReturn(x);
        when(imu.GetYIn()).thenReturn(y);
    }

    /** The wheel powers driving the drive stick alone gives, with no hold and no trigger. */
    private double[] stickOnly(float driveStick) {
        gamepad.left_stick_y = driveStick;
        runLoops();
        double[] powers = {frontLeft(), frontRight(), backLeft(), backRight()};
        gamepad.left_stick_y = 0;
        runLoops();
        return powers;
    }

    @Test
    @DisplayName("launch hold: bumped 4 in backward while launching, the robot drives forward like the stick at 0.2")
    void holdPushesBack() {
        double[] forward = stickOnly(-0.2f);
        robotAt(0, 0);
        operator.left_trigger = 1;
        when(vision.SeesTag()).thenReturn(false);
        drive.HandleUserInput();
        drive.Process();          // the spot is (0, 0)
        robotAt(-4, 0);           // shoved back 4 in
        runLoops();
        // The stick is a float, so allow a float's rounding.
        double floatEps = 1e-6;
        assertEquals(forward[0], frontLeft(), floatEps);
        assertEquals(forward[1], frontRight(), floatEps);
        assertEquals(forward[2], backLeft(), floatEps);
        assertEquals(forward[3], backRight(), floatEps);
    }

    @Test
    @DisplayName("launch hold: let go of the trigger, no more push")
    void holdEndsWithTheTrigger() {
        robotAt(0, 0);
        operator.left_trigger = 1;
        drive.HandleUserInput();
        drive.Process();
        robotAt(-4, 0);
        operator.left_trigger = 0;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("launch hold: moving the stick wins, and once let go the new spot is where it stopped")
    void sticksBeatTheHold() {
        robotAt(0, 0);
        operator.left_trigger = 1;
        drive.HandleUserInput();
        drive.Process();
        // The driver drives 10 in forward on purpose, trigger still held.
        gamepad.left_stick_y = -0.5f;
        robotAt(10, 0);
        runLoops();
        gamepad.left_stick_y = 0;
        runLoops();
        // Stopped at 10 in: that is the new spot, so no push back to 0.
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("launch hold: shoved past the give-up distance, it stops pushing until the trigger is let go")
    void holdGivesUp() {
        properties.launchHoldGiveUpIn = 18;
        robotAt(0, 0);
        operator.left_trigger = 1;
        drive.HandleUserInput();
        drive.Process();
        robotAt(-20, 0);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        robotAt(-4, 0);           // back near the spot: still given up
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("launch hold: no position (Control Hub IMU), nothing is held")
    void holdNeedsAPosition() {
        when(imu.HasPosition()).thenReturn(false);
        when(imu.GetXIn()).thenReturn(-4.0);
        operator.left_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("launch hold: launchHoldsPosition off, the trigger never pushes")
    void holdOff() {
        properties.launchHoldsPosition = false;
        robotAt(0, 0);
        operator.left_trigger = 1;
        drive.HandleUserInput();
        drive.Process();
        robotAt(-4, 0);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("tag almost centered: no turning, so the robot does not wobble")
    void centeredTagNoTurn() {
        operator.left_bumper = true;
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
        operator.left_bumper = true;
        drive.HandleUserInput();
        operator.left_bumper = false;   // let go before the drive acts
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
        operator.left_bumper = true;
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
        operator.left_bumper = true;
        runLoops();
        assertEquals(0.4, frontLeft(), EPS);
        operator.left_bumper = false;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    // ---- Arcing around the hive: D-pad left / right while aiming (0039) ----

    /** Aiming at a centered hive this many inches away, with the arc on and round arc settings. */
    private void aimingAtHive(double rangeIn) {
        properties.aimArc = true;
        properties.arcStrafePower = 0.4;
        properties.arcRangeKP = 0.02;
        properties.arcMaxDrive = 0.4;
        operator.left_bumper = true;
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
        properties.aimArc = true;
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
        properties.aimArc = true;
        properties.arcStrafePower = 0.4;
        operator.left_bumper = true;
        when(vision.SeesTag()).thenReturn(false);
        gamepad.dpad_right = true;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("aimArc off (the default): aiming with the D-pad held does not strafe")
    void arcOffByDefault() {
        aimingAtHive(100);
        properties.aimArc = new BotProperties().aimArc;   // what the robot ships with
        gamepad.dpad_left = true;
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
        operator.left_bumper = true;
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
        operator.left_bumper = true;
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
    @DisplayName("launcher offset: in front of the hive, pollen turns right until the hive sits 1 deg left, nectar the other way")
    void launcherOffsetsAreOpposite() {
        properties.launcherOffsetNoRangeDeg = 1;   // no range in these tests: a round 1 degree
        properties.aimDoneDegrees = 0.1;
        tagAt(0);
        operator.left_bumper = true;
        runLoops();
        // Pollen sits left of the camera: 1 degree to go, to the right: 0.02 x -1 = -0.02.
        assertEquals(-0.02, frontLeft(), EPS);
        operator.left_bumper = false;
        operator.right_bumper = true;
        runLoops();
        // Nectar sits right of the camera: the mirror, to the left.
        assertEquals(0.02, frontLeft(), EPS);
        tagAt(-1);   // now aimed for nectar
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }

    @Test
    @DisplayName("the side offset is the SAME for nectar: left of the hive (no launcher offset), nectar turns right like pollen")
    void nectarSideOffsetSameAsPollen() {
        properties.aimHiveBearingDeg = 1;
        properties.aimDoneDegrees = 0.1;
        tagAt(0);
        when(vision.GetSideDegrees()).thenReturn(20.0);
        operator.right_bumper = true;
        runLoops();
        assertEquals(-0.02, frontLeft(), EPS);
        assertEquals(0.02, frontRight(), EPS);
    }

    @Test
    @DisplayName("nectar launch trigger (right trigger) aims for nectar too")
    void nectarTriggerAims() {
        properties.launcherOffsetNoRangeDeg = 1;
        properties.aimDoneDegrees = 0.1;
        tagAt(0);
        operator.right_trigger = 1;
        runLoops();
        assertEquals(0.02, frontLeft(), EPS);
        assertEquals(-0.02, frontRight(), EPS);
    }

    @Test
    @DisplayName("both operator bumpers held: pollen wins, the robot aims for pollen's launcher offset")
    void bothBumpersPollenWins() {
        properties.launcherOffsetNoRangeDeg = 1;
        properties.aimDoneDegrees = 0.1;
        tagAt(0);
        operator.left_bumper = true;
        operator.right_bumper = true;
        runLoops();
        // Same as pollen alone: turns right.
        assertEquals(-0.02, frontLeft(), EPS);
        assertEquals(0.02, frontRight(), EPS);
    }

    @Test
    @DisplayName("the driver's right bumper and right trigger do nothing now: no aim")
    void driverButtonsDoNotAim() {
        tagAt(20);
        gamepad.right_bumper = true;
        gamepad.right_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    @Test
    @DisplayName("the driver's right trigger does not hold position either")
    void driverTriggerDoesNotHold() {
        robotAt(0, 0);
        gamepad.right_trigger = 1;
        drive.HandleUserInput();
        drive.Process();
        robotAt(-4, 0);
        runLoops();
        assertEquals(0, frontLeft(), EPS);
        assertEquals(0, frontRight(), EPS);
    }

    /** Press a button long enough for the debouncer, then let go. */
    private void tap(Consumer<Boolean> button) {
        for (int loop = 0; loop < properties.debounce + 2; loop++) {
            button.accept(true);
            drive.HandleUserInput();
            drive.Process();
        }
        button.accept(false);
        drive.HandleUserInput();
        drive.Process();
    }

    @Test
    @DisplayName("wiggle on: the launch trigger alone turns left first, toward 1 deg left of the start")
    void wiggleStartsLeft() {
        properties.launchWiggle = true;
        properties.wiggleDeg = 1;
        properties.wiggleKP = 0.1;
        properties.wiggleMaxRotate = 0.3;
        properties.wiggleSwitchSeconds = 60;   // long, so the test stays on the left side
        properties.driveNormal = 1;
        operator.left_trigger = 1;
        runLoops();
        // 1 degree to go, left: 0.1 x 1 = 0.1, a left turn (positive rotate).
        assertEquals(0.1, frontLeft(), EPS);
        assertEquals(-0.1, frontRight(), EPS);
    }

    @Test
    @DisplayName("wiggle on: aim turns first; once aimed the wiggle takes over and keeps going")
    void wiggleWaitsForAim() {
        properties.launchWiggle = true;
        properties.wiggleDeg = 1;   // 1 deg to go x kP 0.1 = 0.1; BotProperties may say more
        properties.wiggleKP = 0.1;
        properties.wiggleSwitchSeconds = 60;
        properties.driveNormal = 1;
        tagAt(-20);   // the hive well to the right
        operator.left_trigger = 1;
        runLoops();
        assertTrue(frontLeft() < -0.1, "aim turns right hard first");
        tagAt(-0.5);  // aimed
        runLoops();
        assertEquals(0.1, frontLeft(), EPS);   // the wiggle's gentle left
        tagAt(-20);   // the camera's next look: the wiggle carries on, it does not hand back
        runLoops();
        assertEquals(0.1, frontLeft(), EPS);
    }

    @Test
    @DisplayName("circle re-sets forward in HyDrive, but not with circleResetsForward off (Pose Recorder: FLOWER)")
    void circleResetsForward() {
        properties.fieldCentricDrive = true;
        properties.circleResetsForward = false;
        tap(down -> gamepad.circle = down);
        verify(imu, never()).ResetYaw();
        properties.circleResetsForward = true;
        tap(down -> gamepad.circle = down);
        verify(imu).ResetYaw();
    }

    @Test
    @DisplayName("wiggle is off at START (square turns it on)")
    void wiggleOffByDefault() {
        assertFalse(new BotProperties().launchWiggle);
    }

    @Test
    @DisplayName("wiggle: no trigger, no wiggle")
    void wiggleNeedsTheTrigger() {
        properties.launchWiggle = true;
        properties.wiggleSwitchSeconds = 60;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }

    @Test
    @DisplayName("square: wiggle off (two rumbles), then on again (one)")
    void squareToggles() {
        properties.launchWiggle = true;
        properties.wiggleSwitchSeconds = 60;
        tap(down -> gamepad.square = down);
        assertFalse(properties.launchWiggle);
        verify(gamepad).rumbleBlips(2);
        operator.left_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);   // off: the trigger launches without a wiggle
        operator.left_trigger = 0;
        tap(down -> gamepad.square = down);
        assertTrue(properties.launchWiggle);
        verify(gamepad).rumbleBlips(1);
    }

    @Test
    @DisplayName("wiggle: no heading from the IMU, no wiggle")
    void wiggleNeedsTheImu() {
        when(imu.Ready()).thenReturn(false);
        properties.launchWiggle = true;
        properties.wiggleSwitchSeconds = 60;
        operator.left_trigger = 1;
        runLoops();
        assertEquals(0, frontLeft(), EPS);
    }
}
