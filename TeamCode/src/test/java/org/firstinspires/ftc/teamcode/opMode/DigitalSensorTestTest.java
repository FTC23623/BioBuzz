package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.TouchSensor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** What the sensor test shows for each switch. */
@DisplayName("Test: Pollen and Nectar Sensors")
class DigitalSensorTestTest {

    @Test
    @DisplayName("a switch that is not in the configuration shows NOT FOUND")
    void notFound() {
        assertEquals("NOT FOUND", DigitalSensorTest.StateText(null));
    }

    @Test
    @DisplayName("a switch that is not pressed shows OPEN, a pressed one shows CLOSED")
    void openAndClosed() {
        TouchSensor sensor = mock(TouchSensor.class);
        when(sensor.isPressed()).thenReturn(false);
        assertEquals("OPEN", DigitalSensorTest.StateText(sensor));
        when(sensor.isPressed()).thenReturn(true);
        assertEquals("CLOSED", DigitalSensorTest.StateText(sensor));
    }

    @Test
    @DisplayName("the names are pollenSensor1 to 4 and nectorSensor1 to 4")
    void names() {
        assertEquals(4, DigitalSensorTest.SENSOR_COUNT);
        assertEquals("pollenSensor1", DigitalSensorTest.SensorName(DigitalSensorTest.POLLEN_NAME, 1));
        assertEquals("pollenSensor4", DigitalSensorTest.SensorName(DigitalSensorTest.POLLEN_NAME, 4));
        assertEquals("nectorSensor1", DigitalSensorTest.SensorName(DigitalSensorTest.NECTAR_NAME, 1));
        assertEquals("nectorSensor4", DigitalSensorTest.SensorName(DigitalSensorTest.NECTAR_NAME, 4));
    }
}
