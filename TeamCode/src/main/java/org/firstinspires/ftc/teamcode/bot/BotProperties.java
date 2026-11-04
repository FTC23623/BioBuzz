package org.firstinspires.ftc.teamcode.bot;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * Every setting on the robot, with a name and its normal value.
 *
 * <p>A {@code 0.05} typed straight into the code is a bug waiting to happen.
 * {@code trgBtnThresh} is something a student can find and change.
 *
 * <p><b>How an OpMode uses it.</b> Make one, change only what this OpMode needs to be
 * different, then build the {@link Bot} with it:
 *
 * <pre>
 *     BotProperties properties = new BotProperties();
 *     properties.driveDisabled = true;       // this OpMode only tests the launcher
 *     properties.pollenLauncherRpm = 2000;   // and starts it slower than normal
 *     Bot bot = new Bot(this, properties);
 * </pre>
 *
 * <p>A change made in an OpMode only counts in that OpMode. To change a setting for every
 * OpMode, change its normal value in this file.
 *
 * <p>The Safe Mode and competition settings at the bottom are locked: the same in every OpMode.
 *
 * <p><b>Live tuning.</b> With FTC Dashboard open on a laptop, the settings listed in
 * {@code Bot.LiveTuning} show up under "Config" and can be changed while the robot runs.
 * A change made there is lost at STOP: write the winning number here (docs/decisions/0022).
 *
 * <p>This file took over from types/Constants.java (docs/decisions/0010). The names are the
 * same, so {@code Constants.driveSlow} in an older note means {@code driveSlow} here.
 */
public class BotProperties {

    // ---- Which parts of the robot this OpMode uses ----------------------------------------
    // Normally all of them. Switch a part off to test another part alone. A part that is
    // off is never set up, so it does not even have to be in the robot configuration.

    /**
     * true: no driver control of the wheels. The Bot never touches the drive motors or the
     * heading sensor. Autos set this too: in an auto, Pedro Pathing drives the wheels.
     */
    public boolean driveDisabled = false;
    /** true: the Bot never touches the two launcher motors. */
    public boolean launcherDisabled = false;
    /** true: the Bot never touches the intake and transfer motors. */
    public boolean intakeDisabled = false;
    /** true: the Bot never touches the pollen and nectar launch servos. */
    public boolean launchGatesDisabled = false;
    /** true: the Bot never touches the flower extractor servo. */
    public boolean flowerExtractorDisabled = false;
    /** true: the Bot never looks for a camera. Aim assist is off. */
    public boolean visionDisabled = false;
    /** true: the Bot never touches the indicator light. */
    public boolean indicatorDisabled = false;
    /** true: the Bot never touches the headlight. */
    public boolean headlightDisabled = false;
    /** true: the Bot never touches the LED strip. */
    public boolean stripDisabled = false;
    /**
     * true: no motor tuner. Off in every OpMode except Test: Motor Tuning, because the tuner
     * uses the same launcher motor as the launcher, and gamepad 1's D-pad, which the drive
     * uses to arc around the hive.
     */
    public boolean motorTunerDisabled = true;
    /**
     * true: no launch servo tuner. Off in every OpMode except Test: Launch Servo Tuning,
     * because it uses the same D-pad as the drive and the motor tuner.
     */
    public boolean launchServoTunerDisabled = true;
    /**
     * true: no pose recorder. Off in every OpMode except Tool: Pose Recorder, because it
     * uses cross, triangle and square on gamepad 1 (docs/decisions/0024).
     */
    public boolean poseRecorderDisabled = true;

    // ---- Drive ------------------------------------------------------------------------------

    /**
     * true: forward on the stick is always away from the driver, whichever way the robot
     * faces (HyDrive, coach 2026-11-02). false: forward is the way the robot faces. The
     * Practice TeleOps and Tool: Pose Recorder pick their own.
     */
    public boolean fieldCentricDrive = true;
    /** How far a trigger has to be pulled before it counts as pressed. */
    public double trgBtnThresh = 0.05;
    /** Top drive power with no trigger held. The right trigger launches, so there is no boost. */
    public double driveNormal = 1;
    /**
     * Top drive power while holding the left bumper, for careful lining up. Half speed
     * (coach, 2026-11-03: a quarter was too slow to be useful).
     */
    public double driveSlow = 0.5;
    /** Mecanum wheels strafe a bit slower than they drive, so strafe gets a small boost. */
    public double driveStrafeGain = 1.1;
    /** Most a drive motor's power may change in one loop. 0.2 means 0 to full in 5 loops. */
    public double driveMotorSlewRate = 0.2;
    /** Loops a button must be held before it counts. Filters out electrical noise. */
    public int debounce = 3;
    /**
     * true: after START the Driver Station shows only the loop time and the battery, and
     * the parts skip the hardware reads they make just for the screen (the Limelight's
     * status, motor powers). For measuring loop time: tick it on the dashboard's Drive group
     * while HyDrive runs, untick it to get the full screen back. Off by default, so a match
     * (no dashboard) always has the full screen. INIT shows everything either way
     * (docs/decisions/0041).
     */
    public boolean quietTelemetry = false;
    /**
     * How often the battery voltage is read, in milliseconds. Each read is an extra trip to
     * the hub, so not every loop. 250 = four times a second, more than enough for a graph.
     */
    public double batteryReadMs = 250;

