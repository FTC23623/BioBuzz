package org.firstinspires.ftc.teamcode.bot;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.pedropathing.follower.Follower;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.BuildConfig;
import org.firstinspires.ftc.teamcode.objects.DashboardTuning;
import org.firstinspires.ftc.teamcode.objects.HeadingHandoff;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.pedro.PedroConstants;
import org.firstinspires.ftc.teamcode.subsystems.AllianceSensor;
import org.firstinspires.ftc.teamcode.subsystems.Drive_Manual;
import org.firstinspires.ftc.teamcode.subsystems.FlowerExtractor;
import org.firstinspires.ftc.teamcode.subsystems.Headlight;
import org.firstinspires.ftc.teamcode.subsystems.Imu;
import org.firstinspires.ftc.teamcode.subsystems.Indicator;
import org.firstinspires.ftc.teamcode.subsystems.ImuSelector;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.subsystems.LaunchGates;
import org.firstinspires.ftc.teamcode.subsystems.LaunchServoTuner;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;
import org.firstinspires.ftc.teamcode.subsystems.LightStrip;
import org.firstinspires.ftc.teamcode.subsystems.MotorTuner;
import org.firstinspires.ftc.teamcode.subsystems.BallSensors;
import org.firstinspires.ftc.teamcode.subsystems.PollenSensor;
import org.firstinspires.ftc.teamcode.subsystems.PoseRecorder;
import org.firstinspires.ftc.teamcode.subsystems.SafeModeKey;
import org.firstinspires.ftc.teamcode.subsystems.TestLog;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.subsystems.VisionSelector;
import org.firstinspires.ftc.teamcode.subsystems.Vision_None;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;
import org.firstinspires.ftc.teamcode.types.HiveUp;
import org.firstinspires.ftc.teamcode.types.TagLook;

/**
 * This season's robot, as one object.
 *
 * <p>An OpMode does not build motors or subsystems. It builds a Bot, then tells the Bot
 * what to do. The Bot knows which parts the robot has and passes each call on to them.
 *
 * <pre>
 *     BotProperties properties = new BotProperties();
 *     Bot bot = new Bot(this, properties);
 *     bot.Init();
 *
 *     while (opModeInInit()) {        // waiting for START
 *         bot.StartLoop();
 *         bot.ShowStatus();
 *         bot.EndLoop();
 *     }
 *
 *     while (opModeIsActive()) {      // START was pressed
 *         bot.StartLoop();
 *         bot.HandleUserInput();      // TeleOp only. An auto leaves this line out.
 *         bot.Process();
 *         bot.EndLoop();
 *     }
 * </pre>
 *
 * <p>Build the Bot inside {@code runOpMode()}, not on the line where a field is declared.
 * Before {@code runOpMode()} starts, the hardware map is still empty.
 *
 * <p>To leave a part of the robot out of one OpMode, switch it off in
 * {@link BotProperties} before building the Bot. See docs/decisions/0010.
 *
 * <p>The operator holds a bumper in TeleOp and the drive turns to face the middle of our
 * hive's AprilTags, offset for that launcher. Which camera is decided from the robot
 * configuration, like the heading.
 *
 * <p><b>Who drives what in TeleOp</b> (docs/decisions/0023):
 * <ul>
 *   <li><b>Gamepad 1, driver</b>: sticks drive (field-centric), left bumper slow, left
 *       trigger (hold) drift assist, circle resets forward, square wiggle on / off, D-pad
 *       left / right while the operator aims arcs around the hive (aimArc, off).
 *   <li><b>Gamepad 2, operator</b>: LEFT bumper (hold) aims pollen, LEFT trigger (hold)
 *       launches pollen; RIGHT bumper (hold) aims nectar, RIGHT trigger (hold) launches
 *       nectar; both sides at once, pollen wins. A launch trigger aims too, and the
 *       intake and transfer push balls in once the gate is open. Cross (hold) runs the
 *       intake and transfer while held, square (hold) runs them backward, slowly, triangle
 *       (hold) deploys the flower extractor, D-pad trims the launcher speeds (both
 *       follow the tag's distance), circle flips always-track. The launchers spin from
 *       START to the end with no button.
 * </ul>
 *
 * <p><b>Adding a new part</b> (an intake, say): write its {@link Subsystem} class, give it
 * a "disabled" setting in BotProperties, and build it in the constructor below the same
 * way the launcher is built.
 */
