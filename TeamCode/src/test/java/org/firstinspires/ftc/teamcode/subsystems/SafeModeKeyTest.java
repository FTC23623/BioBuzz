package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
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

/**
 * Key in = Safe Mode. Key removed = full speed ("Remove Before Flight").
 *
 * <p>These tests talk about "key in" and "key removed", not "pressed". So they stay correct
 * when BotProperties.keyPressedMeansKeyIn changes from the touch stand-in to the magnetic key.
 */
@DisplayName("SafeModeKey")
class SafeModeKeyTest {
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private HydraOpMode op;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        op = new HydraOpMode(telemetry, hardwareMap, mock(Gamepad.class), mock(Gamepad.class));
    }

    /** What the configured sensor reads when the key is in or out. */
    private boolean sensorReading(boolean keyIn) {
        if (BotProperties.keyPressedMeansKeyIn) {
            return keyIn;
        }
        return !keyIn;
    }

    private TouchSensor keySensor(boolean keyIn) {
        TouchSensor sensor = mock(TouchSensor.class);
        when(sensor.isPressed()).thenReturn(sensorReading(keyIn));
        when(hardwareMap.tryGet(TouchSensor.class, BotProperties.cfgSafeModeKey)).thenReturn(sensor);
        return sensor;
    }

    @Test
    @DisplayName("every OpMode starts in Safe Mode, before the key is checked")
    void defaultIsSafe() {
        assertTrue(op.mSafeMode);
    }

    @Test
    @DisplayName("key in: SAFE (touch sensor NOT pressed)")
    void keyInIsSafe() {
        op.mSafeMode = false;
        keySensor(true);
        new SafeModeKey(op).ShowStatus();
        assertTrue(op.mSafeMode);
        verify(telemetry).addLine(contains("key is in"));
    }

    @Test
    @DisplayName("key removed: FULL speed (touch sensor pressed)")
    void keyRemovedIsFull() {
        keySensor(false);
        new SafeModeKey(op).ShowStatus();
        assertFalse(op.mSafeMode);
        verify(telemetry).addLine(contains("FULL SPEED"));
    }

    @Test
    @DisplayName("sensor missing from the robot configuration: SAFE, with a warning")
    void missingIsSafe() {
        op.mSafeMode = false;
        new SafeModeKey(op).ShowStatus();
        assertTrue(op.mSafeMode);
        verify(telemetry).addLine(contains("no REV Touch Sensor named '" + BotProperties.cfgSafeModeKey));
    }

    @Test
    @DisplayName("the key is read exactly once, at INIT, and a mid-match change is ignored")
    void readOnlyOnce() {
        TouchSensor sensor = keySensor(false);
        SafeModeKey key = new SafeModeKey(op);
        assertFalse(op.mSafeMode);

        // A fluke mid-match: the sensor suddenly says "key in".
        when(sensor.isPressed()).thenReturn(sensorReading(true));
        for (int loop = 0; loop < 100; loop++) {
            key.Init();
            key.ShowStatus();
            key.HandleUserInput();
            key.Process();
        }

        assertFalse(op.mSafeMode, "speed must not change after INIT");
        verify(sensor, times(1)).isPressed();
    }

    @Test
    @DisplayName("touch stand-in: pressed means key REMOVED, not pressed means key IN")
    void touchStandIn() {
        assertFalse(SafeModeKey.KeyIsIn(true, false));
        assertTrue(SafeModeKey.KeyIsIn(false, false));
    }

    @Test
    @DisplayName("magnetic key: pressed (magnet near) means key IN")
    void magneticKey() {
        assertTrue(SafeModeKey.KeyIsIn(true, true));
        assertFalse(SafeModeKey.KeyIsIn(false, true));
    }
}
