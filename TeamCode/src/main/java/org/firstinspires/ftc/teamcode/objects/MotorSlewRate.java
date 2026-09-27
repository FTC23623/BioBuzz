package org.firstinspires.ftc.teamcode.objects;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Changes a motor's power a little at a time instead of all at once.
 *
 * <p>Each call moves the power at most {@code slewRate} toward the target. Called once a
 * loop, a slew rate of 0.2 takes 5 loops (about 0.1 seconds) to go from stopped to full.
 * This keeps the robot from jerking and tipping when the driver slams the stick.
 */
public class MotorSlewRate {
    static public void Set(DcMotor motor, double power, double slewRate) {
        double currentPower = motor.getPower();
        double desiredChange = power - currentPower;
        double limitedChange = Math.max(-slewRate, Math.min(desiredChange, slewRate));
        motor.setPower(currentPower + limitedChange);
    }
}
