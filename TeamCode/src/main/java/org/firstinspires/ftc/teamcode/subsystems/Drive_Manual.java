package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;
import org.firstinspires.ftc.teamcode.objects.AimAssist;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HoldPosition;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.MecanumMath;
import org.firstinspires.ftc.teamcode.objects.Wiggle;

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
 *   <li><b>D-pad left / right (hold, while the operator aims, with {@code aimArc} on; off by default)</b>:
 *       arc around the hive. The robot
 *       strafes that way at {@code arcStrafePower}, aim keeps the nose on the hive, and a
 *       little drive holds the distance the arc started at ({@code arcRangeKP}). With the
 *       aim button up, or no tag in view, the D-pad does nothing (docs/decisions/0039).
 *   <li><b>Square / X (press)</b>: launch wiggle on / off ({@code launchWiggle}, off at
 *       START). On, every launch wiggles: once aim has settled (or with no tag in view) the
 *       robot swings {@code wiggleDeg} left, then right, a side per ball, so balls don't run
 *       into each other, until the launch trigger is let go (docs/decisions/0045).
 * </ul>
 *
 * <p>Aim and launch belong to the OPERATOR (gamepad 2, {@link LaunchChoice}): left bumper
 * aims pollen, right bumper aims nectar, and each launch trigger aims for its launcher too
 * ({@code launchTriggerAims}). Aiming turns the robot to face the MIDDLE of our hive's
 * AprilTags, offset for the side the robot stands on and the launcher used
 * ({@link LaunchChoice#AimAtDeg}: pollen and nectar sit either side of the camera). Both at once: pollen wins. The driver's sticks still drive and
 * strafe while it aims. While a launch trigger is held, with {@code launchHoldsPosition} on
 * and a Pinpoint, the robot also holds its spot: bumped, it pushes back. Moving the drive or
 * strafe stick wins over the hold.
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
    /** The operator: aim and launch are theirs. */
    private final Gamepad mOperator;
    private final Vision mVision;
    private final Debouncer mResetHeadingButton;
    /** The driver's sticks as the gamepad sent them this loop, for the dashboard. */
    private double mRawLeftX = 0;
    private double mRawLeftY = 0;
    private double mRawRightX = 0;
    private double mRawRightY = 0;

    // Read from the gamepad in HandleUserInput, used in Process.
    private double mDrive;
    private double mStrafe;
    private double mRotate;
    private double mSpeedLimit;
    private boolean mAimHeld;
    /** Which launcher the operator is aiming: its side of the hive's middle. */
    private LaunchChoice mAimChoice = LaunchChoice.NONE;
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
    /** true while the launch trigger is held with launchWiggle on. */
    private boolean mWiggleHeld = false;
    /** Square: launch wiggle on / off. */
    private final Debouncer mWiggleButton;
    /** true once a wiggle has started: its center heading and its timer are set. */
    private boolean mWiggling = false;
    /** Which way the robot faced when the wiggle started, degrees. */
    private double mWiggleCenterYaw = 0;
    private final ElapsedTime mWiggleTimer = new ElapsedTime();
    /** true while the operator holds a launch trigger with launchHoldsPosition on. */
    private boolean mHoldHeld = false;
    /** The spot the launch hold keeps the robot on, inches. NaN = none yet. */
    private double mHoldX = Double.NaN;
    private double mHoldY = Double.NaN;
    /** true once the robot was shoved past launchHoldGiveUpIn: no hold until the trigger is let go. */
    private boolean mHoldGaveUp = false;

    public Drive_Manual(HydraOpMode op, BotProperties properties, Imu imu, Vision vision) {
        super(op, properties, imu);
        mGamepad = mOp.mDriverGamepad;
        mOperator = mOp.mOperatorGamepad;
        mVision = vision;
        SetAllMotorPower(0.0);
        SetAllMotorMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        mResetHeadingButton = new Debouncer(mProperties.debounce);
        mWiggleButton = new Debouncer(mProperties.debounce);
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
        // The sticks just as the gamepad sent them, for the dashboard graphs in Process().
        mRawLeftX = mGamepad.left_stick_x;
        mRawLeftY = mGamepad.left_stick_y;
        mRawRightX = mGamepad.right_stick_x;
        mRawRightY = mGamepad.right_stick_y;
        mStrafe = -strafeStick * mProperties.driveStrafeGain;
        mRotate = -turnStick;
        // Aim is the operator's: a bumper, or a launch trigger too, so a launch snaps to the hive.
        mAimChoice = LaunchChoice.Aiming(mOperator, mProperties);
        mAimHeld = mAimChoice != LaunchChoice.NONE;
        // Square: launch wiggle on / off. One rumble on, two off.
        mWiggleButton.In(mGamepad.square);
        if (mWiggleButton.Out()) {
            mWiggleButton.Used();
            mProperties.launchWiggle = !mProperties.launchWiggle;
            if (mProperties.launchWiggle) {
                mGamepad.rumbleBlips(1);
            } else {
                mGamepad.rumbleBlips(2);
            }
        }
        boolean launching = LaunchChoice.Launching(mOperator, mProperties) != LaunchChoice.NONE;
        mWiggleHeld = mProperties.launchWiggle && launching;
        mHoldHeld = mProperties.launchHoldsPosition && launching;
        mInputFresh = true;
        // Drift only while the trigger is held, and only in an OpMode that has it on.
        mDriftHeld = mProperties.driveDriftAssist && mGamepad.left_trigger > mProperties.trgBtnThresh;
        // D-pad left or right: arc that way around the hive, but only while aiming (below).
        // Left is positive strafe, like the stick. Both pressed at once means neither.
        // Only with aimArc on.
        mArcDirection = 0;
        if (mProperties.aimArc) {
            if (mGamepad.dpad_left && !mGamepad.dpad_right) {
                mArcDirection = 1;
            } else if (mGamepad.dpad_right && !mGamepad.dpad_left) {
                mArcDirection = -1;
            }
        }

        // Circle resets which way is forward. Rumble so the driver feels it happen.
        mResetHeadingButton.In(mGamepad.circle);
        if (mResetHeadingButton.Out() && mProperties.fieldCentricDrive && mProperties.circleResetsForward) {
            mResetHeadingButton.Used();
            mGamepad.rumbleBlips(1);
            mImu.ResetYaw();
            // The position turns with the heading's 0, so a held spot no longer matches.
            ForgetHoldSpot();
        }

        // Left bumper: slow mode while held.
        if (mGamepad.left_bumper) {
            mSpeedLimit = mProperties.driveSlow;
        } else {
            mSpeedLimit = mProperties.driveNormal;
        }
    }

    /** true while the operator holds an aim bumper, or a launch trigger (with launchTriggerAims). */
    private boolean AimButtonDown() {
        return LaunchChoice.Aiming(mOperator, mProperties) != LaunchChoice.NONE;
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
        if (!mInputFresh) {
            mWiggleHeld = false;
            mHoldHeld = false;
        }
        mInputFresh = false;

        // Aim assist: the middle of the hive's tag group decides the turn, not one tag, so the
        // robot faces the hive itself. The sticks still drive and strafe.
        boolean aimingAtHive = mAimHeld && mVision.SeesTag();
        // true once aim is done turning, or has nothing to aim at: the wiggle may start.
        boolean aimSettled = true;
        if (aimingAtHive) {
            double hiveBearing = mVision.GetGroupBearing();
            // Aim a little off the hive's middle: the side offset for where the robot stands
            // (docs/decisions/0044), plus the launcher offset, since each launcher sits 2 in
            // to one side of the camera (pollen one way, nectar the other).
            double aimAt = mAimChoice.AimAtDeg(mVision.GetSideDegrees(), mVision.GetRange(), mProperties);
            double aimError = AimAssist.AimError(hiveBearing, aimAt);
            aimSettled = Math.abs(aimError) <= mProperties.aimDoneDegrees;
            mRotate = AimAssist.RotateToward(aimError, mProperties.aimKP,
                    mProperties.aimMaxRotate, mProperties.aimDoneDegrees);
            if (mOp.Shows(TelemetryGroup.DRIVE)) {
                mOp.mTelemetry.addData("Aim assist", "ON (%s): hive middle %.1f deg (tag %d, %.0f in), turn %.2f",
                        mAimChoice, hiveBearing, mVision.GetTagId(), mVision.GetRange(), mRotate);
            }
        } else if (mAimHeld && mOp.Shows(TelemetryGroup.DRIVE)) {
            mOp.mTelemetry.addLine("Aim assist: held, but no tag in view");
        } else if (mOp.Shows(TelemetryGroup.DRIVE)) {
            mOp.mTelemetry.addLine("Aim assist: off");
        }
        // Launch trigger with launchWiggle on: wiggle. It waits for aim to settle, then takes
        // over the turn, swinging around that heading, by the IMU, until the trigger is let go.
        if (mWiggleHeld && mImu.Ready() && (mWiggling || aimSettled)) {
            if (!mWiggling) {
                mWiggling = true;
                mWiggleCenterYaw = yaw;
                mWiggleTimer.reset();
            }
            int side = Wiggle.Side(mWiggleTimer.seconds(), mProperties.wiggleSwitchSeconds);
            mRotate = Wiggle.Turn(yaw, mWiggleCenterYaw, side, mProperties.wiggleDeg,
                    mProperties.wiggleKP, mProperties.wiggleMaxRotate);
            if (mOp.Shows(TelemetryGroup.DRIVE)) {
                mOp.mTelemetry.addData("Wiggle", "%s of %.1f deg, now %.1f off, turn %.2f",
                        side > 0 ? "left" : "right", mWiggleCenterYaw, yaw - mWiggleCenterYaw, mRotate);
            }
        } else {
            mWiggling = false;
        }
        // The raw buttons, so a trigger that rests a little pressed shows up on the screen.
        if (mOp.Shows(TelemetryGroup.DRIVE)) {
            mOp.mTelemetry.addData("Aim buttons (gamepad 2)",
                    "pollen: bumper %s, trigger %.2f; nectar: bumper %s, trigger %.2f (counts above %.2f)",
                    LaunchChoice.PollenBumper(mOperator, mProperties),
                    LaunchChoice.PollenTrigger(mOperator, mProperties),
                    LaunchChoice.NectarBumper(mOperator, mProperties),
                    LaunchChoice.NectarTrigger(mOperator, mProperties), mProperties.trgBtnThresh);
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
            if (mOp.Shows(TelemetryGroup.DRIVE)) {
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

        // Launch trigger with launchHoldsPosition on: stay on this spot if bumped.
        HoldPosition.Push hold = LaunchHold(yaw);

        // The drift pivot, the arc and the hold push are all worked out for the robot, not
        // the field, so they stay robot-centric whichever way the sticks are read.
        double robotDrive = driftDrive + arcDrive + hold.drive;
        double robotStrafe = driftStrafe + arcStrafe + hold.strafe;
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

        if (mProperties.stickTelemetry) {
            // The driver's sticks, untouched, from -1 to 1. Graph them on the dashboard. Hands
            // off, each should sit at 0: a number that stays off 0 is stick drift. If these
            // are right but the robot drives the wrong way, look at the yaw instead. Shown
            // with quiet telemetry too: they are already read, so they cost no loop time.
            mOp.mTelemetry.addData("Driver left stick X", mRawLeftX);
            mOp.mTelemetry.addData("Driver left stick Y", mRawLeftY);
            mOp.mTelemetry.addData("Driver right stick X", mRawRightX);
            mOp.mTelemetry.addData("Driver right stick Y", mRawRightY);
            mOp.mTelemetry.addData("Driver yaw (0 = away from the driver)", yaw);
        }
        if (!mOp.Shows(TelemetryGroup.DRIVE)) {
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

    /**
     * One loop of the launch hold. The first loop with the trigger held remembers the spot;
     * after that, a bump is pushed back. Moving the drive or strafe stick wins: no push, and
     * the spot is taken again once the stick is let go. Shoved too far, it gives up until
     * the trigger is let go.
     *
     * @return the push to add to the drive, robot-centric. Nothing when not holding.
     */
    private HoldPosition.Push LaunchHold(double yaw) {
        HoldPosition.Push none = HoldPosition.NONE;
        if (!mHoldHeld || !mImu.HasPosition()) {
            ForgetHoldSpot();
            mHoldGaveUp = false;
            return none;
        }
        if (mHoldGaveUp) {
            if (mOp.Shows(TelemetryGroup.DRIVE)) {
                mOp.mTelemetry.addLine("Launch hold: gave up (shoved too far), let go of the trigger");
            }
            return none;
        }
        boolean sticksMoving = Math.abs(mDrive) > mProperties.launchHoldStickDeadband
                || Math.abs(mStrafe) > mProperties.launchHoldStickDeadband;
        if (sticksMoving) {
            ForgetHoldSpot();
            if (mOp.Shows(TelemetryGroup.DRIVE)) {
                mOp.mTelemetry.addLine("Launch hold: driver moving, new spot when the stick is let go");
            }
            return none;
        }
        double x = mImu.GetXIn();
        double y = mImu.GetYIn();
        if (Double.isNaN(mHoldX)) {
            mHoldX = x;
            mHoldY = y;
        }
        HoldPosition.Push push = HoldPosition.Toward(mHoldX, mHoldY, x, y, yaw,
                mProperties.launchHoldKP, mProperties.launchHoldMaxPower, mProperties.launchHoldDoneIn);
        if (push.offIn > mProperties.launchHoldGiveUpIn) {
            mHoldGaveUp = true;
            mGamepad.rumbleBlips(2);
            return none;
        }
        if (mOp.Shows(TelemetryGroup.DRIVE)) {
            mOp.mTelemetry.addData("Launch hold", "%.1f in off, drive %.2f, strafe %.2f",
                    push.offIn, push.drive, push.strafe);
        }
        return push;
    }

    /** No held spot: the next launch hold takes a new one. */
    private void ForgetHoldSpot() {
        mHoldX = Double.NaN;
        mHoldY = Double.NaN;
    }
}
