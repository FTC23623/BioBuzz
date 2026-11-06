package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import java.util.function.Consumer;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The launch servo tuner with two pretend servos and a pretend gamepad. */
@DisplayName("LaunchServoTuner")
class LaunchServoTunerTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private Servo pollen;
    private Servo nectar;
    private Servo extractor;
    private Gamepad gamepad;
    private LaunchServoTuner tuner;

    @BeforeEach
    void setUp() {
        pollen = mock(Servo.class);
        nectar = mock(Servo.class);
        extractor = mock(Servo.class);
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.get(Servo.class, properties.cfgPollenServo)).thenReturn(pollen);
        when(hardwareMap.get(Servo.class, properties.cfgNectarServo)).thenReturn(nectar);
        when(hardwareMap.get(Servo.class, properties.cfgFlowerExtractor)).thenReturn(extractor);
        gamepad = mock(Gamepad.class);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), hardwareMap, gamepad, gamepad);
        tuner = new LaunchServoTuner(op, properties, gamepad);
    }

    /** Press a D-pad direction and hold it long enough for the debouncer, then let go. */
    private void tap(Consumer<Boolean> button) {
        for (int loop = 0; loop < properties.debounce + 2; loop++) {
            button.accept(true);
            tuner.HandleUserInput();
        }
        button.accept(false);
        tuner.HandleUserInput();
    }

    /** One step, as a 0-to-1 position: 5 of 300 degrees. */
    private double step() {
        return properties.launchServoStepDeg / properties.launchServoRangeDeg;
    }

    @Test
    @DisplayName("INIT does not move any servo")
    void initDoesNotMove() {
        tuner.Init();
        verify(pollen, never()).setPosition(anyDouble());
        verify(nectar, never()).setPosition(anyDouble());
        verify(extractor, never()).setPosition(anyDouble());
    }

    @Test
    @DisplayName("first loop after START: all three servos go to closed")
    void startsClosed() {
        tuner.Process();
        assertEquals(properties.pollenServoClosed, tuner.GetPollenPosition(), EPS);
        assertEquals(properties.nectarServoClosed, tuner.GetNectarPosition(), EPS);
        verify(nectar).setPosition(0.0);
        verify(extractor).setPosition(0.0);
    }

    @Test
    @DisplayName("pollen: starts closed at 0.267, D-pad down opens it one step, up can't pass closed")
    void pollenOpensDownward() {
        tap(down -> gamepad.dpad_up = down);
        assertEquals(0.267, tuner.GetPollenPosition(), EPS);
        tap(down -> gamepad.dpad_down = down);
        assertEquals(0.267 - step(), tuner.GetPollenPosition(), EPS);
        assertEquals(0, tuner.GetNectarPosition(), EPS);
    }

    @Test
    @DisplayName("nectar: starts closed at 0, D-pad right opens it one step, left can't pass closed")
    void nectarOpensUpward() {
        tap(down -> gamepad.dpad_left = down);
        assertEquals(0, tuner.GetNectarPosition(), EPS);
        tap(down -> gamepad.dpad_right = down);
        assertEquals(step(), tuner.GetNectarPosition(), EPS);
        assertEquals(0.267, tuner.GetPollenPosition(), EPS);
    }

    @Test
    @DisplayName("launch servos never leave 0 to 0.267, however many presses")
    void launchServosStaySafe() {
        for (int press = 0; press < 40; press++) {
            tap(down -> gamepad.dpad_down = down);
            tap(down -> gamepad.dpad_right = down);
        }
        assertEquals(properties.pollenServoOpen, tuner.GetPollenPosition(), EPS);
        assertEquals(properties.nectarServoOpen, tuner.GetNectarPosition(), EPS);
    }

    @Test
    @DisplayName("flower extractor: bumpers step it, from 0 up to 10 degrees past open (about 0.617)")
    void extractorStaysSafe() {
        tap(down -> gamepad.left_bumper = down);
        assertEquals(0, tuner.GetExtractorPosition(), EPS);
        tap(down -> gamepad.right_bumper = down);
        assertEquals(step(), tuner.GetExtractorPosition(), EPS);
        for (int press = 0; press < 80; press++) {
            tap(down -> gamepad.right_bumper = down);
        }
        assertEquals(0.584 + 10.0 / 300, tuner.GetExtractorPosition(), EPS);
    }

    @Test
    @DisplayName("past open from the bumpers: cross brings it back to open, not past")
    void crossFromPastOpenGoesToOpen() {
        for (int press = 0; press < 80; press++) {
            tap(down -> gamepad.right_bumper = down);
        }
        tap(down -> gamepad.cross = down);
        assertEquals(properties.flowerExtractorOpen, tuner.GetExtractorPosition(), EPS);
    }

    @Test
    @DisplayName("cross: flower extractor deploys to 0.584, then stores to 0 on the next press")
    void crossDeploysAndStoresExtractor() {
        tap(down -> gamepad.cross = down);
        assertEquals(0.584, tuner.GetExtractorPosition(), EPS);
        assertEquals(properties.pollenServoClosed, tuner.GetPollenPosition(), EPS);
        assertEquals(properties.nectarServoClosed, tuner.GetNectarPosition(), EPS);
        tap(down -> gamepad.cross = down);
        assertEquals(0, tuner.GetExtractorPosition(), EPS);
    }

    @Test
    @DisplayName("holding the D-pad moves one step, not one per loop")
    void holdingIsOneStep() {
        for (int loop = 0; loop < 50; loop++) {
            gamepad.dpad_right = true;
            tuner.HandleUserInput();
        }
        assertEquals(step(), tuner.GetNectarPosition(), EPS);
    }

    @Test
    @DisplayName("the new position is sent to the servo on the next loop")
    void sendsNewPosition() {
        tap(down -> gamepad.dpad_right = down);
        tuner.Process();
        verify(nectar).setPosition(step());
    }

    @Test
    @DisplayName("triangle: pollen opens, then closes on the next press; nectar stays closed")
    void triangleOpensAndClosesPollen() {
        tap(down -> gamepad.triangle = down);
        assertEquals(properties.pollenServoOpen, tuner.GetPollenPosition(), EPS);
        assertEquals(properties.nectarServoClosed, tuner.GetNectarPosition(), EPS);
        tap(down -> gamepad.triangle = down);
        assertEquals(properties.pollenServoClosed, tuner.GetPollenPosition(), EPS);
    }

    @Test
    @DisplayName("circle: nectar opens, then closes on the next press; pollen stays closed")
    void circleOpensAndClosesNectar() {
        tap(down -> gamepad.circle = down);
        assertEquals(properties.nectarServoOpen, tuner.GetNectarPosition(), EPS);
        assertEquals(properties.pollenServoClosed, tuner.GetPollenPosition(), EPS);
        tap(down -> gamepad.circle = down);
        assertEquals(properties.nectarServoClosed, tuner.GetNectarPosition(), EPS);
    }

    @Test
    @DisplayName("part way open from the D-pad: circle opens it the rest of the way")
    void partWayOpensFully() {
        tap(down -> gamepad.dpad_right = down);
        tap(down -> gamepad.circle = down);
        assertEquals(properties.nectarServoOpen, tuner.GetNectarPosition(), EPS);
    }

    @Test
    @DisplayName("holding circle opens once, it does not flicker open and closed")
    void holdingCircleIsOnePress() {
        for (int loop = 0; loop < 50; loop++) {
            gamepad.circle = true;
            tuner.HandleUserInput();
        }
        assertEquals(properties.nectarServoOpen, tuner.GetNectarPosition(), EPS);
    }
}
