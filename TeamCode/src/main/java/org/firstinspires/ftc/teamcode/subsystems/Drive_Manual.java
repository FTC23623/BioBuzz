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
 *   <li><b>Left bumper (hold)</b>: slow mode, for lining up. Above the left trigger, which
 *       is drift: both are "drive carefully" buttons.
 *   <li><b>Left trigger (hold)</b>: drift assist. Turning pivots on the inside back wheel
 *       instead of the middle, like a car drifting.
 *   <li><b>Circle / B</b>: "the way the robot faces now is forward" (field-centric only).
 *   <li><b>Right bumper (hold)</b>: aim assist. The robot turns to face the MIDDLE of our
 *       hive's AprilTags (the camera ignores the other alliance's), the same spot the autos
 *       aim at. The sticks still drive and strafe. Let go to turn by hand again. Above the
 *       right trigger, which launches: both are "at the hive" buttons.
 *   <li><b>Right trigger (hold)</b>: launch (see LaunchGates), and aim the same way while it
 *       is held ({@code launchTriggerAims}), so a launch snaps to the hive.
 *   <li><b>D-pad left / right (hold, while aiming)</b>: arc around the hive. The robot
 *       strafes that way at {@code arcStrafePower}, aim keeps the nose on the hive, and a
 *       little drive holds the distance the arc started at ({@code arcRangeKP}). With the
 *       aim button up, or no tag in view, the D-pad does nothing (docs/decisions/0039).
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
 * {@link MecanumMath}. Aim assist is Decode's bumper idea, done over: Decode's was
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
    /** Which way the D-pad asks to arc: 1 left, -1 right, 0 neither (or both). */
    private int mArcDirection;
    /** Inches to the tag when the arc started, to hold. 0 while not arcing. */
    private double mArcRangeIn;
    /**
     * true once HandleUserInput has read the gamepad this loop. Process clears it. A loop
     * with no fresh reading never aims, so a button seen once can't keep aiming (0040).
     */
    private boolean mInputFresh = false;

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
        // Aim on the right bumper, and on the launch trigger too, so a launch snaps to the hive.
        mAimHeld = AimButtonDown();
        mInputFresh = true;
        // Drift only while the trigger is held, and only in an OpMode that has it on.
        mDriftHeld = mProperties.driveDriftAssist && mGamepad.left_trigger > mProperties.trgBtnThresh;
        // D-pad left or right: arc that way around the hive, but only while aiming (below).
        // Left is positive strafe, like the stick. Both pressed at once means neither.
        mArcDirection = 0;
        if (mGamepad.dpad_left && !mGamepad.dpad_right) {
            mArcDirection = 1;
        } else if (mGamepad.dpad_right && !mGamepad.dpad_left) {
            mArcDirection = -1;
        }

        // Circle resets which way is forward. Rumble so the driver feels it happen.
        mResetHeadingButton.In(mGamepad.circle);
        if (mResetHeadingButton.Out() && mProperties.fieldCentricDrive) {
            mResetHeadingButton.Used();
            mGamepad.rumbleBlips(1);
            mImu.ResetYaw();
        }

        // Left bumper: slow mode while held.
        if (mGamepad.left_bumper) {
            mSpeedLimit = mProperties.driveSlow;
        } else {
            mSpeedLimit = mProperties.driveNormal;
        }
    }

    /** true while the right bumper is down, or the launch trigger is pulled (with launchTriggerAims). */
    private boolean AimButtonDown() {
        boolean launchHeld = mGamepad.right_trigger > mProperties.trgBtnThresh;
        return mGamepad.right_bumper || (mProperties.launchTriggerAims && launchHeld);
    }

    @Override
    public void Process() {
        // No heading yet? Drive as if facing forward, like Decode did.
        double yaw = 0;
        if (mImu.Ready()) {
            yaw = mImu.GetYaw();
        }

        // Aim only while a button is down RIGHT NOW. Two guards: no fresh gamepad reading
        // this loop means no aim, and the buttons are checked again here, so nothing
        // remembered from an earlier loop can keep the robot turning (coach, 2026-11-03).
        if (!mInputFresh) {
            // No reading this loop: the turn left over from last loop may be an aim turn, so
            // nothing old is driven. The robot coasts down to still through the slew rate.
            mDrive = 0;
            mStrafe = 0;
            mRotate = 0;
        }
        if (!mInputFresh || !AimButtonDown()) {
            mAimHeld = false;
            mArcDirection = 0;
        }
        mInputFresh = false;

        // Aim assist: the middle of the hive's tag group decides the turn, not one tag, so the
        // robot faces the hive itself. The sticks still drive and strafe.
        boolean aimingAtHive = mAimHeld && mVision.SeesTag();
        if (aimingAtHive) {
            double hiveBearing = mVision.GetGroupBearing();
            // The pollen launcher sits left of the camera, so aim the hive a little left of
            // center (aimHiveBearingDeg), not dead center.
            double aimError = AimAssist.AimError(hiveBearing, mProperties.aimHiveBearingDeg);
            mRotate = AimAssist.RotateToward(aimError, mProperties.aimKP,
                    mProperties.aimMaxRotate, mProperties.aimDoneDegrees);
            if (!mOp.mQuiet) {
                mOp.mTelemetry.addData("Aim assist", "ON: hive middle %.1f deg (tag %d, %.0f in), turn %.2f",
                        hiveBearing, mVision.GetTagId(), mVision.GetRange(), mRotate);
            }
        } else if (mAimHeld && !mOp.mQuiet) {
            mOp.mTelemetry.addLine("Aim assist: held, but no tag in view");
        } else if (!mOp.mQuiet) {
            mOp.mTelemetry.addLine("Aim assist: off");
        }
        // The raw buttons, so a trigger that rests a little pressed shows up on the screen.
        if (!mOp.mQuiet) {
            mOp.mTelemetry.addData("Aim buttons", "right bumper %s, launch trigger %.2f (counts above %.2f)",
                    mGamepad.right_bumper, mGamepad.right_trigger, mProperties.trgBtnThresh);
        }

        // D-pad while aiming: arc around the hive. Strafe the way the D-pad points, and hold
        // the distance the arc started at with a little drive. Aim above keeps the nose on
        // the hive, so the three together walk the robot around it in a circle.
        double arcDrive = 0;
        double arcStrafe = 0;
        if (aimingAtHive && mArcDirection != 0) {
            double rangeIn = mVision.GetRange();
            if (mArcRangeIn <= 0) {
                // First loop of this arc: this is the distance to keep.
                mArcRangeIn = rangeIn;
            }
            arcStrafe = mArcDirection * mProperties.arcStrafePower;
            arcDrive = AimAssist.HoldRange(rangeIn, mArcRangeIn, mProperties.arcRangeKP,
                    mProperties.arcMaxDrive);
            if (!mOp.mQuiet) {
                mOp.mTelemetry.addData("Arc", "holding %.0f in, now %.0f in, drive %.2f",
                        mArcRangeIn, rangeIn, arcDrive);
            }
        } else {
            // Not arcing: the next arc measures its own distance when it starts.
            mArcRangeIn = 0;
        }

        // Left trigger: drift assist. Turn around the inside back corner, not the middle.
        double driftDrive = 0;
        double driftStrafe = 0;
        if (mDriftHeld) {
            driftDrive = MecanumMath.DriftDrive(mRotate, mProperties.driveDriftSideGain);
            driftStrafe = MecanumMath.DriftStrafe(mRotate, mProperties.driveDriftRearGain);
        }

        // The drift pivot and the arc are spots on the robot and the hive, not on the field,
        // so both stay robot-centric whichever way the sticks are read.
        double robotDrive = driftDrive + arcDrive;
        double robotStrafe = driftStrafe + arcStrafe;
        MecanumMath.WheelPowers powers;
        if (mProperties.fieldCentricDrive) {
            powers = MecanumMath.FieldCentric(mDrive, mStrafe, mRotate, yaw, robotDrive, robotStrafe);
        } else {
            powers = MecanumMath.RobotCentric(mDrive + robotDrive, mStrafe + robotStrafe, mRotate);
        }

        SetMotorPower(mFrontLeftMotor, powers.frontLeft * mSpeedLimit);
        SetMotorPower(mFrontRightMotor, powers.frontRight * mSpeedLimit);
        SetMotorPower(mBackLeftMotor, powers.backLeft * mSpeedLimit);
        SetMotorPower(mBackRightMotor, powers.backRight * mSpeedLimit);

        if (mOp.mQuiet) {
            return;
        }
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