    // Driver practice options, from Decode's DrivePractice (docs/decisions/0018). Swapped
    // sticks and tight turns are off in HyDrive; the Practice TeleOps switch them on. Drift
    // assist is on everywhere (0034).
    /** true: left stick X turns and right stick X strafes. Left stick Y still drives. */
    public boolean driveSwapStrafeRotate = false;
    /** Turning gets this much of the stick when swapped, since one thumb drives and turns. */
    public double driveSwappedTurnGain = 0.5;
    /** true: turning wins over driving when both sticks are pushed. Not used when swapped. */
    public boolean driveTightTurns = false;
    /** How much stronger turning is with tight turns on. 1.5 = half again as strong. */
    public double driveTightTurnGain = 1.5;
    /**
     * true: hold the LEFT TRIGGER while turning and the robot pivots on its inside back
     * corner instead of its middle, like a car drifting. On in HyDrive and the Practice
     * TeleOps (coach, 2026-11-02: it was the right bumper and Practice only; slow mode moved
     * to the right bumper to make room, then to the left bumper on 2026-11-03).
     */
    public boolean driveDriftAssist = true;
    /** Drift pivot, side to side. 0 = the robot's middle, 0.5 = the inside wheels (square robot). */
    public double driveDriftSideGain = 0.5;
    /** Drift pivot, front to back. 0 = the robot's middle, 0.5 = the back wheels (square robot). */
    public double driveDriftRearGain = 0.5;

    // ---- Drive hardware names (must match the robot configuration on the Driver Station) --

    public String cfgDriveFrontLeft = "leftFront";
    public String cfgDriveFrontRight = "rightFront";
    public String cfgDriveBackLeft = "leftBack";
    public String cfgDriveBackRight = "rightBack";
    public String cfgPinpoint = "pinpoint";
    /** The IMU inside the Control Hub. Every Control Hub configuration has it by default. */
    public String cfgHubImu = "imu";

    // ---- goBILDA Pinpoint odometry (tuned on the Decode robot) -------------------------
    // These only affect position tracking, not heading. Field-centric drive needs heading
    // alone, so a mistake here will not change how the robot drives. From Decode
    // MecanumDrive.PARAMS.inPerTick and PinpointLocalizer.PARAMS (lines 19-20).

    /** goBILDA 4-bar pod: 19.894 ticks per mm, the pod type the Pinpoint Tuner was told (2026-10-17). */
    public double pinpointInPerTick = 1 / (19.89436789 * 25.4);
    public double pinpointParYTicks = -1209.212244933345;
    public double pinpointPerpXTicks = -2661.37617633336;

    // ---- Robot size ------------------------------------------------------------------------
    // The outside of the frame, bumpers and all. These are the Code bot's. CHANGE THESE for
    // the competition bot: it is narrower.

    /** Frame length, front to back, in inches. */
    public double frameLengthIn = 18;
    /** Frame width, side to side, in inches. */
    public double frameWidthIn = 18;
    /** Extra room left between a spinning corner and the wall, in inches. */
    public double turnClearanceIn = 0.5;

    // ---- Control Hub IMU mounting (docs/decisions/0005) ---------------------------------
    // Which way the REV logo and the USB ports point on the robot. Wrong values make
    // turning read as tilting. Decode's robot: logo LEFT, USB UP (Decode Imu_Hub line 28).
    // Check this robot: if turning left does not raise Yaw on the Driver Station, fix these.

    public RevHubOrientationOnRobot.LogoFacingDirection hubLogoFacing =
            RevHubOrientationOnRobot.LogoFacingDirection.LEFT;
    public RevHubOrientationOnRobot.UsbFacingDirection hubUsbFacing =
            RevHubOrientationOnRobot.UsbFacingDirection.UP;

    // ---- Odometry pods, read by Pedro Pathing (docs/decisions/0008) ---------------------
    // With no Pinpoint, the two odometry pods plug straight into the hub's encoder jacks.
    // An encoder jack belongs to a motor port, so the "name" of a pod is the name of the
    // motor on that port. CHECK THESE ON THE ROBOT: they are a guess until someone looks.