public class Bot {
    private final LinearOpMode mLinearOpMode;
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    /** Every part that is switched on, in the order it runs each loop. */
    private final List<Subsystem> mSystems = new ArrayList<>();
    private final List<LynxModule> mAllHubs;
    private final ElapsedTime mLoopTimer;
    /** The hubs' battery readers. Empty in unit tests. */
    private final List<VoltageSensor> mBatterySensors;
    private final ElapsedTime mBatteryTimer;
    private double mBatteryVolts = 0;
    /** Loop time smoothed over about the last 50 loops, so two OpModes can be compared. */
    private double mLoopTimeAverage = 0;
    /** true when INIT kept the heading the auto left, instead of zeroing it (0046). */
    private boolean mHeadingFromAuto = false;
    /** The heading the auto handed over, in degrees. NaN when there was none. */
    private double mYawFromAuto = Double.NaN;

    // The parts of the robot. A part that is disabled in BotProperties stays null.
    private final SafeModeKey mSafeModeKey;
    private final AllianceSensor mAllianceSensor;
    private final Vision mVision;
    private final Imu mImu;
    private final Launcher mLauncher;
    private final Intake mIntake;
    private final LaunchGates mLaunchGates;
    private final Indicator mIndicator;
    private final LightStrip mLightStrip;
    private final FlowerExtractor mFlowerExtractor;
    private final BallSensors mBallSensors;

    /**
     * The settings that show on FTC Dashboard's Config tab, to change while the robot runs
     * (docs/decisions/0022). Each list is one group on the dashboard. To add a setting, add
     * its name from BotProperties here. Changes there are lost at STOP: write the winner
     * into BotProperties.
     */
    static final String[] LiveTuningDrive = {
            "driveNormal", "driveSlow", "driveStrafeGain", "driveMotorSlewRate",
            "driveSwappedTurnGain", "driveTightTurnGain", "driveDriftSideGain", "driveDriftRearGain",
            "aimKP", "aimMaxRotate", "aimDoneDegrees", "aimHiveBearingDeg", "launcherSideOffsetIn", "launcherOffsetNoRangeDeg", "pollenLeftOfCamera", "nectarAimLeftDeg", "aimSideCenterDeg", "aimSideReversed",
            "aimArc", "arcStrafePower", "arcRangeKP", "arcMaxDrive",
            "launchHoldsPosition", "launchHoldKP", "launchHoldMaxPower", "launchHoldDoneIn",
            "launchHoldGiveUpIn", "launchHoldStickDeadband",
            "testLog", "testTelemetry", "tuningMarks", "launchWiggle", "wiggleDeg", "wiggleSwitchSeconds", "wiggleKP", "wiggleMaxRotate"};
    /** The Telemetry group on the dashboard: switch each block of Driver Station lines on or off. */
    static final String[] LiveTuningTelemetry = {
            "quietTelemetry", "launcherOnlyTelemetry", "stickTelemetry",
            "tlmAutoStep", "tlmAutoLaunch", "tlmPose", "tlmLauncher", "tlmVision", "tlmDrive",
            "tlmIntake", "tlmLights"};
    static final String[] LiveTuningLauncher = {
            "pollenLauncherRpm", "nectarLauncherRpm", "launcherRangeTrimRpm", "nectarRangeTrimRpm",
            "launchNearRpm", "launchMid1Rpm", "launchMid2Rpm", "launchFarRpm",
            "nectarNearRpm", "nectarMid1Rpm", "nectarMid2Rpm", "nectarFarRpm", "launchBandIn",
            "steadyRange", "steadyRangeAverage", "launchBandHoldIn",
            "launchLocksRpm", "rangeAlwaysTracks", "rangeNeedsTipped", "tagTiltFromRoll", "tagTippedMinDeg", "tagTippedMaxDeg", "launcherKF", "launcherKP", "launcherKI",
            "launcherAtSpeedRpm", "launcherWrongWayRpm", "launcherWrongWaySeconds",
            "launchServoSecondsPer60Deg", "launchServoExtraSeconds", "launchNeedsLock", "operatorSwapsLaunchers", "launchBothTogether", "teleopNectarSettles", "bothAimOffsetDeg",
            "teleopLaunchSettles", "autoLaunchSettles", "autoTagLaunchSettles", "launchSettleBandRpm", "launchSettleHoldSeconds",
            "launchSettleTimeoutSeconds"};
    static final String[] LiveTuningIntake = {
            "intakeRpm", "transferRpm", "launchFeedIntakeScale", "launchFeedTransferScale",
            "intakeReverseRpm", "transferReverseRpm", "pollenAutoReject", "rollerUsePower",
            "rollerKP", "rollerKI", "rollerKD", "rollerKF"};

