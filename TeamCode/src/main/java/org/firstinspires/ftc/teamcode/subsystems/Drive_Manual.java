package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.AimAssist;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.MecanumMath;

/**
 * Driver control of the mecanum drive.
 *
 * <p>Controls (driver gamepad):
 * <ul>
 *   <li><b>Left stick</b>: drive and strafe. Field-centric in HyDrive: "forward" is away
 *       from the driver, whichever way the robot faces.
 *   <li><b>Right stick X</b>: turn.
 *   <li><b>Right bumper (hold)</b>: slow mode, for lining up.
 *   <li><b>Left trigger (hold)</b>: drift assist. Turning pivots on the inside back wheel
 *       instead of the middle, like a car drifting.
 *   <li><b>Circle / B</b>: "the way the robot faces now is forward" (field-centric only).
 *   <li><b>Left bumper (hold)</b>: aim assist. The robot turns to face the MIDDLE of our
 *       hive's AprilTags (the camera ignores the other alliance's), the same spot the autos
 *       aim at. The sticks still drive and strafe. Let go to turn by hand again.
 *   <li><b>Right trigger (hold)</b>: launch (see LaunchGates), and aim the same way while it
 *       is held ({@code launchTriggerAims}), so a launch snaps to the hive.
 * </ul>
 *
 * <p>Practice options, off unless an OpMode turns them on in BotProperties (from Decode's
 * DrivePractice, docs/decisions/0018):
 * <ul>
 *   <li><b>driveSwapStrafeRotate</b>: left stick X turns, right stick X strafes.
 *   <li><b>driveTightTurns</b>: the turn stick is stronger, so turning wins over driving.
 *   <li><b>driveDriftAssist</b>: hold the <b>left trigger</b> while turning to pivot on the
 *       inside back corner instead of the middle. On in HyDrive too (docs/decisions/0034).
 * </ul>
 *
 * <p>Ported from Decode subsystems/Drive_Manual.java. The math is unchanged and lives in
 * {@link MecanumMath}. Aim assist is Decode's left-bumper idea, done over: Decode's was
 * overwritten by the next line every loop. Snap-to-heading (square) is still left out;
 * see docs/decisions/0002-drive-port.md and 0011.
 */
public class Drive_Manual extends Drive {
    private final Gamepad mGamepad;
    private final Vision mVision;
    private final Debouncer mResetHeadingButton;

    // Read from the gamepad in HandleUserInput, used in Process.
    private double mDrive;
    private double mStrafe;
    private double mRotate;
    private double mSpeedLimit;
    private boolean mAimHeld;
    private boolean mDriftHeld;

    public Drive_Manual(HydraOpMode op, BotProperties properties, Imu imu, Vision vision) {
        super(op, properties, imu);
        mGamepad = mOp.mDriverGamepad;
        mVision = vision;
        SetAllMotorPower(0.0);
        SetAllMotorMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        mResetHeadingButton = new Debouncer(mProperties.debounce);
        mSpeedLimit = mProperties.driveNormal;
    }

    @Override
    public void HandleUserInput() {
        double strafeStick = mGamepad.left_stick_x;
        double turnStick = mGamepad.right_stick_x;
        if (mProperties.driveSwapStrafeRotate) {
            // Swapped: the left stick drives and turns, the right stick strafes.
            strafeStick = mGamepad.right_stick_x;
            turnStick = mGamepad.left_stick_x * mProperties.driveSwappedTurnGain;
        } else if (mProperties.driveTightTurns) {
            turnStick = turnStick * mProperties.driveTightTurnGain;
        }
        mDrive = mGamepad.left_stick_y;
        mStrafe = -strafeStick * mProperties.driveStrafeGain;
        mRotate = -turnStick;
        // Aim on the left bumper, and on the launch trigger too, so a launch snaps to the hive.
        boolean launchHeld = mGamepad.right_trigger > mProperties.trgBtnThresh;
        mAimHeld = mGamepad.left_bumper || (mProperties.launchTriggerAims && launchHeld);
        // Drift only while the trigger is held, and only in an OpMode that has it on.
        mDriftHeld = mProperties.driveDriftAssist && mGamepad.left_trigger > mProperties.trgBtnThresh;

        // Circle resets which way is forward. Rumble so the driver feels it happen.
        mResetHeadingButton.In(mGamepad.circle);
        if (mResetHeadingButton.Out() && mProperties.fieldCentricDrive) {
            mResetHeadingButton.Used();
            mGamepad.rumbleBlips(1);
            mImu.ResetYaw();
        }

        // Right bumper: slow mode while held.
        if (mGamepad.right_bumper) {
            mSpeedLimit = mProperties.driveSlow;
        } else {
            mSpeedLimit = mProperties.driveNormal;
        }
    }

    @Override
    public void Process() {
        // No heading yet? Drive as if facing forward, like Decode did.
        double yaw = 0;
        if (mImu.Ready()) {
            yaw = mImu.GetYaw();
        }

        // Aim assist: the middle of the hive's tag group decides the turn, not one tag, so the
        // robot faces the hive itself. The sticks still drive and strafe.
        if (mAimHeld && mVision.SeesTag()) {
            double hiveBearing = mVision.GetGroupBearing();
            mRotate = AimAssist.RotateToward(hiveBearing, mProperties.aimKP,
                    mProperties.aimMaxRotate, mProperties.aimDoneDegrees);
            mOp.mTelemetry.addData("Aim assist", "ON: hive middle %.1f deg (tag %d, %.0f in), turn %.2f",
                    hiveBearing, mVision.GetTagId(), mVision.GetRange(), mRotate);
        } else if (mAimHeld) {
            mOp.mTelemetry.addLine("Aim assist: held, but no tag in view");
        }

        // Left trigger: drift assist. Turn around the inside back corner, not the middle.
        double driftDrive = 0;
        double driftStrafe = 0;
        if (mDriftHeld) {
            driftDrive = MecanumMath.DriftDrive(mRotate, mProperties.driveDriftSideGain);
            driftStrafe = MecanumMath.DriftStrafe(mRotate, mProperties.driveDriftRearGain);
        }

        MecanumMath.WheelPowers powers;
        if (mProperties.fieldCentricDrive) {
            // The pivot is a spot on the robot, so the drift stays robot-centric.
            powers = MecanumMath.FieldCentric(mDrive, mStrafe, mRotate, yaw, driftDrive, driftStrafe);
        } else {
            powers = MecanumMath.RobotCentric(mDrive + driftDrive, mStrafe + driftStrafe, mRotate);
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