    /** Motor port whose encoder jack has the pod that rolls when the robot drives FORWARD. */
    public String cfgForwardPodPort = "leftFront";
    /** Motor port whose encoder jack has the pod that rolls when the robot drives SIDEWAYS. */
    public String cfgStrafePodPort = "rightBack";

    // ---- Nectar launcher (docs/decisions/0009-launcher-velocity-control.md) ---------------
    // The launcher holds a SPEED (RPM), not a power. Each loop it measures the wheel's
    // speed and picks the power that gets it to the target. Starting numbers are Decode's
    // (Decode subsystems/Launcher.java lines 36-48, types/Constants.java lines 17-18).

    /** The launcher motor. Must match the robot configuration on the Driver Station. */
    public String cfgLaunchNectar = "nectarLauncher";
    /**
     * Encoder ticks for one turn of the launcher motor's shaft. 28 = a motor with the
     * encoder on the bare motor shaft (no gearbox), like Decode's. CHECK THIS: a geared
     * motor has a different number, and every RPM on the screen will be wrong.
     */
    public double launcherTicksPerRev = 28;
    /**
     * Pollen launcher target speed. D-pad up / down on gamepad 2 changes it while driving,
     * and so does the dashboard. Each launcher has its own speed.
     */
    public double pollenLauncherRpm = 3100;
    /** Nectar launcher target speed. D-pad right / left on gamepad 2 changes it. */
    public double nectarLauncherRpm = 0;
    /** How much one D-pad press changes a launcher's target speed (or its trim, below). */
    public double launcherRpmStep = 50;

    // ---- Launcher speed from distance (docs/decisions/0038) --------------------------------
    // A farther shot needs a faster wheel. While the camera sees our hive, the pollen
    // launcher's target follows the table below, so the wheel is already at the right speed
    // when the trigger is pulled. Measured by the coach, 2026-11-02: 2900 at 110 in, 3300
    // past 140 in; the far row raised 25 to 3325 on 2026-11-03. The rows between are a
    // straight line from 2900 to 3325, rounded; tune each on the robot.

    /**
     * true: the pollen launcher's target follows the distance curve below whenever a tag of
     * ours is in view, and D-pad up / down trims the curve instead of setting the speed. false:
     * the D-pad and the dashboard set pollenLauncherRpm directly, as before.
     */
    public boolean launcherRangeTuning = true;
    // The speed is a curve through two measured shots, stepped every launchBandIn inches.
    // A ball flies in an arc, so each step out needs a little more than the one before:
    //
    //     rpm = near + (far - near) x ((band start - nearIn) / (farIn - nearIn)) ^ launchCurve
    //
    // launchCurve 1 is a straight line; higher bends harder toward the far end. 1.1 is subtle:
    // the steps grow from 22 RPM near the hive to 31 RPM at 140 in (coach, 2026-11-03).
    // Nearer than nearIn uses the near speed; farther than farIn uses the far speed.
    /** The near measured shot: distance, in inches. */
    public double launchNearIn = 110;
    /** The near measured shot: RPM that scored from there. */
    public double launchNearRpm = 2900;
    /** The far measured shot: distance, in inches. */
    public double launchFarIn = 140;
    /** The far measured shot: RPM that scored from there (3300 measured, +25 on 2026-11-03). */
    public double launchFarRpm = 3325;
    /** How bent the curve is. 1 = a straight line, 2 = a parabola. 1.1 to start. */
    public double launchCurve = 1.1;
    /** The speed changes in steps this many inches apart, so it doesn't hunt with every reading. */
    public double launchBandIn = 2;
    /**
     * Added to every step of the curve. D-pad up / down on gamepad 2 moves it by
     * launcherRpmStep while range tuning is on, so the operator can nudge every shot faster
     * or slower on the day. On the dashboard's Launcher group.
     */
    public double launcherRangeTrimRpm = 0;
    /** The target speed never goes above this. */
    public double launcherMaxRpm = 6000;
    /** How often the launcher measures its speed and picks a new power, in milliseconds. */
    public double launcherUpdateMs = 50;
    /** "At speed" means the measured RPM is within this much of the target. */
    public double launcherAtSpeedRpm = 50;
    /**
     * A powered launcher reading slower than this (backward) is spinning the wrong way. A
     * ball or a bump can kick a wheel backward for a moment; that is fine.
     */
    public double launcherWrongWayRpm = -100;
    /**
     * Only a launcher that stays backward this long, in seconds, while powered, is stopped.
     * Shorter reverses are ignored. Turning the launchers off and on again clears the stop.
     */
    public double launcherWrongWaySeconds = 1.0;

