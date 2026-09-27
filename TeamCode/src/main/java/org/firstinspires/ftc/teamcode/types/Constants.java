package org.firstinspires.ftc.teamcode.types;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;

/**
 * Every tunable number on the robot, with a name.
 *
 * <p>A {@code 0.05} typed straight into the code is a bug waiting to happen.
 * {@code Constants.trgBtnThresh} is something a student can find and change.
 *
 * <p>Only the drive block is here so far. It came over from Decode unchanged
 * (Decode types/Constants.java lines 6-19 and 75). Season numbers (launcher RPMs,
 * intake powers) arrive with the pieces that use them.
 */
public final class Constants {
    private Constants() {
    }

    // ---- Drive ------------------------------------------------------------

    /** true: forward on the stick is always away from the driver. false: the way the robot faces. */
    public static final boolean fieldCentricDrive = true;
    /** How far a trigger has to be pulled before it counts as pressed. */
    public static final double trgBtnThresh = 0.05;
    /** Top drive power while holding the right trigger. */
    public static final double driveBoosted = 1;
    /** Top drive power with no trigger held. */
    public static final double driveNormal = 1;
    /** Top drive power while holding the left trigger, for careful lining up. */
    public static final double driveSlow = 0.25;
    /** Mecanum wheels strafe a bit slower than they drive, so strafe gets a small boost. */
    public static final double driveStrafeGain = 1.1;
    /** Most a drive motor's power may change in one loop. 0.2 means 0 to full in 5 loops. */
    public static final double driveMotorSlewRate = 0.2;
    /** Loops a button must be held before it counts. Filters out electrical noise. */
    public static final int debounce = 3;

    // ---- Safe Mode (docs/decisions/0004-safe-mode.md) -------------------------------

    /** Fastest any drive motor may go in Safe Mode. 0.25 is one quarter of full power. */
    public static final double safeModeMaxPower = 0.25;

    // ---- Drive hardware names (must match the robot configuration on the Driver Station) --

    public static final String cfgDriveFrontLeft = "leftFront";
    public static final String cfgDriveFrontRight = "rightFront";
    public static final String cfgDriveBackLeft = "leftBack";
    public static final String cfgDriveBackRight = "rightBack";
    public static final String cfgPinpoint = "pinpoint";
    /** The IMU inside the Control Hub. Every Control Hub configuration has it by default. */
    public static final String cfgHubImu = "imu";

    // ---- Safe Mode key (docs/decisions/0007-safe-mode-key.md) --------------------------
    // Checked once at INIT. Key in = full speed. Key out = Safe Mode.
    //
    // NOW (bench testing): the touch sensor "liftHomeSwitch". Pressed = pretend the key is out.
    // LATER (the real key): REV Magnetic Limit Switch. Change the name below to the magnetic
    //   switch's name, and change keyPressedMeansKeyIn to true.

    /** The key sensor. Configure it as a "REV Touch Sensor" (the magnetic switch too). */
    public static final String cfgSafeModeKey = "liftHomeSwitch";
    /**
     * true: the sensor reads "pressed" when the key is IN (magnetic switch, magnet near).
     * false: "pressed" means the key is OUT (touch sensor stand-in for testing).
     */
    public static final boolean keyPressedMeansKeyIn = false;

    // ---- goBILDA Pinpoint odometry (tuned on the Decode robot) -------------------------
    // These only affect position tracking, not heading. Field-centric drive needs heading
    // alone, so a mistake here will not change how the robot drives. From Decode
    // MecanumDrive.PARAMS.inPerTick and PinpointLocalizer.PARAMS (lines 19-20).

    public static final double pinpointInPerTick = 0.0019526927633206;
    public static final double pinpointParYTicks = -1209.212244933345;
    public static final double pinpointPerpXTicks = -2661.37617633336;

    // ---- Control Hub IMU mounting (docs/decisions/0005) ---------------------------------
    // Which way the REV logo and the USB ports point on the robot. Wrong values make
    // turning read as tilting. Decode's robot: logo LEFT, USB UP (Decode Imu_Hub line 28).
    // Check this robot: if turning left does not raise Yaw on the Driver Station, fix these.

    public static final RevHubOrientationOnRobot.LogoFacingDirection hubLogoFacing =
            RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
    public static final RevHubOrientationOnRobot.UsbFacingDirection hubUsbFacing =
            RevHubOrientationOnRobot.UsbFacingDirection.UP;
}
