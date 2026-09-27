package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.firstinspires.ftc.teamcode.objects.DrivePowerLimit;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.MotorSlewRate;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * The four mecanum drive motors. Sets them up; does not decide how to move.
 * {@link Drive_Manual} adds driver control on top.
 *
 * <p>Ported from Decode subsystems/Drive.java. Left out on purpose: the {@code if (false)}
 * PID block (PORT-PLAN P6 cut list), the encoder ramp fields nothing read, and the
 * RUN_TO_POSITION tolerance (the drive never runs in that mode).
 */
public class Drive implements Subsystem {
    protected final HydraOpMode mOp;
    protected final Imu mImu;
    protected final DcMotorEx mMotDrFrLt;
    protected final DcMotorEx mMotDrFrRt;
    protected final DcMotorEx mMotDrBkLt;
    protected final DcMotorEx mMotDrBkRt;

    public Drive(HydraOpMode op, Imu imu) {
        mOp = op;
        mImu = imu;
        mMotDrFrLt = mOp.mHardwareMap.get(DcMotorEx.class, Constants.cfgDriveFrontLeft);
        mMotDrFrRt = mOp.mHardwareMap.get(DcMotorEx.class, Constants.cfgDriveFrontRight);
        mMotDrBkLt = mOp.mHardwareMap.get(DcMotorEx.class, Constants.cfgDriveBackLeft);
        mMotDrBkRt = mOp.mHardwareMap.get(DcMotorEx.class, Constants.cfgDriveBackRight);
        // Left side forward, right side reverse. MecanumMath's signs depend on this.
        mMotDrFrLt.setDirection(DcMotor.Direction.FORWARD);
        mMotDrBkLt.setDirection(DcMotor.Direction.FORWARD);
        mMotDrFrRt.setDirection(DcMotor.Direction.REVERSE);
        mMotDrBkRt.setDirection(DcMotor.Direction.REVERSE);
        SetAllMotorMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        // Hold still instead of coasting when the stick is released.
        mMotDrFrLt.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        mMotDrBkLt.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        mMotDrFrRt.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        mMotDrBkRt.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    @Override
    public boolean Init() {
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    @Override
    public void Process() {
    }

    /** Set the same power on all four motors. 0 stops the robot. Safe Mode applies. */
    protected void SetAllMotorPower(double value) {
        double limited = DrivePowerLimit.Apply(value, mOp.mSafeMode);
        mMotDrBkLt.setPower(limited);
        mMotDrBkRt.setPower(limited);
        mMotDrFrLt.setPower(limited);
        mMotDrFrRt.setPower(limited);
    }

    /**
     * Move one drive motor toward a power, no faster than the slew rate. Safe Mode applies.
     * Every drive power in TeleOp and Auto must go through here or SetAllMotorPower.
     */
    protected void SetMotorPower(DcMotorEx motor, double power) {
        MotorSlewRate.Set(motor, DrivePowerLimit.Apply(power, mOp.mSafeMode),
                Constants.driveMotorSlewRate);
    }

    /** Set the run mode on all four motors. */
    protected void SetAllMotorMode(DcMotor.RunMode value) {
        mMotDrBkLt.setMode(value);
        mMotDrBkRt.setMode(value);
        mMotDrFrLt.setMode(value);
        mMotDrFrRt.setMode(value);
    }
}