    // ---- Pollen launcher, intake and transfer (docs/decisions/0019 and 0023) ---------------
    // The pollen launcher uses the nectar launcher's settings above (ticks per turn, gains,
    // steps), so the two must be the same kind of motor.

    /** The second launcher motor. Must match the robot configuration on the Driver Station. */
    public String cfgLaunchPollen = "pollenLauncher";
    /** The intake motor. Must match the robot configuration on the Driver Station. */
    public String cfgIntake = "intake";
    /** The transfer motor. Must match the robot configuration on the Driver Station. */
    public String cfgTransfer = "transfer";
    // Launcher directions, like the intake's: true = the motor is set REVERSE, false = the
    // normal way (FORWARD). If the screen says a launcher spins the wrong way and stops it,
    // flip its setting.
    /** true: the nectar launcher runs REVERSE. It is mounted the other way on this bot (coach, 2026-10-31). */
    public boolean nectarLauncherReversed = true;
    /** true: the pollen launcher runs REVERSE. false: the normal way, FORWARD. */
    public boolean pollenLauncherReversed = true;
    /** true: the intake spins the other way. CHECK on the robot: it should pull balls in. */
    public boolean intakeReversed = false;
    /** true: the transfer spins the other way. CHECK on the robot: it should carry balls up. */
    public boolean transferReversed = false;
    /**
     * Encoder ticks for one turn of the intake and transfer OUTPUT shafts. The intake and
     * transfer top out at 1150 RPM while the launchers do 6000 (coach, 2026-11-02), so they
     * are NOT the same motor as the launchers: a 1150 RPM goBILDA motor has a 5.2:1 gearbox,
     * and its encoder gives 145.1 ticks per turn of the output shaft (goBILDA's number).
     * CHECK in Test: Motor Tuning: at a 1150 target the actual RPM should read about 1150
     * with the power near 1. Actual far below target at full power means this is too big;
     * target reached at low power means it is too small.
     */
    public double intakeTicksPerRev = 145.1;
    // Speeds are in output RPM, out of a 1150 top. 100% when intaking on purpose, a share
    // of that when feeding a launch (launchFeed...Scale), about a third backward.
    /** Intake target speed, pulling balls in: full speed. Each roller has its own speed. */
    public double intakeRpm = 1150;
    /** Transfer target speed, carrying balls up to the launchers: full speed. */
    public double transferRpm = 1150;
    // While a launch is being fed (the gates fully open), each roller runs at a fraction of
    // its speed above, so balls go into the launcher one at a time at a steady rate.
    // Intaking on purpose is always 100%.
    /** The intake's share while feeding a launch: 0.5 = half (coach, 2026-11-02). */
    public double launchFeedIntakeScale = 0.5;
    /**
     * The transfer's share while feeding a launch: 0.3. At half the balls ran into each
     * other at the launcher (coach, 2026-11-03).
     */
    public double launchFeedTransferScale = 0.3;
    // Reverse is SLOWER than forward on purpose: at full speed one tap of square emptied the
    // robot. About a third of forward lets the operator let out one ball at a time (coach,
    // 2026-11-02). Both are on the dashboard's Intake group: tune them there, then write the
    // winner here.
    /**
     * Intake speed while gamepad 2 holds square: spitting balls back out, for when the robot
     * has picked up too many. A plus number; the code turns it into backward.
     */
    public double intakeReverseRpm = 400;
    /** Transfer speed while gamepad 2 holds square, carrying balls back down. A plus number. */
    public double transferReverseRpm = 400;
    // Auto-reject (docs/decisions/0040): the pollen switch sits where a ball only waits once
    // the robot is full. If the operator is intaking and pollen stays detected that long, the
    // intake spits the extra ball back out while the transfer keeps the rest moving up. Then
    // the operator's intake and transfer stop until both their buttons are let go.
    /** true: reject an extra ball by itself. false: the operator watches for it. */
    public boolean pollenAutoReject = true;
    /** Pollen must stay detected this long while intaking, in seconds, before a reject. */
    public double pollenRejectAfterSeconds = 0.5;
    /** How long the intake runs backward (transfer forward) to spit the ball out, in seconds. */
    public double pollenRejectSeconds = 0.75;
    /** How much one D-pad press changes a roller's speed in Test: Motor Tuning. Same as the launchers'. */
    public double intakeRpmStep = 50;
    /** The intake and transfer target speed never goes above this: the motors' top speed. */
    public double intakeMaxRpm = 1150;
    // Hub speed-control gains for the intake and transfer (docs/decisions/0025). The hub
    // runs this loop itself; we only hand it the numbers. Starting values are the REV hub's
    // usual defaults, so nothing changes until someone tunes them. CHECK on the robot: the
    // INIT screen shows "hub started with". If it differs, copy those numbers here.
    // Tuning order: kF first, then kP, then kI. Leave kD at 0.
    /** Proportional: how hard the hub pushes back when the roller is off its target speed. */
    public double rollerKP = 10;
    /** Integral: extra push for being off target for a long time. */
    public double rollerKI = 3;
    /** Derivative: brakes fast changes. Usually 0 for a roller. */
    public double rollerKD = 0;
    /** Feed forward: a guess of the power from the target alone. */
    public double rollerKF = 0;

