package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.function.Consumer;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The intake and transfer with pretend motors and a pretend operator gamepad. */
@DisplayName("Intake")
class IntakeTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();

    /** One pretend motor: the last power and speed (ticks per second) it was given. */
    private static class FakeMotor {
        final DcMotorEx motor = mock(DcMotorEx.class);
        double power = 0;
        double velocity = 0;

        FakeMotor() {
            doAnswer(inv -> {
                power = inv.getArgument(0);
                velocity = 0;
                return null;
            }).when(motor).setPower(anyDouble());
            doAnswer(inv -> {
                velocity = inv.getArgument(0);
                return null;
            }).when(motor).setVelocity(anyDouble());
        }
    }

    private final FakeMotor intake = new FakeMotor();
    private final FakeMotor transfer = new FakeMotor();
    private Gamepad gamepad;
    private Intake rollers;
    private LaunchGates gates;

    @BeforeEach
    void setUp() {
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgIntake)).thenReturn(intake.motor);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgTransfer)).thenReturn(transfer.motor);
        gamepad = mock(Gamepad.class);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), hardwareMap, mock(Gamepad.class), gamepad);
        gates = mock(LaunchGates.class);
        rollers = new Intake(op, properties, gamepad, gates);
    }

    /** One TeleOp loop. */
    private void loop() {
        rollers.HandleUserInput();
        rollers.Process();
    }

    /** Press a button and hold it long enough for the debouncer, then let go. */
    private void tap(Consumer<Boolean> button) {
        for (int i = 0; i < properties.debounce + 2; i++) {
            button.accept(true);
            loop();
        }
        button.accept(false);
        loop();
    }

    private double TicksPerSecond(double rpm) {
        return rpm / 60 * properties.intakeTicksPerRev;
    }

    @Test
    @DisplayName("starts off")
    void startsOff() {
        loop();
        assertEquals(0, intake.velocity, EPS);
        assertEquals(0, transfer.velocity, EPS);
    }

    @Test
    @DisplayName("left bumper: both run forward, each at its own speed; again: off")
    void bumperToggles() {
        properties.intakeRpm = 200;
        properties.transferRpm = 300;
        tap(down -> gamepad.left_bumper = down);
        assertTrue(rollers.IsOn());
        assertEquals(TicksPerSecond(200), intake.velocity, EPS);
        assertEquals(TicksPerSecond(300), transfer.velocity, EPS);

        tap(down -> gamepad.left_bumper = down);
        assertEquals(0, intake.power, EPS);
        assertEquals(0, transfer.power, EPS);
    }

    @Test
    @DisplayName("square held: both run backward at their reverse speeds, even when off")
    void squareReverses() {
        properties.intakeReverseRpm = 120;
        properties.transferReverseRpm = 180;
        gamepad.square = true;
        loop();
        assertTrue(rollers.IsReversing());
        assertEquals(-TicksPerSecond(120), intake.velocity, EPS);
        assertEquals(-TicksPerSecond(180), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("let go of square: back to forward if it was on")
    void releaseSquareGoesBack() {
        tap(down -> gamepad.left_bumper = down);
        gamepad.square = true;
        loop();
        gamepad.square = false;
        loop();
        assertFalse(rollers.IsReversing());
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
    }

    @Test
    @DisplayName("a speed changed in BotProperties (the dashboard) counts on the next loop")
    void liveSpeed() {
        rollers.SetOn(true);
        loop();
        properties.intakeRpm = 400;
        loop();
        assertEquals(TicksPerSecond(400), intake.velocity, EPS);
    }

    @Test
    @DisplayName("a robot with no intake motors still runs")
    void missingMotors() {
        HydraOpMode empty = new HydraOpMode(mock(Telemetry.class), mock(HardwareMap.class),
                mock(Gamepad.class), gamepad);
        Intake nothing = new Intake(empty, properties, gamepad, null);
        nothing.Init();
        nothing.SetOn(true);
        nothing.Process();
    }

    @Test
    @DisplayName("sends the roller gains once, then again only when one changes")
    void gainsFromSettings() {
        loop();
        loop();
        verify(intake.motor, times(1)).setPIDFCoefficients(eq(DcMotor.RunMode.RUN_USING_ENCODER),
                any(PIDFCoefficients.class));

        // A change on the dashboard: sent on the next loop, once.
        properties.rollerKP = 15;
        loop();
        loop();
        verify(intake.motor, times(2)).setPIDFCoefficients(eq(DcMotor.RunMode.RUN_USING_ENCODER),
                any(PIDFCoefficients.class));
        verify(transfer.motor, times(2)).setPIDFCoefficients(eq(DcMotor.RunMode.RUN_USING_ENCODER),
                any(PIDFCoefficients.class));
    }

    @Test
    @DisplayName("left trigger held: both run forward; let go: off again")
    void leftTriggerHold() {
        gamepad.left_trigger = 1;
        loop();
        assertTrue(rollers.IsRunning());
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);

        gamepad.left_trigger = 0;
        loop();
        assertFalse(rollers.IsRunning());
        assertEquals(0, intake.power, EPS);
        assertEquals(0, transfer.power, EPS);
    }

    @Test
    @DisplayName("left trigger let go while the bumper has them on: they stay on")
    void triggerDoesNotTurnOffBumper() {
        tap(down -> gamepad.left_bumper = down);
        gamepad.left_trigger = 1;
        loop();
        gamepad.left_trigger = 0;
        loop();
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
    }

    @Test
    @DisplayName("launching with the rollers off: intake and transfer both run forward, at their feed shares (intake half, transfer 30%)")
    void launchFeedsBoth() {
        when(gates.IsFullyOpen()).thenReturn(true);
        loop();
        assertEquals(TicksPerSecond(properties.transferRpm * properties.launchFeedTransferScale), transfer.velocity, EPS);
        assertEquals(TicksPerSecond(properties.intakeRpm * properties.launchFeedIntakeScale), intake.velocity, EPS);

        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(0, transfer.power, EPS);
    }

    @Test
    @DisplayName("launching with the rollers already on: launching wins, both drop to their feed shares")
    void launchWhileOn() {
        tap(down -> gamepad.left_bumper = down);
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);   // 100% while intaking
        when(gates.IsFullyOpen()).thenReturn(true);
        loop();
        assertEquals(TicksPerSecond(properties.intakeRpm * properties.launchFeedIntakeScale), intake.velocity, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm * properties.launchFeedTransferScale), transfer.velocity, EPS);
        // Gates closed again: back to full speed, since the bumper still has them on.
        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
    }

    @Test
    @DisplayName("the speeds fit the motor: full speed is the 1150 RPM top; the launch feed is half of it for the intake and 30% for the transfer")
    void speedsFitTheMotor() {
        assertEquals(1150, properties.intakeMaxRpm, EPS);
        assertTrue(properties.intakeRpm <= properties.intakeMaxRpm);
        assertTrue(properties.transferRpm <= properties.intakeMaxRpm);
        assertTrue(properties.intakeReverseRpm <= properties.intakeMaxRpm);
        assertTrue(properties.transferReverseRpm <= properties.intakeMaxRpm);
        assertEquals(0.5, properties.launchFeedIntakeScale, EPS);
        assertEquals(0.3, properties.launchFeedTransferScale, EPS);
    }

    @Test
    @DisplayName("square still wins while launching: reverse is for jams")
    void reverseWinsOverLaunch() {
        when(gates.IsFullyOpen()).thenReturn(true);
        gamepad.square = true;
        loop();
        assertEquals(-TicksPerSecond(properties.transferReverseRpm), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("gates still swinging open: the transfer waits, so no ball hits a closed gate")
    void waitsForTheGate() {
        when(gates.IsOpen()).thenReturn(true);
        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(0, transfer.velocity, EPS);
        assertEquals(0, intake.velocity, EPS);
    }

    @Test
    @DisplayName("rollers running when the trigger is pulled: the transfer STOPS while the gate opens, the intake slows to its feed speed at once")
    void runningTransferPausesForTheGate() {
        tap(down -> gamepad.left_bumper = down);
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);

        when(gates.IsOpen()).thenReturn(true);
        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(0, transfer.velocity, EPS);
        assertEquals(TicksPerSecond(properties.intakeRpm * properties.launchFeedIntakeScale), intake.velocity, EPS);

        // Gate open: the transfer runs again to feed the launch, at the feed speed.
        when(gates.IsFullyOpen()).thenReturn(true);
        loop();
        assertEquals(TicksPerSecond(properties.transferRpm * properties.launchFeedTransferScale), transfer.velocity, EPS);

        // Trigger let go: back to what the bumper says.
        when(gates.IsOpen()).thenReturn(false);
        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("square still wins while the gate opens: reverse is for jams")
    void reverseWinsOverGateOpening() {
        when(gates.IsOpen()).thenReturn(true);
        when(gates.IsFullyOpen()).thenReturn(false);
        gamepad.square = true;
        loop();
        assertEquals(-TicksPerSecond(properties.transferReverseRpm), transfer.velocity, EPS);
    }

    // ---- Auto-reject an extra ball (0040) ----

    /** Rollers built with a pretend pollen sensor. */
    private Intake withPollen(PollenSensor pollen) {
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgIntake)).thenReturn(intake.motor);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgTransfer)).thenReturn(transfer.motor);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), hardwareMap, mock(Gamepad.class), gamepad);
        return new Intake(op, properties, gamepad, gates, pollen);
    }

    private void loop(Intake r) {
        r.HandleUserInput();
        r.Process();
    }

    @Test
    @DisplayName("intaking and pollen stays detected: the intake runs backward, the transfer keeps going up")
    void rejectsAnExtraBall() {
        properties.pollenRejectAfterSeconds = 0;
        properties.pollenRejectSeconds = 60;
        PollenSensor pollen = mock(PollenSensor.class);
        when(pollen.IsDetected()).thenReturn(true);
        Intake r = withPollen(pollen);
        gamepad.left_trigger = 1;
        loop(r);
        assertTrue(r.IsRejecting());
        assertEquals(-TicksPerSecond(properties.intakeReverseRpm), intake.velocity, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("after the reject: both stop until the operator lets go, then work again")
    void blockedUntilReleased() {
        properties.pollenRejectAfterSeconds = 0;
        properties.pollenRejectSeconds = 0;
        PollenSensor pollen = mock(PollenSensor.class);
        when(pollen.IsDetected()).thenReturn(true);
        Intake r = withPollen(pollen);
        gamepad.left_trigger = 1;
        loop(r);   // reject starts
        loop(r);   // reject over: stopped, trigger still held
        loop(r);
        assertFalse(r.IsRunning());
        assertEquals(0, intake.power, EPS);
        assertEquals(0, transfer.power, EPS);

        when(pollen.IsDetected()).thenReturn(false);
        gamepad.left_trigger = 0;
        loop(r);   // let go: the block lifts
        gamepad.left_trigger = 1;
        loop(r);
        assertTrue(r.IsRunning());
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
    }

    @Test
    @DisplayName("the bumper toggle is turned off by a reject, and a press while blocked does nothing")
    void rejectTurnsTheToggleOff() {
        properties.pollenRejectAfterSeconds = 0;
        properties.pollenRejectSeconds = 0;
        PollenSensor pollen = mock(PollenSensor.class);
        Intake r = withPollen(pollen);
        r.SetOn(true);
        when(pollen.IsDetected()).thenReturn(true);
        loop(r);
        loop(r);
        assertFalse(r.IsOn());
        assertFalse(r.IsRunning());
    }

    @Test
    @DisplayName("pollen for less than the wait: no reject")
    void briefPollenIsNotAReject() {
        properties.pollenRejectAfterSeconds = 60;
        PollenSensor pollen = mock(PollenSensor.class);
        when(pollen.IsDetected()).thenReturn(true);
        Intake r = withPollen(pollen);
        gamepad.left_trigger = 1;
        loop(r);
        loop(r);
        assertFalse(r.IsRejecting());
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
    }

    @Test
    @DisplayName("not intaking, or launching, or switched off: pollen never starts a reject")
    void rejectOnlyWhileIntaking() {
        properties.pollenRejectAfterSeconds = 0;
        PollenSensor pollen = mock(PollenSensor.class);
        when(pollen.IsDetected()).thenReturn(true);
        Intake r = withPollen(pollen);
        loop(r);   // rollers off
        assertFalse(r.IsRejecting());

        when(gates.IsFullyOpen()).thenReturn(true);   // launching: balls pass the switch on purpose
        gamepad.left_trigger = 1;
        loop(r);
        assertFalse(r.IsRejecting());

        when(gates.IsFullyOpen()).thenReturn(false);
        properties.pollenAutoReject = false;
        loop(r);
        assertFalse(r.IsRejecting());
    }

    // ---- Autos: the transfer alone, the intake roller only for FLOWER (0043) ----

    @Test
    @DisplayName("auto, intake roller off: the transfer runs at full speed, the intake roller stays off")
    void transferOnly() {
        rollers.SetOn(true);
        rollers.SetIntakeRollerOn(false);
        rollers.Process();
        assertEquals(0, intake.power, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("auto launch: the transfer feeds at 30%, the intake roller stays off")
    void autoLaunchFeedsAtThirtyPercent() {
        rollers.SetOn(true);
        rollers.SetIntakeRollerOn(false);
        when(gates.IsFullyOpen()).thenReturn(true);
        rollers.Process();
        assertEquals(0.3, properties.launchFeedTransferScale, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm * 0.3), transfer.velocity, EPS);
        assertEquals(0, intake.power, EPS);
    }

    @Test
    @DisplayName("FLOWER turns the intake roller back on: both run")
    void flowerTurnsTheRollerOn() {
        rollers.SetOn(true);
        rollers.SetIntakeRollerOn(false);
        rollers.Process();
        rollers.SetIntakeRollerOn(true);
        rollers.Process();
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("intake roller off: pollen at the switch never starts a reject, so the transfer keeps running")
    void noRejectWithTheRollerOff() {
        properties.pollenRejectAfterSeconds = 0;
        PollenSensor pollen = mock(PollenSensor.class);
        when(pollen.IsDetected()).thenReturn(true);
        Intake r = withPollen(pollen);
        r.SetOn(true);
        r.SetIntakeRollerOn(false);
        r.Process();
        r.Process();
        assertFalse(r.IsRejecting());
        assertTrue(r.IsRunning());
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);
    }

    @Test
    @DisplayName("autos keep the intake roller off by default")
    void autoDefault() {
        assertFalse(new BotProperties().autoIntakeRoller);
    }
}
