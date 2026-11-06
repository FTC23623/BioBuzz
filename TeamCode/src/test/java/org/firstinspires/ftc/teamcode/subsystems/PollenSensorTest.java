package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The pollen switch with a pretend sensor and a pretend operator gamepad. */
@DisplayName("PollenSensor")
class PollenSensorTest {
    private final BotProperties properties = new BotProperties();
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private TouchSensor touch;
    private Gamepad operator;
    private HydraOpMode op;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        touch = mock(TouchSensor.class);
        operator = mock(Gamepad.class);
        op = new HydraOpMode(telemetry, hardwareMap, mock(Gamepad.class), operator);
    }

    private PollenSensor sensorConfigured() {
        when(hardwareMap.tryGet(TouchSensor.class, properties.cfgPollenSensor)).thenReturn(touch);
        return new PollenSensor(op, properties);
    }

    @Test
    @DisplayName("as wired (inverted): the switch closed means NO pollen, open means pollen")
    void invertedByDefault() {
        assertTrue(properties.pollenSensorInverted);
        PollenSensor sensor = sensorConfigured();
        when(touch.isPressed()).thenReturn(true);
        assertFalse(sensor.IsPressed());
        when(touch.isPressed()).thenReturn(false);
        assertTrue(sensor.IsPressed());
    }

    @Test
    @DisplayName("plain wiring: closed means pollen")
    void plainWiring() {
        properties.pollenSensorInverted = false;
        PollenSensor sensor = sensorConfigured();
        when(touch.isPressed()).thenReturn(true);
        assertTrue(sensor.IsPressed());
    }

    @Test
    @DisplayName("no switch in the robot configuration: never pollen, either wiring, and INIT says so")
    void missingSensor() {
        PollenSensor sensor = new PollenSensor(op, properties);
        sensor.Init();
        assertFalse(sensor.IsPressed());
        assertFalse(sensor.IsDetected());
        verify(telemetry).addLine(contains("Pollen sensor: MISSING"));
    }

    @Test
    @DisplayName("detected holds on after the switch lets go, for pollenHoldMs")
    void detectedHolds() {
        properties.pollenSensorInverted = false;
        properties.pollenHoldMs = 60_000;
        PollenSensor sensor = sensorConfigured();
        when(touch.isPressed()).thenReturn(true);
        assertTrue(sensor.IsDetected());
        when(touch.isPressed()).thenReturn(false);
        assertTrue(sensor.IsDetected());
    }

    @Test
    @DisplayName("detected lets go once the hold time has passed")
    void detectedEnds() throws InterruptedException {
        properties.pollenSensorInverted = false;
        properties.pollenHoldMs = 20;
        PollenSensor sensor = sensorConfigured();
        when(touch.isPressed()).thenReturn(true);
        assertTrue(sensor.IsDetected());
        when(touch.isPressed()).thenReturn(false);
        Thread.sleep(60);
        assertFalse(sensor.IsDetected());
    }

    @Test
    @DisplayName("pollen arrives: the operator's gamepad rumbles once, not every loop")
    void rumblesOnceOnArrival() {
        properties.pollenSensorInverted = false;
        PollenSensor sensor = sensorConfigured();
        sensor.Process();
        verify(operator, never()).rumble(anyInt());

        when(touch.isPressed()).thenReturn(true);
        for (int loop = 0; loop < 10; loop++) {
            sensor.Process();
        }
        verify(operator, times(1)).rumble(properties.pollenRumbleMs);
    }

    @Test
    @DisplayName("pollenRumbleMs 0: no rumble")
    void noRumbleWhenOff() {
        properties.pollenSensorInverted = false;
        properties.pollenRumbleMs = 0;
        PollenSensor sensor = sensorConfigured();
        when(touch.isPressed()).thenReturn(true);
        sensor.Process();
        verify(operator, never()).rumble(anyInt());
    }
}