    // ---- Launch servos (docs/decisions/0020) ---------------------------------------------
    // A ball launches when its servo "opens". Test: Launch Servo Tuning finds the closed and
    // open spots for each one.

    /** The pollen launch servo. CHECK: must match the robot configuration on the Driver Station. */
    public String cfgPollenServo = "pollenServo";
    /** The nectar launch servo. CHECK: must match the robot configuration on the Driver Station. */
    public String cfgNectarServo = "nectarServo";
    /**
     * The flower extractor servo. Tuned in the same OpMode as the launch servos, with the
     * same range and step below, so it must be the same kind of servo.
     */
    public String cfgFlowerExtractor = "flowerExtractor";
    /**
     * How far the servo turns from position 0 to position 1, in degrees. 300 = a goBILDA
     * 2000 series servo. CHECK THIS: a REV Smart Servo is 270, and every degree on the
     * screen will be off if this is wrong. Positions (0 to 1) are right either way.
     */
    public double launchServoRangeDeg = 300;
    /** How far one D-pad press turns a launch servo while tuning, in degrees. */
    public double launchServoStepDeg = 5;
    /**
     * How long a launch servo takes to turn 60 degrees, in seconds. The transfer waits for
     * the gate to swing out of the way before pushing balls in (docs/decisions/0028).
     * goBILDA's numbers are at 7.4 V: 25-2 Torque 0.17, 25-3 Speed 0.07, 25-4 Super Speed
     * 0.035. The hub's servo ports give about 5 V, so a servo is slower there. 0.11 makes
     * the whole wait about 0.2 s (coach, 2026-10-31). If the transfer starts before the gate
     * is out of the way, raise it.
     */
    public double launchServoSecondsPer60Deg = 0.11;
    /** Extra wait after the servo should be open, in seconds, for the load on it. */
    public double launchServoExtraSeconds = 0.05;

    // Closed and open spots, measured with Test: Launch Servo Tuning. No servo goes outside
    // its closed-to-open range. The two launch servos face each other, so pollen's numbers
    // are nectar's flipped.

    /** Nectar launch servo position when closed. */
    public double nectarServoClosed = 0;
    /** Nectar launch servo position when open (launching). */
    public double nectarServoOpen = 0.267;
    /** Pollen launch servo position when closed. */
    public double pollenServoClosed = 0.267;
    /** Pollen launch servo position when open (launching). */
    public double pollenServoOpen = 0;
    /** Flower extractor position when closed (stored): gamepad 2 triangle not held. */
    public double flowerExtractorClosed = 0;
    /** Flower extractor position when open (deployed): gamepad 2 triangle held. 0.567 plus 5 degrees. */
    public double flowerExtractorOpen = 0.584;
    /**
     * Tuning only: the bumpers may push the flower extractor this many degrees past open,
     * to try a further deployed spot. Cross / A still deploys to flowerExtractorOpen. 10
     * degrees past 0.584 is about 0.617.
     */
    public double flowerExtractorTuneExtraDeg = 10;
    /**
     * How fast a Pedro auto drives, as a part of top speed: 1 = full, 0.5 = half. Lower it to
     * check a new path on the field, then put it back to 1 for matches.
     */
    public double autoSpeedScale = 1;
    // Giving up on a path. Each path gets its own time limit, worked out at INIT from its
    // length and its turning (PedroAuto.GiveUpSeconds). Past it, the robot stops pushing and
    // starts the next path, so it does not keep ramming a wall or a stuck ball. If a good
    // path gets cut short, raise the margin first.

