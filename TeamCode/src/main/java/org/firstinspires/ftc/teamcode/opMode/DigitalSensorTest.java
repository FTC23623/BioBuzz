package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.TouchSensor;

/**
 * Checks that the pollen and nectar switches are in the robot configuration and wired up.
 * Nothing moves: this only reads.
 *
 * <p>It looks for eight "REV Touch Sensor" entries: {@code pollenSensor1} to
 * {@code pollenSensor4} and {@code nectorSensor1} to {@code nectorSensor4}. For each one the
 * Driver Station shows:
 * <ul>
 *   <li>{@code OPEN}: found, and the switch is not pressed
 *   <li>{@code CLOSED}: found, and the switch is pressed
 *   <li>{@code NOT FOUND}: nothing by that name in the robot configuration (check the
 *       spelling, capitals too)
 * </ul>
 *
 * <p>Press each switch by hand. Its line, and only its line, should change. If a different
 * line changes, two names are swapped in the configuration.
 */
@TeleOp(name = "Test: Pollen and Nectar Sensors", group = "Test")
public class DigitalSensorTest extends LinearOpMode {
    /** The start of each pollen switch's name: pollenSensor1, pollenSensor2, ... */
    static final String POLLEN_NAME = "pollenSensor";
    /** The start of each nectar switch's name: nectorSensor1, nectorSensor2, ... */
    static final String NECTAR_NAME = "nectorSensor";
    /** How many switches of each kind. */
    static final int SENSOR_COUNT = 4;

    /** "pollenSensor" and 2 make "pollenSensor2". Numbers start at 1. */
    static String SensorName(String kind, int number) {
        return kind + number;
    }

    /**
     * What to show for one switch.
     *
     * <p>A touch sensor says "pressed" while its switch is closed, like
     * {@code subsystems/PollenSensor} reads it.
     *
     * @param sensor the switch, or null when it is not in the robot configuration
     */
    static String StateText(TouchSensor sensor) {
        if (sensor == null) {
            return "NOT FOUND";
        }
        if (sensor.isPressed()) {
            return "CLOSED";
        }
        return "OPEN";
    }

    /** Look up one kind's switches. A missing one is null. */
    private TouchSensor[] FindSensors(String kind) {
        TouchSensor[] sensors = new TouchSensor[SENSOR_COUNT];
        for (int i = 0; i < SENSOR_COUNT; i++) {
            sensors[i] = hardwareMap.tryGet(TouchSensor.class, SensorName(kind, i + 1));
        }
        return sensors;
    }

    private void ShowSensors(String kind, TouchSensor[] sensors) {
        for (int i = 0; i < SENSOR_COUNT; i++) {
            telemetry.addData(SensorName(kind, i + 1), StateText(sensors[i]));
        }
    }

    @Override
    public void runOpMode() {
        TouchSensor[] pollenSensors = FindSensors(POLLEN_NAME);
        TouchSensor[] nectorSensors = FindSensors(NECTAR_NAME);

        // The states show before and after START, so the check can happen either way.
        while (opModeInInit() || opModeIsActive()) {
            telemetry.addLine("Press each switch by hand. Only its line should change.");
            telemetry.addLine("NOT FOUND = no REV Touch Sensor by that name in the configuration.");
            telemetry.addLine();
            ShowSensors(POLLEN_NAME, pollenSensors);
            telemetry.addLine();
            ShowSensors(NECTAR_NAME, nectorSensors);
            telemetry.update();
            idle();
        }
    }
}
