package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * The driver practice options through the real drive code: swapped sticks, tight turns and
 * drift assist. Full speed, robot-centric, heading 0. A left turn is left wheels positive,
 * right wheels negative; driving forward is every wheel negative (see MecanumMath).
 */
@DisplayName("Drive_Manual practice options")
class DrivePracticeOptionsTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private final Map<String, double[]> power = new LinkedHashMap<>();
    private Gamepad gamepad;
    private Drive_Manual drive;

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
        Imu imu = mock(Imu.class);
        when(imu.Ready()).thenReturn(true);
        when(imu.GetYaw()).thenReturn(0.0);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), hardwareMap, gamepad, mock(Gamepad.class));
        op.mSafeMode = false;
        // Robot-centric, as the class comment says. HyDrive's default is field-centric now.
        properties.fieldCentricDrive = false;
        drive = new Drive_Manual(op, properties, imu, mock(Vision.class));
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

    @Test
    @DisplayName("swapped: left stick X turns, gently")
    void swappedLeftStickTurns() {
        properties.driveSwapStrafeRotate = true;
        gamepad.left_stick_x = -1;    // push left
        runLoops();
        // Turn gain 0.5: a half-power left turn.
        assertEquals(0.5, frontLeft(), EPS);
        assertEquals(0.5, backLeft(), EPS);
        assertEquals(-0.5, frontRight(), EPS);
        assertEquals(-0.5, backRight(), EPS);
    }

    @Test
    @DisplayName("swapped: right stick X strafes")
    void swappedRightStickStrafes() {
        properties.driveSwapStrafeRotate = true;
        properties.driveStrafeGain = 1;    // keep the numbers round
        gamepad.right_stick_x = -0.5f;    // push left
        runLoops();
        // Strafe left: front left and back right one way, the other pair the other way.
        assertEquals(0.5, frontLeft(), EPS);
        assertEquals(0.5, backRight(), EPS);
        assertEquals(-0.5, frontRight(), EPS);
        assertEquals(-0.5, backLeft(), EPS);
    }

    @Test
    @DisplayName("not swapped: the right stick still turns, the way HyDrive always has")
    void normalRightStickTurns() {
        gamepad.right_stick_x = -0.5f;
        runLoops();
        assertEquals(0.5, frontLeft(), EPS);
        assertEquals(-0.5, frontRight(), EPS);
    }

    @Test
    @DisplayName("tight turns: the same stick turns half again as hard")
    void tightTurnsAreStronger() {
        properties.driveTightTurns = true;
        gamepad.right_stick_x = -0.5f;
        runLoops();
        assertEquals(0.75, frontLeft(), EPS);
        assertEquals(-0.75, frontRight(), EPS);
    }

    @Test
    @DisplayName("drift assist (on by default) and left trigger held: a left turn pivots on the back-left wheel")
    void driftPivotsOnInsideBackWheel() {
        gamepad.left_trigger = 1;
        gamepad.right_stick_x = -1;    // full left turn
        runLoops();
        assertEquals(0, backLeft(), EPS);
        // The far front corner swings the most.
        assertEquals(-1, frontRight(), EPS);
    }

    @Test
    @DisplayName("drift assist switched off: the left trigger does nothing to the drive")
    void driftOffIgnoresTrigger() {
        properties.driveDriftAssist = false;
        gamepad.left_trigger = 1;
        gamepad.right_stick_x = -1;
        runLoops();
        // A plain spin in the middle: every wheel the same size.
        assertEquals(1, frontLeft(), EPS);
        assertEquals(1, backLeft(), EPS);
        assertEquals(-1, frontRight(), EPS);
        assertEquals(-1, backRight(), EPS);
    }

    @Test
    @DisplayName("left bumper held: slow mode, driveSlow of the power (half)")
    void leftBumperIsSlow() {
        assertEquals(0.5, properties.driveSlow, EPS);
        gamepad.left_bumper = true;
        gamepad.left_stick_y = -1;    // full forward
        runLoops();
        assertEquals(-properties.driveSlow, frontLeft(), EPS);
        assertEquals(-properties.driveSlow, backRight(), EPS);
    }

    @Test
    @DisplayName("the right bumper is aim now, not slow mode")
    void rightBumperIsNotSlow() {
        gamepad.right_bumper = true;
        gamepad.left_stick_y = -1;
        runLoops();
        assertEquals(-1, frontLeft(), EPS);
    }

    @Test
    @DisplayName("the left trigger is not slow mode any more")
    void leftTriggerIsNotSlow() {
        gamepad.left_trigger = 1;
        gamepad.left_stick_y = -1;
        runLoops();
        assertEquals(-1, frontLeft(), EPS);
    }
}
