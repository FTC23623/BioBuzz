package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.MecanumMath;
import org.firstinspires.ftc.teamcode.objects.MotorSlewRate;
import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * Driver control of the mecanum drive.
 *
 * <p>Controls (driver gamepad):
 * <ul>
 *   <li><b>Left stick</b>: drive and strafe.
 *   <li><b>Right stick X</b>: turn.
 *   <li><b>Left trigger</b>: slow mode. <b>Right trigger</b>: boost.
 *   <li><b>Circle / B</b>: "the way the robot faces now is forward" (field-centric only).
 * </ul>
 *
 * <p>Ported from Decode subsystems/Drive_Manual.java. The math is unchanged and lives in
 * {@link MecanumMath}. Vision aim (left bumper) and snap-to-heading (square) are left out
 * until vision is ported; see docs/decisions/0002-drive-port.md.
 */
public class Drive_Manual extends Drive {
    private final Gamepad mGamepad;
    private final Debouncer mCircle;
    private final MecanumMath.WheelPowers mPowers;

    // Read from the gamepad in HandleUserInput, used in Process.
    private double mDrive;
    private double mStrafe;
    private double mRotate;
    private double mMaxPower;

    public Drive_Manual(HydraOpMode op, Imu imu) {
        super(op, imu);
        mGamepad = mOp.mDriverGamepad;
        SetAllMotorPower(0.0);
        SetAllMotorMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        mCircle = new Debouncer(Constants.debounce);
        mPowers = new MecanumMath.WheelPowers();
        mMaxPower = Constants.driveNormal;
    }

    @Override
    public void HandleUserInput() {
        mDrive = mGamepad.left_stick_y;
        mStrafe = -mGamepad.left_stick_x * Constants.driveStrafeGain;
        mRotate = -mGamepad.right_stick_x;

        // Circle resets which way is forward. Rumble so the driver feels it happen.
        mCircle.In(mGamepad.circle);
        if (mCircle.Out() && Constants.fieldCentricDrive) {
            mCircle.Used();
            mGamepad.rumbleBlips(1);
            mImu.ResetYaw();
        }

        if (mGamepad.left_trigger > Constants.trgBtnThresh) {
            mMaxPower = Constants.driveSlow;
        } else if (mGamepad.right_trigger > Constants.trgBtnThresh) {
            mMaxPower = Constants.driveBoosted;
        } else {
            mMaxPower = Constants.driveNormal;
        }
    }

    @Override
    public void Process() {
        // No heading yet? Drive as if facing forward, like Decode did.
        double yaw = mImu.Ready() ? mImu.GetYaw() : 0;
        if (Constants.fieldCentricDrive) {
            MecanumMath.FieldCentric(mDrive, mStrafe, mRotate, yaw, mPowers);
        } else {
            MecanumMath.RobotCentric(mDrive, mStrafe, mRotate, mPowers);
        }
        MotorSlewRate.Set(mMotDrBkLt, mPowers.backLeft * mMaxPower, Constants.driveMotorSlewRate);
        MotorSlewRate.Set(mMotDrBkRt, mPowers.backRight * mMaxPower, Constants.driveMotorSlewRate);
        MotorSlewRate.Set(mMotDrFrLt, mPowers.frontLeft * mMaxPower, Constants.driveMotorSlewRate);
        MotorSlewRate.Set(mMotDrFrRt, mPowers.frontRight * mMaxPower, Constants.driveMotorSlewRate);

        mOp.mTelemetry.addData("Yaw", yaw);
        mOp.mTelemetry.addData("Power FL", mPowers.frontLeft * mMaxPower);
        mOp.mTelemetry.addData("Power FR", mPowers.frontRight * mMaxPower);
        mOp.mTelemetry.addData("Power BL", mPowers.backLeft * mMaxPower);
        mOp.mTelemetry.addData("Power BR", mPowers.backRight * mMaxPower);
    }
}