    /**
     * Build the robot. Nothing moves.
     *
     * @param opMode     the OpMode that is running. Inside an OpMode, pass {@code this}.
     * @param properties the settings for this OpMode
     */
    public Bot(LinearOpMode opMode, BotProperties properties) {
        mLinearOpMode = opMode;
        mProperties = properties;
        // A match build turns the test and tuning settings off, whatever was set (0053).
        mProperties.ApplyMatchBuild(BuildConfig.MATCH_BUILD);
        Telemetry telemetry = opMode.telemetry;
        // At an event, nothing goes to the dashboard (rule R704.D, docs/decisions/0022).
        if (!BotProperties.competitionMode) {
            telemetry = WithDashboard(opMode.telemetry);
            DashboardTuning.Show("Drive", mProperties, LiveTuningDrive);
            DashboardTuning.Show("Launcher", mProperties, LiveTuningLauncher);
            DashboardTuning.Show("Intake", mProperties, LiveTuningIntake);
            DashboardTuning.Show("Telemetry", mProperties, LiveTuningTelemetry);
        }
        mOp = new HydraOpMode(telemetry, opMode.hardwareMap, opMode.gamepad1, opMode.gamepad2);
        mOp.mProperties = mProperties;

        // The Safe Mode key is checked here, one time, before any other part is built. No
        // OpMode can forget it, and it is never read again (docs/decisions/0007).
        mSafeModeKey = new SafeModeKey(mOp, mProperties);
        mSystems.add(mSafeModeKey);

        // The alliance next: the camera needs it to know which hive's tags are ours
        // (docs/decisions/0021).
        mAllianceSensor = new AllianceSensor(mOp, mProperties);
        mSystems.add(mAllianceSensor);

        // The camera goes before the drive in the list, so each loop the drive aims at what
        // the camera saw this loop, not last loop.
        if (mProperties.visionDisabled) {
            // The drive still needs something to ask. This one never sees a tag.
            mVision = new Vision_None(mOp, mProperties);
        } else {
            mVision = VisionSelector.Pick(mOp, mProperties);
            mSystems.add(mVision);
        }

        if (mProperties.driveDisabled) {
            mImu = null;
        } else {
            mImu = ImuSelector.Pick(mOp, mProperties);
            mSystems.add(mImu);
            mSystems.add(new Drive_Manual(mOp, mProperties, mImu, mVision));
        }

        // The operator (gamepad 2) runs the launchers, the intake, and aims and launches each
        // launcher: the one who aims is the one who shoots. The driver (gamepad 1) only drives.
        if (mProperties.launcherDisabled) {
            mLauncher = null;
        } else {
            // The launcher gets the camera: the pollen wheel's speed follows the distance to the tag.
            mLauncher = new Launcher(mOp, mProperties, mOp.mOperatorGamepad, mVision);
            mSystems.add(mLauncher);
        }
        if (mProperties.launchGatesDisabled) {
            mLaunchGates = null;
        } else {
            // The gates get the camera, so each launch notes how far the tag was.
            mLaunchGates = new LaunchGates(mOp, mProperties, mOp.mOperatorGamepad, mVision);
            mSystems.add(mLaunchGates);
            // The launcher holds its speed while the gates are open (launchLocksRpm, 0050).
            if (mLauncher != null) {
                mLauncher.SetLaunchGates(mLaunchGates);
                // And the trigger settles on the speed before the gates open (0056).
                mLaunchGates.SetLauncher(mLauncher);
            }
        }
        // The flower extractor is the operator's too: hold triangle and it swings out.
        if (mProperties.flowerExtractorDisabled) {
            mFlowerExtractor = null;
        } else {
            mFlowerExtractor = new FlowerExtractor(mOp, mProperties, mOp.mOperatorGamepad);
            mSystems.add(mFlowerExtractor);
        }
        // The pollen sensor: the intake rejects extra balls with it, the strip turns yellow
        // with it. Built once if either needs it. A robot without the switch reads "no pollen".
        PollenSensor pollenSensor = null;
        if (!mProperties.intakeDisabled || !mProperties.stripDisabled) {
            pollenSensor = new PollenSensor(mOp, mProperties);
            mSystems.add(pollenSensor);
        }

        // The ball switches (pollenSensor1.., nectorSensor1..): an auto's LAUNCH_UNTIL_EMPTY
        // waits on them. Missing ones are ignored.
        mBallSensors = new BallSensors(mOp, mProperties);
        mSystems.add(mBallSensors);

        // The intake is handed the gates: once they are open, both rollers push balls in.
        // With the gates switched off it gets null, and the transfer just never does that.
        // It also gets the pollen sensor, to spit out an extra ball (0040).
        if (mProperties.intakeDisabled) {
            mIntake = null;
        } else {
            mIntake = new Intake(mOp, mProperties, mOp.mOperatorGamepad, mLaunchGates, pollenSensor);
            mSystems.add(mIntake);
        }

        // The light shows what the camera sees, so it goes after the camera in the list.
        if (mProperties.indicatorDisabled) {
            mIndicator = null;
        } else {
            mIndicator = new Indicator(mOp, mProperties, mVision);
            mSystems.add(mIndicator);
        }

        // The headlight helps the camera, so it turns on at INIT with everything else.
        if (!mProperties.headlightDisabled) {
            mSystems.add(new Headlight(mOp, mProperties));
        }

        // The LED strip reads no gamepad: the alliance underglow and the pollen yellow run
        // by themselves (0039).
        if (mProperties.stripDisabled) {
            mLightStrip = null;
        } else {
            // The strip turns LEDs yellow while the pollen sensor is pressed.
            mLightStrip = new LightStrip(mOp, mProperties, pollenSensor);
            mSystems.add(mLightStrip);
        }

        // The motor tuner shares the launcher's motor and gamepad 1's D-pad. Only
        // Test: Motor Tuning turns it on, with the launcher and the drive off.
        if (!mProperties.motorTunerDisabled) {
            mSystems.add(new MotorTuner(mOp, mProperties, mOp.mDriverGamepad));
        }
        // Same D-pad again. Only Test: Launch Servo Tuning turns it on.
        if (!mProperties.launchServoTunerDisabled) {
            mSystems.add(new LaunchServoTuner(mOp, mProperties, mOp.mDriverGamepad));
        }
        // Gamepad 1's face buttons. Only Tool: Pose Recorder turns it on. It reads where the
        // robot is the same way Pedro does, so the spots it saves match the autos' (0024).
        if (!mProperties.poseRecorderDisabled) {
            File folder = new File(AppUtil.FIRST_FOLDER, "recordings");
            // It also gets the parts it blips to show a recorded step (0055).
            mSystems.add(new PoseRecorder(mOp, mProperties, mOp.mDriverGamepad,
                    PedroConstants.CreateLocalizer(mOp.mHardwareMap, mProperties), folder,
                    mIntake, mFlowerExtractor, mIndicator));
        }
        // Last, so it sees what every other part did this loop. It does nothing unless
        // testLog or testTelemetry is on (docs/decisions/0050).
        mSystems.add(new TestLog(mOp, mProperties, mVision, mLauncher, mLaunchGates, mIntake, mImu, null));

        // Read every hub's sensors in one trip per loop instead of one trip per sensor.
        // StartLoop() throws the old readings away, so each loop reads fresh ones.
        mAllHubs = mOp.mHardwareMap.getAll(LynxModule.class);
        for (LynxModule hub : mAllHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.MANUAL);
        }

        mLoopTimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        mBatterySensors = mOp.mHardwareMap.getAll(VoltageSensor.class);
        mBatteryTimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        ReadBattery();
    }

    /**
     * Read the battery voltage. With two hubs there are two readings of the same battery;
     * the lowest one is shown, since a sagging battery is what we want to see.
     */
    private void ReadBattery() {
        double lowest = Double.MAX_VALUE;
        for (VoltageSensor sensor : mBatterySensors) {
            double volts = sensor.getVoltage();
            if (volts > 0 && volts < lowest) {
                lowest = volts;
            }
        }
        if (lowest != Double.MAX_VALUE) {
            mBatteryVolts = lowest;
        }
        mBatteryTimer.reset();
    }

    /**
     * The Driver Station's telemetry, plus the FTC Dashboard's while the dashboard is
     * running. Every addData line then also draws a live graph on a laptop at
     * http://192.168.43.1:8080/dash (docs/decisions/0012). The dashboard is not running in
     * unit tests, and can be switched off in the Robot Controller app's menu; then the
     * Driver Station gets the telemetry alone, as before.
     */
    static Telemetry WithDashboard(Telemetry driverStation) {
        FtcDashboard dashboard = FtcDashboard.getInstance();
        if (dashboard == null) {
            return driverStation;
        }
        return new MultipleTelemetry(driverStation, dashboard.getTelemetry());
    }

    /**
     * Get every part ready, without moving. Waits here until they all say ready, or until
     * START or STOP is pressed. Call once, right after building the Bot.
     */
    public void Init() {
        boolean everyPartReady;
        do {
            StartLoop();
            everyPartReady = true;
            for (Subsystem system : mSystems) {
                if (!system.Init()) {
                    everyPartReady = false;
                }
            }
            EndLoop();
        } while (!everyPartReady && mLinearOpMode.opModeInInit());

        KeepHeadingFromAuto();
    }

    /**
     * Right after an auto, keep the heading it left, so "forward" is still away from the
     * driver (docs/decisions/0046). Only in an OpMode that drives, once the heading is ready.
     * The auto's heading is taken either way, so it is never used twice.
     */
    private void KeepHeadingFromAuto() {
        double yawFromAuto = HeadingHandoff.Take(HeadingHandoff.NowSeconds(),
                mProperties.headingHandoffMaxSeconds);
        if (mImu == null || !mImu.Ready() || !mProperties.headingFromAuto) {
            return;
        }
        if (!Double.isNaN(yawFromAuto)) {
            mImu.SetYaw(yawFromAuto);
            mHeadingFromAuto = true;
            mYawFromAuto = yawFromAuto;
        }
    }

    /** true when INIT kept the heading the auto left. */
    public boolean HeadingFromAuto() {
        return mHeadingFromAuto;
    }

    /**
     * Put each part's status on the Driver Station. Call every loop while waiting for
     * START, so the drive team can check the robot before the match.
     */
    public void ShowStatus() {
        // INIT always shows everything: the drive team's checks come from this screen.
        mOp.mQuiet = false;
        ShowCompetitionStatus();
        // The key was checked once, when the Bot was built. This only shows the answer.
        mSafeModeKey.ShowStatus();
        // The alliance is read again every loop until START, so the magnet can be moved.
        mAllianceSensor.ShowStatus();

        if (mProperties.driveDisabled) {
            mOp.mTelemetry.addLine("Driver control: OFF in this OpMode");
        } else {
            // Keep reading the heading, so the driver can see it change before START.
            mImu.Process();
            mOp.mTelemetry.addData("Heading ready", mImu.Ready());
            mOp.mTelemetry.addData("Yaw", mImu.GetYaw());
            if (mHeadingFromAuto) {
                mOp.mTelemetry.addLine("Forward: kept from the auto. Do not turn the robot. Circle re-sets it.");
                // 0 = facing away from the driver, 90 = to the driver's left. Compare it with
                // the way the robot really faces: half a turn off = the auto's StartFacingDeg.
                mOp.mTelemetry.addData("Yaw the auto handed over", "%.0f", mYawFromAuto);
            } else {
                mOp.mTelemetry.addLine("Forward: the way the robot faces now");
            }
        }

        if (mProperties.launcherDisabled) {
            mOp.mTelemetry.addLine("Launchers: OFF in this OpMode");
        } else {
            mOp.mTelemetry.addData("Launcher target RPM (pollen / nectar)", "%.0f / %.0f",
                    mLauncher.GetPollenTargetRpm(), mLauncher.GetNectarTargetRpm());
        }
        if (mProperties.intakeDisabled) {
            mOp.mTelemetry.addLine("Intake and transfer: OFF in this OpMode");
        }
        if (mProperties.launchGatesDisabled) {
            mOp.mTelemetry.addLine("Launch gates: OFF in this OpMode");
        }
        if (mProperties.flowerExtractorDisabled) {
            mOp.mTelemetry.addLine("Flower extractor: OFF in this OpMode");
        }

        if (mProperties.visionDisabled) {
            mOp.mTelemetry.addLine("Vision: OFF in this OpMode");
        } else {
            // Keep looking, so the camera can be checked against a tag before START.
            mVision.Process();
        }
        if (mIndicator != null) {
            // The light follows the camera before START too, so the drive team can point the
            // robot at our tags and see that it picks them up (blinking red = wrong alliance).
            mIndicator.Process();
        }
        if (mLightStrip != null) {
            mLightStrip.ShowStatus();
        }
    }

    /**
     * Competition or practice, said at the top of the Driver Station before every match.
     * At an event the dashboard must be off (rule R704.D). The code stops sending to it, but
     * the dashboard itself can only be switched off in the Robot Controller app's menu.
     */
    private void ShowCompetitionStatus() {
        FtcDashboard dashboard = FtcDashboard.getInstance();
        boolean dashboardOn = dashboard != null && dashboard.isEnabled();
        mOp.mTelemetry.addLine(CompetitionStatus(BotProperties.competitionMode, dashboardOn));
        mOp.mTelemetry.addLine(BuildStatus(BuildConfig.MATCH_BUILD));
    }

    /** Which build is on the robot (docs/decisions/0053). No hardware, so a test can check it. */
    static String BuildStatus(boolean matchBuild) {
        if (matchBuild) {
            return "Match build: only the match OpModes are installed.";
        }
        return "Full build: test and tool OpModes installed. For events, install the match build (matchBuild=true).";
    }

    /** The competition line for the Driver Station. No hardware, so a test can check it. */
    static String CompetitionStatus(boolean competitionMode, boolean dashboardOn) {
        if (!competitionMode) {
            return "PRACTICE MODE: dashboard and live tuning on. Set competitionMode = true "
                    + "in BotProperties before an event.";
        }
        if (dashboardOn) {
            return "DASHBOARD IS STILL ON (rule R704.D). Robot Controller app: menu, "
                    + "Disable Dashboard. Then INIT again.";
        }
        return "Competition mode: dashboard off.";
    }

    /** First line of every loop. Throws away last loop's sensor readings and times the loop. */
    public void StartLoop() {
        for (LynxModule hub : mAllHubs) {
            hub.clearBulkCache();
        }
        mOp.mLoopTime = mLoopTimer.milliseconds();
        mLoopTimer.reset();
    }

    /** Every part reads the gamepads. TeleOp calls this each loop, before {@link #Process()}. */
    public void HandleUserInput() {
        mOp.mQuiet = Quiet(mProperties);
        for (Subsystem system : mSystems) {
            system.HandleUserInput();
        }
    }

    /** Every part does its work for one loop. TeleOp and Auto both call this each loop. */
    public void Process() {
        mOp.mQuiet = Quiet(mProperties);
        mLoopTimeAverage = mLoopTimeAverage + (mOp.mLoopTime - mLoopTimeAverage) / 50;
        if (mProperties.launcherOnlyTelemetry) {
            mOp.mTelemetry.addLine("LAUNCHER TELEMETRY: loop time, battery and launchers only (launcherOnlyTelemetry)");
        } else if (mOp.mQuiet) {
            mOp.mTelemetry.addLine("QUIET TELEMETRY: only loop time and battery (quietTelemetry)");
        }
        mOp.mTelemetry.addData("LoopTime", mOp.mLoopTime);
        mOp.mTelemetry.addData("LoopTime average (ms)", "%.1f", mLoopTimeAverage);
        if (mBatteryTimer.milliseconds() >= mProperties.batteryReadMs) {
            ReadBattery();
        }
        // Graph this while the launchers spin up: a weak battery dips here first.
        mOp.mTelemetry.addData("Battery volts", "%.2f", mBatteryVolts);
        for (Subsystem system : mSystems) {
            system.Process();
        }
    }

    /**
     * true when the parts should skip their Driver Station lines: quietTelemetry, or
     * launcherOnlyTelemetry (the launchers still show theirs, see Launcher).
     */
    static boolean Quiet(BotProperties properties) {
        return properties.quietTelemetry || properties.launcherOnlyTelemetry;
    }

    /** Last line of every loop. Sends this loop's telemetry to the Driver Station. */
    public void EndLoop() {
        mOp.mTelemetry.update();
        mLinearOpMode.idle();
    }

    /**
     * true: blink the indicator light yellow, so the drive team can see a launch happen.
     * false: turn it off. An auto calls this at a LAUNCH step. Does nothing with no light.
     */
    public void ShowLaunching(boolean launching) {
        if (mIndicator == null) {
            return;
        }
        mIndicator.SetLaunching(launching);
    }

    /**
     * true: blink the indicator light orange, so the drive team can see the robot is waiting
     * for a tag. false: turn it off. Does nothing with no light.
     */
    public void ShowWaitingForTag(boolean waiting) {
        if (mIndicator == null) {
            return;
        }
        mIndicator.SetWaitingForTag(waiting);
    }

    /** true: both launchers spin at their targets. false: off. Does nothing with no launcher. */
    public void SpinLaunchers(boolean spin) {
        if (mLauncher == null) {
            return;
        }
        mLauncher.SetSpinning(spin);
    }

    /** true when both launchers are at speed. Always true with no launcher, so nothing waits on it. */
    /**
     * true when the pollen launcher is spinning at least this fast. Always true with no
     * launcher, or no pollen wheel in the configuration, so nothing waits on a missing part.
     */
    public boolean PollenLauncherAtLeast(double rpm) {
        if (mLauncher == null) {
            return true;
        }
        return mLauncher.PollenAtLeast(rpm);
    }

    /** The pollen launcher's measured speed, in RPM. 0 with no launcher. */
    public double PollenLauncherRpm() {
        if (mLauncher == null) {
            return 0;
        }
        return mLauncher.GetPollenMeasuredRpm();
    }

    public boolean LaunchersAtSpeed() {
        if (mLauncher == null) {
            return true;
        }
        return mLauncher.AtSpeed();
    }

    /**
     * The auto's launch command (autoLaunchSettles, 0056): lock the pollen speed and start
     * waiting for it to settle. Does nothing with no launcher.
     */
    public void BeginLaunchSettle() {
        if (mLauncher != null) {
            mLauncher.BeginSettle();
        }
    }

    /** true once the pollen speed has settled, or the wait timed out. Always true with no launcher. */
    public boolean LaunchSettled() {
        if (mLauncher == null) {
            return true;
        }
        return mLauncher.SettleDone();
    }

    /** Stop waiting to settle and unlock the speed. Does nothing with no launcher. */
    public void CancelLaunchSettle() {
        if (mLauncher != null) {
            mLauncher.CancelSettle();
        }
    }

    /** Settling, settled or timed out, for the Driver Station. */
    public String LaunchSettleStatus() {
        if (mLauncher == null) {
            return "no launcher";
        }
        return mLauncher.SettleStatus();
    }

    /** true: open both launch gates (launch!). false: close them. Does nothing with no gates. */
    public void OpenLaunchGates(boolean open) {
        if (mLaunchGates == null) {
            return;
        }
        mLaunchGates.SetOpen(open);
    }

    /**
     * Open (or close) just the gates asked for: the auto's LAUNCH_NECTAR opens only the
     * nectar gate. Does nothing with no gates.
     */
    public void OpenLaunchGates(boolean open, boolean pollen, boolean nectar) {
        if (mLaunchGates == null) {
            return;
        }
        mLaunchGates.SetOpen(open, pollen, nectar);
    }

    /** true when the nectar launcher is spinning at least this fast. Always true with no launcher or nectar wheel. */
    public boolean NectarLauncherAtLeast(double rpm) {
        if (mLauncher == null) {
            return true;
        }
        return mLauncher.NectarAtLeast(rpm);
    }

    /** The nectar launcher's measured speed, in RPM. 0 with no launcher. */
    public double NectarLauncherRpm() {
        if (mLauncher == null) {
            return 0;
        }
        return mLauncher.GetNectarMeasuredRpm();
    }

    /** The auto's nectar launch command: lock the nectar speed and wait for it to settle. */
    public void BeginNectarLaunchSettle() {
        if (mLauncher != null) {
            mLauncher.BeginNectarSettle();
        }
    }

    /** true once the nectar speed has settled, or the wait timed out. Always true with no launcher. */
    public boolean NectarLaunchSettled() {
        if (mLauncher == null) {
            return true;
        }
        return mLauncher.NectarSettleDone();
    }

    /** Stop waiting on the nectar wheel and unlock its speed. */
    public void CancelNectarLaunchSettle() {
        if (mLauncher != null) {
            mLauncher.CancelNectarSettle();
        }
    }

    /** The nectar wheel settling, for the Driver Station. */
    public String NectarLaunchSettleStatus() {
        if (mLauncher == null) {
            return "no launcher";
        }
        return mLauncher.NectarSettleStatus();
    }

    /**
     * Is this telemetry group switched on? An auto asks this for its own lines. The quiet
     * settings are not checked: an auto's screen is not what slows a TeleOp loop.
     */
    public boolean TelemetryOn(TelemetryGroup group) {
        return mProperties.TelemetryOn(group);
    }

    /** true when at least one ball switch is in the robot configuration. */
    public boolean BallSensorsFound() {
        return mBallSensors.AnyFound();
    }

    /** How long every ball switch has read empty, in a row, in seconds. 0 while any shows a ball. */
    public double BallsEmptyForSeconds() {
        return mBallSensors.EmptyForSeconds();
    }

    /** How many ball switches show a ball right now. */
    public int BallsSeen() {
        return mBallSensors.BallsSeen();
    }

    /**
     * true: the flower extractor swings out (deployed). false: back in (stored). An auto's
     * FLOWER step calls this. Does nothing with no extractor.
     */
    public void SetFlowerExtractor(boolean deployed) {
        if (mFlowerExtractor == null) {
            return;
        }
        mFlowerExtractor.SetDeployed(deployed);
    }

    /**
     * true: the intake roller runs along with the transfer. false: the transfer alone. An
     * auto turns the roller on only for FLOWER steps. Does nothing with no intake.
     */
    public void SetIntakeRoller(boolean on) {
        if (mIntake == null) {
            return;
        }
        mIntake.SetIntakeRollerOn(on);
    }

    /** true: intake and transfer run forward. false: off. Does nothing with no intake. */
    public void RunIntake(boolean run) {
        if (mIntake == null) {
            return;
        }
        mIntake.SetOn(run);
    }

    /** @return true when the camera sees one of our hive's AprilTags. Always false with vision switched off. */
    public boolean SeesTag() {
        return mVision.SeesTag();
    }

    /**
     * Has our hive tipped up? true when the camera sees one of our hive's tags and, with
     * {@code autoTippedNeedsTilt} on, the tag's roll is in the UP window
     * ({@code autoTagUpMinDeg} to {@code autoTagUpMaxDeg}, measured on 2026-11-24).
     * An auto's CHECK_TIPPED step asks this (docs/decisions/0057).
     */
    public boolean SeesTippedHive() {
        if (!mVision.SeesTag()) {
            return false;
        }
        if (!mProperties.autoTippedNeedsTilt) {
            return true;
        }
        return HiveUp.InWindow(mVision.GetTagFlipDegrees(),
                mProperties.autoTagUpMinDeg, mProperties.autoTagUpMaxDeg);
    }

    /**
     * Which of our hive's cells has tipped up, from the nearest tag of ours. A tipped tag
     * ({@link #SeesTippedHive()}) means its own side is up: the garden side's or the flower
     * side's ({@code gardenSideTags}, {@code flowerSideTags}). With
     * {@code autoDownMeansOtherSideUp} on, a tag in the DOWN window is a cell that is down, so
     * the OTHER side is up. NOTHING when the tag says neither, or no tag is seen.
     */
    public HiveUp SeesHiveUp() {
        if (!mVision.SeesTag()) {
            return HiveUp.NOTHING;
        }
        boolean down = mProperties.autoDownMeansOtherSideUp
                && HiveUp.InWindow(mVision.GetTagFlipDegrees(),
                        mProperties.autoTagDownMinDeg, mProperties.autoTagDownMaxDeg);
        return HiveUp.FromLook(SeesTippedHive(), down, mVision.GetTagId(),
                mProperties.gardenSideTags, mProperties.flowerSideTags);
    }

    /** Every tag of ours in the newest picture, each with its own angles (Test: Tip Detection). */
    public List<TagLook> TagsInView() {
        return mVision.GetTagsInView();
    }

    /** The nearest tag's angle that CHECK_TIPPED reads, degrees. NaN with no tag. */
    public double TipAngleDegrees() {
        return mVision.GetTagFlipDegrees();
    }

    /** The nearest tag's id, for the Driver Station. */
    public int GetTagId() {
        return mVision.GetTagId();
    }

    /**
     * @return degrees from straight ahead to the middle of the nearest tag's group (a hive's
     *     tags). Left is positive. 0 with no tag.
     */
    public double GetTagGroupBearing() {
        return mVision.GetGroupBearing();
    }

    /** Which side of the hive the robot stands, in degrees, left positive. 0 = not known. */
    public double GetHiveSideDegrees() {
        return mVision.GetSideDegrees();
    }

    /** Inches from the camera to the nearest tag of ours. 0 = no tag, or no distance. */
    public double GetTagRange() {
        return mVision.GetRange();
    }

    /**
     * Build Pedro Pathing's Follower, the thing that drives the robot along a path in an
     * auto. Safe Mode applies to it too.
     *
     * <p>Only one thing may drive the wheels. An OpMode that uses the Follower sets
     * {@code driveDisabled = true}, so driver control is not fighting it for the motors.
     */
    public Follower CreateFollower() {
        return PedroConstants.CreateFollower(mOp.mHardwareMap, mProperties, mOp.mSafeMode);
    }
}
