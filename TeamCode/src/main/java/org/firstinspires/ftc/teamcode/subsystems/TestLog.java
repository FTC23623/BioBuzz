package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.Gamepad;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * For testing: what the drivers pressed and what the robot measured, every loop
 * (docs/decisions/0050). Two switches in BotProperties, both off for matches:
 * <ul>
 *   <li><b>testLog</b>: one line per loop in a CSV file on the hub, in
 *       {@code /sdcard/FIRST/logs/}. Open it in a spreadsheet to see, say, the distance and
 *       the transfer's speed in the moment the launch trigger was pulled.
 *   <li><b>testTelemetry</b>: the same numbers on the Driver Station, even with quiet or
 *       launcher-only telemetry on.
 * </ul>
 *
 * <p>The columns: time and loop time; each button that does something (1 held, 0 not), by
 * what it does; the switches the buttons flip; the camera's distances and tilt; the
 * launchers' and rollers' target and measured RPM, and the rollers' power.
 *
 * <p>The Bot runs this last each loop, so it sees what every other part did this loop.
 * A part that is switched off writes 0.
 */
public class TestLog implements Subsystem {
    /** Write the file out to the hub's storage every this many lines. */
    static final int linesPerFlush = 10;

    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final Vision mVision;
    private final Launcher mLauncher;
    private final LaunchGates mLaunchGates;
    private final Intake mIntake;
    private final File mFolder;

    private BufferedWriter mWriter = null;
    private File mFile = null;
    private int mLinesSinceFlush = 0;
    private long mStartNanos = 0;
    private String mFileStatus = "off (testLog)";

    // This loop's columns, names and values in the same order. Filled by Collect().
    private final List<String> mNames = new ArrayList<>();
    private final List<String> mValues = new ArrayList<>();
    // Where each gamepad's buttons are in those lists, for the Driver Station.
    private int mDriverFirst = 0;
    private int mOperatorFirst = 0;
    private int mButtonsEnd = 0;

    /**
     * @param folder where the log files go. null (what the Bot passes) means
     *               /sdcard/FIRST/logs, looked up only when the first file opens.
     *               Any part may be null: it is switched off, and its columns read 0.
     */
    public TestLog(HydraOpMode op, BotProperties properties, Vision vision, Launcher launcher,
                   LaunchGates launchGates, Intake intake, File folder) {
        mOp = op;
        mProperties = properties;
        mVision = vision;
        mLauncher = launcher;
        mLaunchGates = launchGates;
        mIntake = intake;
        mFolder = folder;
    }

