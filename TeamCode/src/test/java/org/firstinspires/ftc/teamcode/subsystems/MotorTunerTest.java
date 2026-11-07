package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.function.Consumer;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The motor tuner with four pretend motors and a pretend gamepad. Each motor remembers the
 * last power and the last speed it was given, and reports whatever speed the test sets.
 */
@DisplayName("MotorTuner")
class MotorTunerTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private final double UPDATE_SECONDS = properties.launcherUpdateMs / 1000;

    /** One pretend motor: the last power and velocity it was given, and the speed it reports. */
    private static class FakeMotor {
        final DcMotorEx motor = mock(DcMotorEx.class);
        double power = 0;
        double velocity = 0;
        double ticksPerSecond = 0;

        FakeMotor() {
            doAnswer(inv -> {
                power = inv.getArgument(0);
                return null;
            }).when(motor).setPower(anyDouble());
            doAnswer(inv -> {
                velocity = inv.getArgument(0);
                return null;
            }).when(motor).setVelocity(anyDouble());
            when(motor.getVelocity()).thenAnswer(inv -> ticksPerSecond);
        }
    }

    private final FakeMotor pollen = new FakeMotor();
    private final FakeMotor nectar = new FakeMotor();
    private final FakeMotor intake = new FakeMotor();
    private final FakeMotor transfer = new FakeMotor();
    private Gamepad gamepad;
    private MotorTuner tuner;

    @BeforeEach
    void setUp() {
        // Set here, so tuning BotProperties on the robot can't break the tests.
        properties.pollenLauncherReversed = false;
        // The tuner's roller checks are in speeds (the hub's speed control), not power mode.
        properties.rollerUsePower = false;
        properties.nectarLauncherReversed = true;
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgLaunchPollen)).thenReturn(pollen.motor);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgLaunchNectar)).thenReturn(nectar.motor);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgIntake)).thenReturn(intake.motor);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgTransfer)).thenReturn(transfer.motor);
        gamepad = mock(Gamepad.class);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), hardwareMap, gamepad, gamepad);
        tuner = new MotorTuner(op, properties, gamepad);
    }

    /** Press a button and hold it long enough for the debouncer, then let go. */
    private void tap(Consumer<Boolean> button) {
        for (int loop = 0; loop < properties.debounce + 2; loop++) {
            button.accept(true);
            tuner.HandleUserInput();
        }
        button.accept(false);
        tuner.HandleUserInput();
    }

    private void launcherSpinsAt(FakeMotor launcher, double rpm) {
        launcher.ticksPerSecond = rpm / 60 * properties.launcherTicksPerRev;
    }

    @Test
    @DisplayName("starts with every motor off")
    void startsOff() {
        tuner.Update(UPDATE_SECONDS);
        assertEquals(0, pollen.power, EPS);
        assertEquals(0, nectar.power, EPS);
        assertEquals(0, intake.power, EPS);
        assertEquals(0, transfer.power, EPS);
    }

    @Test
    @DisplayName("launchers coast like Launcher; intake and transfer use the hub's speed control")
    void motorSetUp() {
        verify(nectar.motor).setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        verify(pollen.motor).setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        // nectarLauncherReversed true, pollenLauncherReversed false (set in setUp).
        verify(nectar.motor).setDirection(DcMotor.Direction.REVERSE);
        verify(pollen.motor).setDirection(DcMotor.Direction.FORWARD);
        verify(intake.motor).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        verify(transfer.motor).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    @Test
    @DisplayName("D-pad changes only the motor pressed last: every speed is its own")
    void dpadChangesPickedMotorOnly() {
        double pollenStart = properties.pollenLauncherRpm;
        double nectarStart = properties.nectarLauncherRpm;
        double intakeStart = properties.intakeRpm;
        double transferStart = properties.transferRpm;

        tap(down -> gamepad.circle = down);
        assertEquals("Nectar launcher", tuner.GetPickedName());
        tap(down -> gamepad.dpad_up = down);
        assertEquals(nectarStart + properties.launcherRpmStep, properties.nectarLauncherRpm, EPS);
        assertEquals(pollenStart, properties.pollenLauncherRpm, EPS);

        tap(down -> gamepad.square = down);
        tap(down -> gamepad.dpad_down = down);
        assertEquals(transferStart - properties.intakeRpmStep, properties.transferRpm, EPS);
        assertEquals(intakeStart, properties.intakeRpm, EPS);
    }

    @Test
    @DisplayName("targets never go below 0 or above the maximum")
    void targetsStayInRange() {
        tap(down -> gamepad.cross = down);
        properties.intakeRpm = 10;
        tuner.ChangePickedRpm(-1);
        assertEquals(0, properties.intakeRpm, EPS);
        tap(down -> gamepad.triangle = down);
        properties.pollenLauncherRpm = properties.launcherMaxRpm;
        tuner.ChangePickedRpm(1);
        assertEquals(properties.launcherMaxRpm, properties.pollenLauncherRpm, EPS);
    }

    @Test
    @DisplayName("triangle turns on the pollen launcher only; a second press turns it off")
    void triangleTogglesPollen() {
        tap(down -> gamepad.triangle = down);
        tuner.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > 0, "pollen should spin");
        assertEquals(0, nectar.power, EPS);
        assertEquals(0, intake.power, EPS);

        tap(down -> gamepad.triangle = down);
        tuner.Update(UPDATE_SECONDS);
        assertEquals(0, pollen.power, EPS);
    }

    @Test
    @DisplayName("two launchers with different targets get different powers")
    void launchersHaveTheirOwnTargets() {
        properties.pollenLauncherRpm = 3000;
        properties.nectarLauncherRpm = 1000;
        tap(down -> gamepad.triangle = down);
        tap(down -> gamepad.circle = down);
        launcherSpinsAt(pollen, 900);
        launcherSpinsAt(nectar, 900);
        tuner.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > nectar.power, "pollen aims higher, so it gets more power");
    }

    @Test
    @DisplayName("cross turns on the intake at its own speed, in encoder ticks per second")
    void crossRunsIntake() {
        tap(down -> gamepad.cross = down);
        tuner.Update(UPDATE_SECONDS);
        double expected = properties.intakeRpm / 60 * properties.intakeTicksPerRev;
        assertEquals(expected, intake.velocity, EPS);
        assertEquals(0, transfer.velocity, EPS);
    }

    @Test
    @DisplayName("square turns on the transfer at its own speed")
    void squareRunsTransfer() {
        properties.transferRpm = 300;
        tap(down -> gamepad.square = down);
        tuner.Update(UPDATE_SECONDS);
        double expected = 300.0 / 60 * properties.intakeTicksPerRev;
        assertEquals(expected, transfer.velocity, EPS);
    }

    @Test
    @DisplayName("a short reverse while powered is ignored: the launcher keeps pushing")
    void shortReverseIsFine() {
        tap(down -> gamepad.triangle = down);
        launcherSpinsAt(pollen, -500);
        tuner.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > 0, "one backward reading must not stop it");
        launcherSpinsAt(pollen, 900);
        tuner.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > 0);
    }

    @Test
    @DisplayName("backward without a break for launcherWrongWaySeconds: stopped, so it can't run away")
    void longReverseStops() {
        tap(down -> gamepad.triangle = down);
        launcherSpinsAt(pollen, -500);
        int updates = (int) Math.ceil(properties.launcherWrongWaySeconds / UPDATE_SECONDS);
        for (int i = 0; i < updates; i++) {
            tuner.Update(UPDATE_SECONDS);
        }
        assertEquals(0, pollen.power, EPS);

        // Coasting to a stop does not restart it.
        launcherSpinsAt(pollen, 0);
        tuner.Update(UPDATE_SECONDS);
        assertEquals(0, pollen.power, EPS);
    }

    @Test
    @DisplayName("off and on again clears the wrong-way stop")
    void offAndOnClears() {
        tap(down -> gamepad.triangle = down);
        launcherSpinsAt(pollen, -500);
        int updates = (int) Math.ceil(properties.launcherWrongWaySeconds / UPDATE_SECONDS);
        for (int i = 0; i < updates; i++) {
            tuner.Update(UPDATE_SECONDS);
        }
        tap(down -> gamepad.triangle = down);
        tap(down -> gamepad.triangle = down);
        launcherSpinsAt(pollen, 0);
        tuner.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > 0, "it should push again");
    }

    @Test
    @DisplayName("a wheel that is OFF can be spun backward without any stop")
    void offWheelNeverStops() {
        launcherSpinsAt(pollen, -2000);
        for (int i = 0; i < 100; i++) {
            tuner.Update(UPDATE_SECONDS);
        }
        tap(down -> gamepad.triangle = down);
        launcherSpinsAt(pollen, 0);
        tuner.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > 0);
    }
}