    /** The time limit is the expected time multiplied by this. 1.5 = 50% more. */
    public double autoGiveUpMargin = 1.5;
    /** Then this many seconds are added, for speeding up at the start and braking at the end. */
    public double autoGiveUpExtraSeconds = 1;
    /**
     * true: in an auto, the intake and transfer run from START to the end, and the launchers
     * spin the whole time. A LAUNCH step opens both launch servos.
     */
    public boolean autoRunScoring = true;
    /** How long a 90-degree turn takes at full speed, in seconds. A guess: CHECK on the robot. */
    public double autoQuarterTurnSeconds = 1;
    /**
     * In an auto, the most a LAUNCH step waits for the launchers to reach speed before it
     * opens the gates anyway, in seconds. Usually they are already at speed: they spin from
     * START.
     */
    public double autoSpinUpSeconds = 2;
    /**
     * A LAUNCH_WHEN_TAG_SEEN step still waiting for one of our hive's tags this many seconds
     * after START launches anyway, facing the way it is. AUTO is 30 seconds (Competition
     * Manual: "a 30-second AUTO period"), and a robot should never end it holding its balls.
     */
    public double autoLaunchAnywayAtSeconds = 26;
    /** In an auto, how long the gates stay open at a LAUNCH step, in seconds. */
    public double autoLaunchSeconds = 2;
    /**
     * In an auto, the most time the robot spends turning to face a tag before it launches
     * anyway, in seconds. A guess: CHECK on the robot.
     */
    public double autoAimSeconds = 1.5;

    // Speed control gains. Tune on the robot: kF first, then kP, then kI (see 0009).
    /** Feed forward: power per RPM of target. Does most of the work. 0.000242 x 2475 = 0.60. */
    public double launcherKF = 0.000242;
    /** Proportional: extra power per RPM the wheel is too slow. */
    public double launcherKP = 0.003;
    /**
     * Integral: extra power for being too slow for a long time. Starts at 0: Decode's was
     * 0.0001, but FTCLib capped it so hard it never added more than 0.0001 power. Raise it
     * only if the wheel settles a little below the target and stays there.
     */
    public double launcherKI = 0;

    // ---- Vision: AprilTags and aim assist (docs/decisions/0011) ----------------------------
    // The Bot looks for a Limelight first, then a webcam. With neither, aim assist is off
    // and everything else still works.

    /** The Limelight 3A, if the robot has one. Configure it as a "Limelight 3A". */
    public String cfgLimelight = "limelight";
    /** The USB webcam, if the robot has one instead. Configure it as a "Webcam". */
    public String cfgWebcam = "Webcam 1";
    /** How many times a second the Limelight is asked for a new result. */
    public int limelightPollRateHz = 100;
    /** Which of the Limelight's pipelines (0 to 9) is set up for AprilTags. */
    public int limelightAprilTagPipeline = 0;
    /** A tag last seen longer ago than this is treated as gone. Keeps the robot from chasing a ghost. */
    public double visionMaxAgeMs = 250;
    /**
     * Tags that belong together, as {lowest id, highest id}: this season, the four tags on
     * each hive. An auto aims at the middle of the group the nearest tag is in.
     */
    public int[][] tagGroups = {{30, 33}, {34, 37}, {38, 41}, {42, 45}};
    /**
     * Our hive's tags, lowest and highest id, when we are red. Competition Manual section 9.9:
     * 30-33 and 34-37 are on the red cells. Aim assist and the autos ignore every other tag.
     */
    public int[] redHiveTags = {30, 37};
    /** Our hive's tags when we are blue. Manual section 9.9: 38-41 and 42-45, the blue cells. */
    public int[] blueHiveTags = {38, 45};
    /** Any tag at all: used while the alliance is UNKNOWN, or with visionAnyTag on. */
    public int[] anyTag = {0, 1000};
    /**
     * PRACTICE ONLY. true: every AprilTag counts, not just our hive's. For practice with old
     * tags (last season's 20 to 24) or with no alliance magnet on the robot. NEVER true at an
     * event: aim assist would lock onto the other alliance's hive. The Driver Station warns
     * in capitals while it is on.
     */
    public boolean visionAnyTag = false;

    // Aim assist: hold the right bumper and the robot turns to face the middle of our hive.
    /**
     * Turn power per degree the tag is off center. 0.02 x 20 degrees = 0.4 power.
     * CHECK ON THE ROBOT: if the robot turns away from the tag, make this negative.
     */
    public double aimKP = 0.02;
    /** Aim assist never turns harder than this, either way. */
    public double aimMaxRotate = 0.6;
    /** Within this many degrees of center, aim assist stops turning. Stops the wobble. */
    public double aimDoneDegrees = 1.0;
    /**
     * true: the driver's launch trigger also turns the robot to face the middle of our hive's
     * tags while it is held, the way an auto aims before LAUNCH_WHEN_TAG_SEEN (coach,
     * 2026-11-02). false: the trigger only opens the gates; the right bumper still aims.
     */
    public boolean launchTriggerAims = true;
    /**
     * true: the driver's launch trigger only opens the gates while the camera is LOCKED on
     * our hive: a tag of ours in view and the hive's middle within aimDoneDegrees. Pulled
     * with no tag, the gates stay shut and the driver's gamepad buzzes, so a wild launch is
     * caught (coach, 2026-11-03). The operator's right trigger, held, overrides the lock for
     * a camera that has died. false: the trigger launches whatever the camera sees.
     * OFF since 2026-11-03 at the coach's request; tick it on the dashboard's Launcher group
     * to try it again.
     */
    public boolean launchNeedsLock = false;
    /** How long the driver's gamepad buzzes when the trigger is pulled with no lock, in milliseconds. */
    public int launchNoLockRumbleMs = 300;