    @Override
    public boolean Init() {
        if (mProperties.testLog) {
            mOp.mTelemetry.addLine("TEST LOG ON: every loop goes to /sdcard/FIRST/logs. Off for matches.");
        }
        if (mProperties.testTelemetry) {
            mOp.mTelemetry.addLine("TEST TELEMETRY ON: buttons, distances and rollers on screen. Off for matches.");
        }
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    @Override
    public void Process() {
        if (!mProperties.testLog && !mProperties.testTelemetry) {
            Close();
            return;
        }
        Collect();
        if (mProperties.testLog) {
            WriteLine();
        } else {
            Close();
        }
        if (mProperties.testTelemetry) {
            Show();
        }
    }

    /** Fill this loop's names and values. */
    private void Collect() {
        mNames.clear();
        mValues.clear();
        if (mStartNanos == 0) {
            mStartNanos = System.nanoTime();
        }
        Add("time_s", (System.nanoTime() - mStartNanos) / 1e9);
        Add("loop_ms", mOp.mLoopTime);

        // Gamepad 1, the driver.
        mDriverFirst = mNames.size();
        Gamepad driver = mOp.mDriverGamepad;
        Add("aim", driver.right_bumper);
        Add("launch", driver.right_trigger > mProperties.trgBtnThresh);
        Add("slow", driver.left_bumper);
        Add("drift", driver.left_trigger > mProperties.trgBtnThresh);
        Add("arc_left", driver.dpad_left);
        Add("arc_right", driver.dpad_right);
        Add("wiggle_button", driver.square);
        Add("reset_forward", driver.circle);
        // Gamepad 2, the operator.
        mOperatorFirst = mNames.size();
        Gamepad operator = mOp.mOperatorGamepad;
        Add("intake_toggle", operator.left_bumper);
        Add("intake_hold", operator.left_trigger > mProperties.trgBtnThresh);
        Add("reverse", operator.square);
        Add("extractor", operator.triangle);
        Add("trim_up", operator.dpad_up);
        Add("trim_down", operator.dpad_down);
        Add("steady_button", operator.cross);
        Add("always_track_button", operator.circle);
        Add("lock_override", operator.right_trigger > mProperties.trgBtnThresh);
        mButtonsEnd = mNames.size();
        // What those buttons switch.
        Add("wiggle_on", mProperties.launchWiggle);
        Add("steady_on", mProperties.steadyRange);
        Add("always_track_on", mProperties.rangeAlwaysTracks);
        Add("gates_open", mLaunchGates != null && mLaunchGates.IsOpen());

        // The camera.
        boolean sees = mVision != null && mVision.SeesTag();
        Add("tag_seen", sees);
        Add("tag_id", sees ? mVision.GetTagId() : 0);
        Add("range_in", sees ? mVision.GetRange() : 0);
        Add("group_range_in", sees ? mVision.GetGroupRange() : 0);
        Add("tag_tilt_deg", sees ? mVision.GetTagTiltDegrees() : Double.NaN);
        Add("hive_bearing_deg", sees ? mVision.GetGroupBearing() : 0);

        // The launchers.
        boolean launcher = mLauncher != null;
        Add("pollen_target_rpm", launcher ? mLauncher.GetPollenTargetRpm() : 0);
        Add("pollen_rpm", launcher ? mLauncher.GetPollenMeasuredRpm() : 0);
        Add("nectar_target_rpm", launcher ? mLauncher.GetNectarTargetRpm() : 0);
        Add("nectar_rpm", launcher ? mLauncher.GetNectarMeasuredRpm() : 0);

        // The rollers.
        Roller intake = mIntake == null ? null : mIntake.GetIntakeRoller();
        Roller transfer = mIntake == null ? null : mIntake.GetTransferRoller();
        Add("intake_target_rpm", intake == null ? 0 : intake.GetTargetRpm());
        Add("intake_rpm", intake == null ? 0 : intake.GetMeasuredRpm());
        Add("intake_power", intake == null ? 0 : intake.GetPower());
        Add("transfer_target_rpm", transfer == null ? 0 : transfer.GetTargetRpm());
        Add("transfer_rpm", transfer == null ? 0 : transfer.GetMeasuredRpm());
        Add("transfer_power", transfer == null ? 0 : transfer.GetPower());
        Add("roller_power_mode", mProperties.rollerUsePower);
    }

    private void Add(String name, boolean value) {
        mNames.add(name);
        mValues.add(value ? "1" : "0");
    }

    private void Add(String name, double value) {
        mNames.add(name);
        mValues.add(Number(value));
    }

    /** A number for the file: whole numbers without ".0", others to three places, NaN empty. */
    static String Number(double value) {
        if (Double.isNaN(value)) {
            return "";
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e9) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.3f", value);
    }

    /** The names, or the values, as one CSV line. */
    static String CsvLine(List<String> cells) {
        StringBuilder line = new StringBuilder();
        for (String cell : cells) {
            if (line.length() > 0) {
                line.append(',');
            }
            line.append(cell);
        }
        return line.toString();
    }

    /** One line to the file. The first line opens it and writes the column names. */
    private void WriteLine() {
        try {
            if (mWriter == null) {
                Open();
            }
            mWriter.write(CsvLine(mValues));
            mWriter.newLine();
            mLinesSinceFlush = mLinesSinceFlush + 1;
            if (mLinesSinceFlush >= linesPerFlush) {
                // Out to the hub's storage, so STOP or a dead battery loses at most a few loops.
                mWriter.flush();
                mLinesSinceFlush = 0;
            }
            mFileStatus = mFile.getPath();
        } catch (IOException e) {
            mFileStatus = "COULD NOT WRITE: " + e.getMessage();
            mWriter = null;
        }
    }

    private void Open() throws IOException {
        File folder = mFolder;
        if (folder == null) {
            folder = new File(AppUtil.FIRST_FOLDER, "logs");
        }
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("could not make " + folder);
        }
        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        mFile = new File(folder, "TestLog_" + time + ".csv");
        mWriter = new BufferedWriter(new FileWriter(mFile));
        mWriter.write(CsvLine(mNames));
        mWriter.newLine();
        mLinesSinceFlush = 0;
    }

    /** testLog switched off (or never on): write out what is left and close the file. */
    private void Close() {
        if (mWriter == null) {
            return;
        }
        try {
            mWriter.close();
        } catch (IOException e) {
            mFileStatus = "COULD NOT CLOSE: " + e.getMessage();
        }
        mWriter = null;
    }

    /** The same numbers on the Driver Station, a few to a line. */
    private void Show() {
        mOp.mTelemetry.addData("Test: driver held", Held(mDriverFirst, mOperatorFirst));
        mOp.mTelemetry.addData("Test: operator held", Held(mOperatorFirst, mButtonsEnd));
        mOp.mTelemetry.addData("Test: switches", "wiggle %s, steady %s, always-track %s, gates %s",
                Value("wiggle_on"), Value("steady_on"), Value("always_track_on"), Value("gates_open"));
        mOp.mTelemetry.addData("Test: distance", "tag %s, %s in (group %s), tilt %s",
                Value("tag_id"), Value("range_in"), Value("group_range_in"), Value("tag_tilt_deg"));
        mOp.mTelemetry.addData("Test: pollen", "target %s, actual %s RPM",
                Value("pollen_target_rpm"), Value("pollen_rpm"));
        mOp.mTelemetry.addData("Test: transfer", "target %s, actual %s RPM, power %s",
                Value("transfer_target_rpm"), Value("transfer_rpm"), Value("transfer_power"));
        mOp.mTelemetry.addData("Test: intake", "target %s, actual %s RPM, power %s",
                Value("intake_target_rpm"), Value("intake_rpm"), Value("intake_power"));
        mOp.mTelemetry.addData("Test log file", mFileStatus);
    }

    /** The names of the buttons held, from column "from" up to (not including) "to". */
    String Held(int from, int to) {
        StringBuilder held = new StringBuilder();
        for (int i = from; i < to && i < mNames.size(); i++) {
            if (mValues.get(i).equals("1")) {
                if (held.length() > 0) {
                    held.append(", ");
                }
                held.append(mNames.get(i));
            }
        }
        if (held.length() == 0) {
            return "none";
        }
        return held.toString();
    }

    /** This loop's value for a column, as written in the file. */
    String Value(String name) {
        int i = mNames.indexOf(name);
        if (i < 0) {
            return "?";
        }
        return mValues.get(i);
    }

    /** The file being written, or null when there is none. For tests. */
    File GetFile() {
        return mFile;
    }
}
