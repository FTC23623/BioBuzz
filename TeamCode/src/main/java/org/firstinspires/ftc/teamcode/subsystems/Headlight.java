package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * A light on the front of the robot. It lights up the field so the Limelight can see
 * AprilTags better. It turns on at INIT and stays on.
 *
 * <p>Like the indicator light, it plugs into a servo port, so "on" is a servo position.
 * A robot with no headlight configured still runs: the Driver Station says so at INIT and
 * this part does nothing.
 */
public class Headlight implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    /** null when no headlight is in the robot configuration. */
    private final Servo mLight;

    public Headlight(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
        mLight = op.mHardwareMap.tryGet(Servo.class, properties.cfgHeadlight);
    }

    /** Turn the light on. */
    @Override
    public boolean Init() {
        if (mLight == null) {
            mOp.mTelemetry.addLine("No headlight: nothing named '" + mProperties.cfgHeadlight
                    + "' in the robot configuration (a Servo).");
        } else {
            mLight.setPosition(mProperties.headlightOn);
        }
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Nothing to do each loop: the light just stays on. */
    @Override
    public void Process() {
    }
}
