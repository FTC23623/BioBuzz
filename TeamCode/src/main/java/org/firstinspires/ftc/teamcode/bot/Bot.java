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
import org.firstinspires.ftc.teamcode.subsystems.PollenSensor;
import org.firstinspires.ftc.teamcode.subsystems.PoseRecorder;
import org.firstinspires.ftc.teamcode.subsystems.SafeModeKey;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.subsystems.VisionSelector;
import org.firstinspires.ftc.teamcode.subsystems.Vision_None;

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
 * <p>Hold the right bumper in TeleOp and the drive turns to face the middle of our hive's
 * AprilTags. Which camera is decided from the robot configuration, like the heading.
 *
 * <p><b>Who drives what in TeleOp</b> (docs/decisions/0023):
 * <ul>
 *   <li><b>Gamepad 1, driver</b>: sticks drive (field-centric), left bumper slow, right
 *       bumper aim, D-pad left / right while aiming arcs around the hive, RIGHT TRIGGER
 *       (hold) opens the launch gates once the camera is locked on our hive, then runs the
 *       intake and transfer to push balls in. Left trigger (hold) is drift assist.
 *   <li><b>Gamepad 2, operator</b>: the launchers spin from START to the end with no
 *       button, D-pad trims their speeds (pollen follows the tag's distance), left bumper
 *       runs the intake and transfer (on / off),
 *       left trigger (hold) runs them while held, square (hold) runs them backward, slowly,
 *       triangle (hold) deploys the flower extractor, right trigger (hold) lets a launch
 *       through without the camera lock.
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

    /**
     * The settings that show on FTC Dashboard's Config tab, to change while the robot runs
     * (docs/decisions/0022). Each list is one group on the dashboard. To add a setting, add
     * its name from BotProperties here. Changes there are lost at STOP: write the winner
     * into BotProperties.
     */
    static final String[] LiveTuningDrive = {
            "driveNormal", "driveSlow", "driveStrafeGain", "driveMotorSlewRate",
            "driveSwappedTurnGain", "driveTightTurnGain", "driveDriftSideGain", "driveDriftRearGain",
            "aimKP", "aimMaxRotate", "aimDoneDegrees", "aimHiveBearingDeg", "aimSideCenterDeg", "aimSideReversed",
            "arcStrafePower", "arcRangeKP", "arcMaxDrive",
            "wiggleDeg", "wiggleSwitchSeconds", "wiggleKP", "wiggleMaxRotate",
            "quietTelemetry", "launcherOnlyTelemetry"};
    static final String[] LiveTuningLauncher = {
            "pollenLauncherRpm", "nectarLauncherRpm", "launcherRangeTrimRpm",
            "launchNearRpm", "launchFarRpm", "launchCurve", "launchBandIn",
            "steadyRange", "steadyRangeAverage", "launchBandHoldIn", "launcherKF", "launcherKP", "launcherKI",
            "launcherAtSpeedRpm", "launcherWrongWayRpm", "launcherWrongWaySeconds",
            "launchServoSecondsPer60Deg", "launchServoExtraSeconds", "launchNeedsLock"};
    static final String[] LiveTuningIntake = {
            "intakeRpm", "transferRpm", "launchFeedIntakeScale", "launchFeedTransferScale",
            "intakeReverseRpm", "transferReverseRpm",
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
        Telemetry telemetry = opMode.telemetry;
        // At an event, nothing goes to the dashboard (rule R704.D, docs/decisions/0022).
        if (!BotProperties.competitionMode) {
            telemetry = WithDashboard(opMode.telemetry);
            DashboardTuning.Show("Drive", mProperties, LiveTuningDrive);
            DashboardTuning.Show("Launcher", mProperties, LiveTuningLauncher);
            DashboardTuning.Show("Intake", mProperties, LiveTuningIntake);
        }
        mOp = new HydraOpMode(telemetry, opMode.hardwareMap, opMode.gamepad1, opMode.gamepad2);

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

        // The operator (gamepad 2) runs the launchers and the intake. The driver (gamepad 1)
        // opens the launch gates, so the one who aims is the one who shoots (0023).
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
            mLaunchGates = new LaunchGates(mOp, mProperties, mOp.mDriverGamepad, mVision);
            mSystems.add(mLaunchGates);
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
            mSystems.add(new PoseRecorder(mOp, mProperties, mOp.mDriverGamepad,
                    PedroConstants.CreateLocalizer(mOp.mHardwareMap, mProperties), folder));
        }

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

    /** true: open both launch gates (launch!). false: close them. Does nothing with no gates. */
    public void OpenLaunchGates(boolean open) {
        if (mLaunchGates == null) {
            return;
        }
        mLaunchGates.SetOpen(open);
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
