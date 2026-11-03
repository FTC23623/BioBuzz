package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.types.Alliance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The alliance magnet with a pretend sensor. */
@DisplayName("AllianceSensor")
class AllianceSensorTest {
    private final BotProperties properties = new BotProperties();
    private HardwareMap hardwareMap;
    private TouchSensor magnet;
    private HydraOpMode op;

    @BeforeEach
    void setUp() {
        magnet = mock(TouchSensor.class);
        hardwareMap = mock(HardwareMap.class);
        op = new HydraOpMode(mock(Telemetry.class), hardwareMap, mock(Gamepad.class), mock(Gamepad.class));
    }

    private void sensorInConfig() {
        when(hardwareMap.tryGet(TouchSensor.class, properties.cfgAllianceSensor)).thenReturn(magnet);
    }

    @Test
    @DisplayName("magnet sensed: RED")
    void magnetIsRed() {
        sensorInConfig();
        when(magnet.isPressed()).thenReturn(true);
        new AllianceSensor(op, properties);
        assertEquals(Alliance.RED, op.mAlliance);
    }

    @Test
    @DisplayName("no magnet: BLUE")
    void noMagnetIsBlue() {
        sensorInConfig();
        when(magnet.isPressed()).thenReturn(false);
        new AllianceSensor(op, properties);
        assertEquals(Alliance.BLUE, op.mAlliance);
    }

    @Test
    @DisplayName("no sensor in the robot configuration: BLUE")
    void noSensorIsBlue() {
        new AllianceSensor(op, properties);
        assertEquals(Alliance.BLUE, op.mAlliance);
    }

    @Test
    @DisplayName("before START, moving the magnet changes the alliance")
    void rereadBeforeStart() {
        sensorInConfig();
        when(magnet.isPressed()).thenReturn(false);
        AllianceSensor sensor = new AllianceSensor(op, properties);
        when(magnet.isPressed()).thenReturn(true);
        sensor.ShowStatus();
        assertEquals(Alliance.RED, op.mAlliance);
    }

    @Test
    @DisplayName("after START, Process never reads the sensor: a bump can't change sides")
    void fixedAfterStart() {
        sensorInConfig();
        when(magnet.isPressed()).thenReturn(false);
        AllianceSensor sensor = new AllianceSensor(op, properties);
        when(magnet.isPressed()).thenReturn(true);
        sensor.HandleUserInput();
        sensor.Process();
        assertEquals(Alliance.BLUE, op.mAlliance);
    }

    @Test
    @DisplayName("a backup auto's override wins, and the sensor is never looked up")
    void overrideWins() {
        properties.allianceOverride = Alliance.RED;
        new AllianceSensor(op, properties);
        assertEquals(Alliance.RED, op.mAlliance);
        verify(hardwareMap, never()).tryGet(TouchSensor.class, properties.cfgAllianceSensor);
    }

    @Test
    @DisplayName("each alliance aims only at its own hive's tags (manual section 9.9)")
    void hiveTags() {
        assertEquals(30, properties.HiveTags(Alliance.RED)[0]);
        assertEquals(37, properties.HiveTags(Alliance.RED)[1]);
        assertEquals(38, properties.HiveTags(Alliance.BLUE)[0]);
        assertEquals(45, properties.HiveTags(Alliance.BLUE)[1]);
        assertEquals(properties.anyTag, properties.HiveTags(Alliance.UNKNOWN));
    }
}
