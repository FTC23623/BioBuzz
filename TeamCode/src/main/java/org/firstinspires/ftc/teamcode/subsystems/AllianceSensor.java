package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.TouchSensor;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * Works out which alliance we are on and puts it in {@code HydraOpMode.mAlliance}.
 *
 * <ul>
 *   <li><b>Magnet near the sensor</b>: RED.
 *   <li><b>No magnet</b>, or no sensor in the robot configuration: BLUE.
 * </ul>
 *
 * <p>Read over and over while waiting for START, so the drive team can move the magnet and
 * watch the LED strip change color. After START it is never read again: a bump in the
 * middle of a match can't change sides.
 *
 * <p>The backup autos skip the sensor: they set {@code allianceOverride} in BotProperties
 * to RED or BLUE. See docs/decisions/0021.
 *
 * <p>Hardware: a REV Magnetic Limit Switch, configured as a "REV Touch Sensor". It reads
 * "pressed" while a magnet is near.
 */
public class AllianceSensor implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    /** null when the sensor is not in the robot configuration, or an override is set. */
    private final TouchSensor mSensor;
    /** Where the answer came from, for the Driver Station. */
    private String mSource = "";

    public AllianceSensor(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
        if (properties.allianceOverride != Alliance.UNKNOWN) {
            mSensor = null;
        } else {
            mSensor = op.mHardwareMap.tryGet(TouchSensor.class, properties.cfgAllianceSensor);
        }
        Read();
    }

    /** Read the sensor (or the override) and set the alliance. Only call before START. */
    public void Read() {
        if (mProperties.allianceOverride != Alliance.UNKNOWN) {
            mOp.mAlliance = mProperties.allianceOverride;
            mSource = "set by this OpMode, sensor not read";
        } else if (mSensor == null) {
            mOp.mAlliance = Alliance.BLUE;
            mSource = "NO SENSOR named '" + mProperties.cfgAllianceSensor + "', so blue";
        } else {
            boolean magnetSensed = mSensor.isPressed();
            mOp.mAlliance = FromMagnet(magnetSensed);
            if (magnetSensed) {
                mSource = "magnet sensed";
            } else {
                mSource = "no magnet";
            }
        }
    }

    /** Magnet near means red. No magnet means blue. */
    static Alliance FromMagnet(boolean magnetSensed) {
        if (magnetSensed) {
            return Alliance.RED;
        }
        return Alliance.BLUE;
    }

    /** Read again and show the answer. The Bot calls this every loop while waiting for START. */
    public void ShowStatus() {
        Read();
        mOp.mTelemetry.addData("ALLIANCE", "%s (%s)", mOp.mAlliance, mSource);
    }

    @Override
    public boolean Init() {
        ShowStatus();
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Nothing. After START the alliance is fixed. */
    @Override
    public void Process() {
    }
}