    // Arc around the hive (docs/decisions/0039): while aiming, D-pad left / right slide the
    // robot sideways. Aim keeps the nose on the hive, and a little drive keeps the distance
    // the arc started at, so the robot circles the hive instead of sliding away from it.
    // Without the aim button the D-pad does nothing.
    /** How hard the robot strafes while arcing. 0.4 is a brisk walk. */
    public double arcStrafePower = 0.4;
    /**
     * Drive power per inch the robot has drifted from the distance the arc started at.
     * 0.02 x 10 inches = 0.2 power. Too far: drives in. Too near: backs off.
     */
    public double arcRangeKP = 0.02;
    /** The distance correction never drives harder than this, either way. */
    public double arcMaxDrive = 0.4;

    // ---- Alliance (docs/decisions/0021) ---------------------------------------------------
    // A REV Magnetic Limit Switch on the robot. A magnet near it means RED. No magnet, or no
    // sensor at all, means BLUE. Read over and over during INIT, so the drive team can move
    // the magnet and watch the LED strip change color. Fixed at START.

    /** The alliance sensor. Configure it as a "REV Touch Sensor" (the magnetic switch is one). */
    public String cfgAllianceSensor = "allianceMagnet";
    /**
     * UNKNOWN: read the alliance sensor (normal). RED or BLUE: skip the sensor and use this.
     * Only the backup autos ("Auto: ... Red", "Auto: ... Blue") set it.
     */
    public Alliance allianceOverride = Alliance.UNKNOWN;

    /** Our hive's tags for this alliance: red or blue, or any tag while UNKNOWN or in practice. */
    public int[] HiveTags(Alliance alliance) {
        if (visionAnyTag) {
            return anyTag;
        }
        if (alliance == Alliance.RED) {
            return redHiveTags;
        }
        if (alliance == Alliance.BLUE) {
            return blueHiveTags;
        }
        return anyTag;
    }

    // ---- Indicator light (docs/decisions/0015) ---------------------------------------------
    // A goBILDA RGB indicator light on a servo port. Each color is a servo position, from
    // goBILDA's table: 0 off, 0.28 red, 0.333 orange, 0.388 yellow, 0.5 green, 0.611 blue,
    // 0.722 violet, 1 white.

    /** The light. Configure it as a "Servo" on the servo port it is plugged into. */
    public String cfgIndicator = "led";
    /** Three or more of our tags in view. */
    public double indicatorGreen = 0.5;
    /** Two of our tags in view. One of our tags in view blinks this color. */
    public double indicatorBlue = 0.611;
    /**
     * No tag of ours in view. Blinks this color when the only tags in view are the other
     * alliance's: the camera works, so check the alliance magnet.
     */
    public double indicatorRed = 0.280;
    /** An auto is at a LAUNCH step. Blinks this color. */
    public double indicatorYellow = 0.388;
    /** An auto is waiting to see a tag before it launches. Blinks this color. */
    public double indicatorOrange = 0.333;
    /** The light off: before START, and the dark half of a blink. */
    public double indicatorOff = 0;
    /** When the light blinks: this long on, this long off, in milliseconds. */
    public double indicatorBlinkMs = 300;

    // ---- Headlight -------------------------------------------------------------------------
    // A light on a servo port that lights up the field so the Limelight sees tags better.
    // It turns on at INIT and stays on.

    /** The headlight. Configure it as a "Servo" on the servo port it is plugged into. */
    public String cfgHeadlight = "headlight";
    /** The servo position that turns the headlight on. 1 is full brightness. */
    public double headlightOn = 1;

    // ---- LED strip: goBILDA Prism RGB LED driver, over I2C (docs/decisions/0017) ---------

    /** The Prism driver: type "goBILDA Prism LED Driver" on an I2C port the Pinpoint is not on. */
    public String cfgStrip = "strip";
    /**
     * How many LEDs are on the strip. One 200 mm Prism strip has 12. Chained strips add up.
     * 36 = LEDs 0 to 35, for the underglow below. CHECK it matches the strip.
     */
    public int stripLedCount = 36;
    // Every LED range below is a FIRST and a LAST LED, counted from 0, and both are lit.
    // "12 to 35" means LED 12, LED 35, and every LED between (24 LEDs).
    /**
     * Underglow: these LEDs show the alliance color from INIT to the end of the match.
     * LEDs 12 to 35 (coach, 2026-10-31).
     */
    public int underglowFirstLed = 12;
    public int underglowLastLed = 35;
    /**
     * How the Prism counts the end of a run. true: it wants one past the last LED, so
     * "12 to 35" is sent as start 12, stop 36 (the guide's example: start 0, stop 10 for ten
     * LEDs). CHECK on the strip: if every run lights one LED too few, set this false.
     */
    public boolean prismStopIsOnePastLast = true;
    /** Strip brightness, 0 to 100. Start low; 100 is very bright and warm. */
    public int stripBrightness = 50;

