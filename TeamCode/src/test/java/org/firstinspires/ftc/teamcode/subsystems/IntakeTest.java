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
    @DisplayName("launching with the rollers off: intake and transfer both run forward, at the feed speed (half)")
    void launchFeedsBoth() {
        when(gates.IsFullyOpen()).thenReturn(true);
        loop();
        assertEquals(TicksPerSecond(properties.transferRpm * properties.launchFeedScale), transfer.velocity, EPS);
        assertEquals(TicksPerSecond(properties.intakeRpm * properties.launchFeedScale), intake.velocity, EPS);

        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(0, transfer.power, EPS);
    }

    @Test
    @DisplayName("launching with the rollers already on: launching wins, both drop to the feed speed")
    void launchWhileOn() {
        tap(down -> gamepad.left_bumper = down);
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);   // 100% while intaking
        when(gates.IsFullyOpen()).thenReturn(true);
        loop();
        assertEquals(TicksPerSecond(properties.intakeRpm * properties.launchFeedScale), intake.velocity, EPS);
        assertEquals(TicksPerSecond(properties.transferRpm * properties.launchFeedScale), transfer.velocity, EPS);
        // Gates closed again: back to full speed, since the bumper still has them on.
        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);
    }

    @Test
    @DisplayName("the speeds fit the motor: full speed is the 1150 RPM top, and the feed is half of it")
    void speedsFitTheMotor() {
        assertEquals(1150, properties.intakeMaxRpm, EPS);
        assertTrue(properties.intakeRpm <= properties.intakeMaxRpm);
        assertTrue(properties.transferRpm <= properties.intakeMaxRpm);
        assertTrue(properties.intakeReverseRpm <= properties.intakeMaxRpm);
        assertTrue(properties.transferReverseRpm <= properties.intakeMaxRpm);
        assertEquals(0.5, properties.launchFeedScale, EPS);
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
    @DisplayName("transfer already running when the trigger is pulled: it STOPS while the gate opens, the intake keeps going")
    void runningTransferPausesForTheGate() {
        tap(down -> gamepad.left_bumper = down);
        assertEquals(TicksPerSecond(properties.transferRpm), transfer.velocity, EPS);

        when(gates.IsOpen()).thenReturn(true);
        when(gates.IsFullyOpen()).thenReturn(false);
        loop();
        assertEquals(0, transfer.velocity, EPS);
        assertEquals(TicksPerSecond(properties.intakeRpm), intake.velocity, EPS);

        // Gate open: the transfer runs again to feed the launch, at the feed speed.
        when(gates.IsFullyOpen()).thenReturn(true);
        loop();
        assertEquals(TicksPerSecond(properties.transferRpm * properties.launchFeedScale), transfer.velocity, EPS);

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
}
