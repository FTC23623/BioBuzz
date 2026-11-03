package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The launch gates with pretend servos and a pretend driver gamepad. */
@DisplayName("LaunchGates")
class LaunchGatesTest {
    private final BotProperties properties = new BotProperties();
    private Servo pollen;
    private Servo nectar;
    private Gamepad gamepad;
    private Telemetry telemetry;
    private Vision vision;
    private LaunchGates gates;

    @BeforeEach
    void setUp() {
        pollen = mock(Servo.class);
        nectar = mock(Servo.class);
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(Servo.class, properties.cfgPollenServo)).thenReturn(pollen);
        when(hardwareMap.tryGet(Servo.class, properties.cfgNectarServo)).thenReturn(nectar);
        gamepad = mock(Gamepad.class);
        telemetry = mock(Telemetry.class);
        vision = mock(Vision.class);
        HydraOpMode op = new HydraOpMode(telemetry, hardwareMap, gamepad, mock(Gamepad.class));
        gates = new LaunchGates(op, properties, gamepad, vision);
    }

    private void loop() {
        gates.HandleUserInput();
        gates.Process();
    }

    @Test
    @DisplayName("nothing moves during INIT")
    void stillAtInit() {
        gates.Init();
        verify(pollen, never()).setPosition(anyDouble());
        verify(nectar, never()).setPosition(anyDouble());
    }

    @Test
    @DisplayName("no trigger: both closed")
    void closedByDefault() {
        loop();
        verify(pollen).setPosition(properties.pollenServoClosed);
        verify(nectar).setPosition(properties.nectarServoClosed);
    }

    @Test
    @DisplayName("right trigger held: BOTH open at once")
    void triggerOpensBoth() {
        gamepad.right_trigger = 1;
        loop();
        assertTrue(gates.IsOpen());
        verify(pollen).setPosition(properties.pollenServoOpen);
        verify(nectar).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("hold to open: letting go closes them right away, no second press")
    void releaseCloses() {
        gamepad.right_trigger = 1;
        loop();
        gamepad.right_trigger = 0;
        loop();
        assertFalse(gates.IsOpen());
        verify(pollen).setPosition(properties.pollenServoClosed);
    }

    @Test
    @DisplayName("a trigger barely touched does not launch")
    void deadband() {
        gamepad.right_trigger = (float) (properties.trgBtnThresh / 2);
        loop();
        assertFalse(gates.IsOpen());
    }

    @Test
    @DisplayName("an auto opens them with SetOpen")
    void autoOpens() {
        gates.SetOpen(true);
        gates.Process();
        verify(pollen).setPosition(properties.pollenServoOpen);
    }

    @Test
    @DisplayName("open time: 80 degrees at 0.25 s per 60, plus 0.05 extra, is 0.38 s")
    void openSecondsMath() {
        properties.pollenServoClosed = 0.267;
        properties.pollenServoOpen = 0;
        properties.nectarServoClosed = 0;
        properties.nectarServoOpen = 0.2;
        properties.launchServoRangeDeg = 300;
        properties.launchServoSecondsPer60Deg = 0.25;
        properties.launchServoExtraSeconds = 0.05;
        // The slower one wins: pollen turns 80.1 degrees, nectar only 60.
        double expected = 0.267 * 300 / 60 * 0.25 + 0.05;
        assertEquals(expected, LaunchGates.OpenSeconds(properties), 1e-9);
    }

    @Test
    @DisplayName("just opened: not fully open yet, so nothing is pushed into it")
    void notFullyOpenAtFirst() {
        gamepad.right_trigger = 1;
        loop();
        assertTrue(gates.IsOpen());
        assertFalse(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("once the open time has passed: fully open")
    void fullyOpenAfterTheWait() {
        properties.launchServoSecondsPer60Deg = 0;
        properties.launchServoExtraSeconds = 0;
        gamepad.right_trigger = 1;
        loop();
        assertTrue(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("trigger pulled: the camera's distance to the tag at that moment is remembered and shown")
    void remembersLaunchRange() {
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetRange()).thenReturn(62.0);
        when(vision.GetTagId()).thenReturn(31);
        when(vision.GetGroupBearing()).thenReturn(-1.5);
        gamepad.right_trigger = 1;
        loop();
        // The camera moves on; the launch keeps the number it had.
        when(vision.GetRange()).thenReturn(99.0);
        loop();
        assertEquals(1, gates.LaunchCount());
        assertEquals(62.0, gates.LastLaunchRangeIn(), 1e-9);
        verify(telemetry, atLeast(1)).addData(eq("Last launch (camera)"), contains("#1: 62 in from tag 31, hive middle 1.5 deg to the right"));
    }

    @Test
    @DisplayName("trigger pulled with no tag in view: the launch is still counted, and says so")
    void launchWithoutTag() {
        when(vision.SeesTag()).thenReturn(false);
        gamepad.right_trigger = 1;
        loop();
        assertEquals(1, gates.LaunchCount());
        assertEquals(0, gates.LastLaunchRangeIn(), 1e-9);
        verify(telemetry, atLeast(1)).addData(eq("Last launch (camera)"), contains("#1: no tag in view"));
    }

    @Test
    @DisplayName("holding the trigger is one launch; pull again for the next")
    void oneLaunchPerPull() {
        gamepad.right_trigger = 1;
        loop();
        loop();
        loop();
        assertEquals(1, gates.LaunchCount());
        gamepad.right_trigger = 0;
        loop();
        gamepad.right_trigger = 1;
        loop();
        assertEquals(2, gates.LaunchCount());
    }

    @Test
    @DisplayName("let go and pull again: the wait starts over")
    void waitStartsOver() {
        gamepad.right_trigger = 1;
        loop();
        gamepad.right_trigger = 0;
        loop();
        assertFalse(gates.IsFullyOpen());
        gamepad.right_trigger = 1;
        loop();
        assertFalse(gates.IsFullyOpen());
    }
}