    // ---- Pollen sensor (docs/decisions/0026, changed by 0033, 0034, 0035) ------------------
    // While pollen is detected, every layer of the strip is cleared and the LEDs below turn
    // yellow, and the operator's gamepad rumbles once. The holes in a ball make the switch
    // open and close as the ball turns, so "detected" is held on for a moment after each
    // press, and only goes off once the switch has stayed open that long. Then every layer is
    // cleared again and the alliance underglow comes back.

    /** The pollen switch. Configure it as a "REV Touch Sensor" on a digital port. */
    public String cfgPollenSensor = "pollenSensor";
    /**
     * true: the switch reads "pressed" when there is NO pollen, and lets go when a ball
     * arrives, so the reading is flipped here. That is how it is wired today (coach,
     * 2026-11-02). false: pressed means pollen. If the strip is yellow with the intake empty,
     * flip this.
     */
    public boolean pollenSensorInverted = true;
    /**
     * The operator's gamepad rumbles this long, in milliseconds, the moment pollen is first
     * detected, so they know a ball is in without looking at the strip. 0 = no rumble.
     */
    public int pollenRumbleMs = 300;
    /**
     * How long the yellow stays on after the switch last read pressed, in milliseconds. 500
     * = half a second (coach, 2026-11-02). The holes in a ball pass the switch faster than
     * that, so a ball reads as one steady "detected"; an empty intake goes dark half a
     * second after the last ball leaves.
     */
    public double pollenHoldMs = 500;
    /**
     * The yellow LEDs: the WHOLE strip for now (coach, 2026-11-02), LED 0 to the last one,
     * both lit. Before 0033 it was two small groups, 0-3 and 6-9.
     */
    public int pollenLedsFirst = 0;
    public int pollenLedsLast = 35;

    // ---- Safe Mode: LOCKED (docs/decisions/0004 and 0007) --------------------------------
    // Safe Mode protects the people standing next to the robot, so these three are not
    // per-OpMode settings. "static final" means: one value, shared by every OpMode, that
    // can't be changed while the program runs. Writing
    // "properties.safeModeMaxPower = 1;" in an OpMode will not compile.
    //
    // The key is checked once at INIT. "Remove Before Flight": key in = Safe Mode, key
    // removed = full speed.
    // NOW (bench testing): the touch sensor "liftHomeSwitch". Pressed = pretend the key is
    //   removed (full speed). Not pressed = key in (Safe Mode).
    // LATER (the real key): REV Magnetic Limit Switch. Change the name below to the magnetic
    //   switch's name, and change keyPressedMeansKeyIn to true.

    /**
     * TEMPORARY, 2026-10-17: Safe Mode is switched OFF while Pedro's Foresight numbers are
     * tested at the speed they were measured at. The robot runs at full speed, key or no
     * key, and the Driver Station says so in capitals. Set this back to true when the
     * autos are proven (docs/decisions/0014).
     */
    public boolean safeModeEnabled = false;
    /** Fastest any drive motor may go in Safe Mode. 0.25 is one quarter of full power. */
    public static final double safeModeMaxPower = 0.25;
    /** The key sensor. Configure it as a "REV Touch Sensor" (the magnetic switch too). */
    public static final String cfgSafeModeKey = "liftHomeSwitch";
    /**
     * true: the sensor reads "pressed" when the key is IN (magnetic switch, magnet near).
     * false: "pressed" means the key is REMOVED (touch sensor stand-in for testing).
     */
    public static final boolean keyPressedMeansKeyIn = false;

    // ---- Competition: LOCKED (docs/decisions/0022) --------------------------------------
    // Competition Manual rule R704.D: during a match, only the Driver Station app may stream
    // telemetry. "Third party plugins and tools such as FTC Dashboard ... are prohibited."

    /**
     * true at an event. The Bot then sends nothing to FTC Dashboard, offers no live tuning,
     * the webcam does not stream to it, and the Limelight web page relay does not start.
     * The Driver Station warns until the dashboard is also switched off in the Robot
     * Controller app's menu (Disable Dashboard). false at practice.
     */
    public static final boolean competitionMode = false;
}
