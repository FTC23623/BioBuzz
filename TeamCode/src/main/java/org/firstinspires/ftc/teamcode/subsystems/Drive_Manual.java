package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.MecanumMath;
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
    private final Debouncer mResetHeadingButton;

    // Read from the gamepad in HandleUserInput, used in Process.
    private double mDrive;
    private double mStrafe;
    private double mRotate;
    private double mSpeedLimit;

    public Drive_Manual(HydraOpMode op, Imu imu) {
        super(op, imu);
        mGamepad = mOp.mDriverGamepad;
        SetAllMotorPower(0.0);
        SetAllMotorMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        mResetHeadingButton = new Debouncer(Constants.debounce);
        mSpeedLimit = Constants.driveNormal;
    }

    @Override
    public void HandleUserInput() {
        mDrive = mGamepad.left_stick_y;
        mStrafe = -mGamepad.left_stick_x * Constants.driveStrafeGain;
        mRotate = -mGamepad.right_stick_x;

        // Circle resets which way is forward. Rumble so the driver feels it happen.
        mResetHeadingButton.In(mGamepad.circle);
        if (mResetHeadingButton.Out() && Constants.fieldCentricDrive) {
            mResetHeadingButton.Used();
            mGamepad.rumbleBlips(1);
            mImu.ResetYaw();
        }

        if (mGamepad.left_trigger > Constants.trgBtnThresh) {
            mSpeedLimit = Constants.driveSlow;
        } else if (mGamepad.right_trigger > Constants.trgBtnThresh) {
            mSpeedLimit = Constants.driveBoosted;
        } else {
            mSpeedLimit = Constants.driveNormal;
        }
    }

    @Override
    public void Process() {
        // No heading yet? Drive as if facing forward, like Decode did.
        double yaw = 0;
        if (mImu.Ready()) {
            yaw = mImu.GetYaw();
        }

        MecanumMath.WheelPowers powers;
        if (Constants.fieldCentricDrive) {
            powers = MecanumMath.FieldCentric(mDrive, mStrafe, mRotate, yaw);
        } else {
            powers = MecanumMath.RobotCentric(mDrive, mStrafe, mRotate);
        }

        SetMotorPower(mFrontLeftMotor, powers.frontLeft * mSpeedLimit);
        SetMotorPower(mFrontRightMotor, powers.frontRight * mSpeedLimit);
        SetMotorPower(mBackLeftMotor, powers.backLeft * mSpeedLimit);
        SetMotorPower(mBackRightMotor, powers.backRight * mSpeedLimit);

        if (mOp.mSafeMode) {
            mOp.mTelemetry.addLine("SAFE MODE: drive limited");
        }
        mOp.mTelemetry.addData("Yaw", yaw);
        mOp.mTelemetry.addData("Power FL", mFrontLeftMotor.getPower());
        mOp.mTelemetry.addData("Power FR", mFrontRightMotor.getPower());
        mOp.mTelemetry.addData("Power BL", mBackLeftMotor.getPower());
        mOp.mTelemetry.addData("Power BR", mBackRightMotor.getPower());
    }
}
