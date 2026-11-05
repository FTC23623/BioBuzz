package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.localization.Localizer;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.Gamepad;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.Debouncer;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * Drive the robot by hand and save spots along the way. The recorder writes them out as
 * Java an auto can use: {@code new Pose(...)} lines and the paths between them.
 *
 * <p>Like teaching a factory robot arm: move it there by hand, press "remember", move on.
 * The robot records where it IS (from the odometry pods), never what the motors did, so
 * the auto that drives it later corrects for battery and carpet (PORT-PLAN P19).
 *
 * <p>Controls (gamepad 1, on top of the normal HyDrive driving):
 * <ul>
 *   <li><b>Cross / A</b>: save this spot.
 *   <li><b>Triangle / Y</b>: save this spot, and LAUNCH here.
 *   <li><b>Circle / B</b>: save this spot, and work the FLOWER here: back up, extractor out,
 *       stroke forward and back, extractor in (docs/decisions/0042). Drive up to the flower,
 *       facing it, the way you want the robot to meet it, then press.
 *   <li><b>D-pad up</b>: save this spot, and turn the INTAKE_ON here: roller and transfer
 *       run from here on (docs/decisions/0047).
 *   <li><b>D-pad down</b>: save this spot, and turn the INTAKE_OFF here.
 *   <li><b>Square / X</b>: forget the last spot.
 * </ul>
 *
 * <p>Every change is written straight away to a file on the hub, in
 * {@code /sdcard/FIRST/recordings/}, so STOP or a dead battery loses nothing. Copy the
 * file into a new auto's {@code BuildPaths()}. See docs/decisions/0024.
 *
 * <p>Measured like Pedro: inches and degrees, from where the robot was at INIT. x grows
 * driving forward, y grows driving left, heading grows turning left. Start the recording
 * from the exact spot (and facing) the auto will start from.
 */
public class PoseRecorder implements Subsystem {
    /** One saved spot. */
    static class Spot {
        final double x;
        final double y;
        final double headingDeg;
        /** true: the auto stops here and launches. */
        final boolean launch;
        /** true: the auto works the flower here (the FLOWER step). */
        final boolean flower;
        /** true: the auto turns the intake on here (the INTAKE_ON step). */
        final boolean intakeOn;
        /** true: the auto turns the intake off here (the INTAKE_OFF step). */
        final boolean intakeOff;

        Spot(double x, double y, double headingDeg, boolean launch) {
            this(x, y, headingDeg, launch, false);
        }

        Spot(double x, double y, double headingDeg, boolean launch, boolean flower) {
            this(x, y, headingDeg, launch, flower, false, false);
        }

        Spot(double x, double y, double headingDeg, boolean launch, boolean flower,
             boolean intakeOn, boolean intakeOff) {
            this.x = x;
            this.y = y;
            this.headingDeg = headingDeg;
            this.launch = launch;
            this.flower = flower;
            this.intakeOn = intakeOn;
            this.intakeOff = intakeOff;
        }
    }

    /** A heading change smaller than this, in degrees, counts as "no turn". */
    static final double noTurnDeg = 1;

    private final HydraOpMode mOp;
    private final Gamepad mGamepad;
    private final Localizer mLocalizer;
    private final File mFile;
    private final List<Spot> mSpots = new ArrayList<>();
    private final Debouncer mSaveButton;
    private final Debouncer mLaunchButton;
    private final Debouncer mFlowerButton;
    private final Debouncer mIntakeOnButton;
    private final Debouncer mIntakeOffButton;
    private final Debouncer mUndoButton;

    private boolean mStarted = false;
    private String mFileStatus = "nothing saved yet";

    /**
     * @param folder where the recording files go. The Bot passes /sdcard/FIRST/recordings.
     */
    public PoseRecorder(HydraOpMode op, BotProperties properties, Gamepad gamepad,
                        Localizer localizer, File folder) {
        mOp = op;
        mGamepad = gamepad;
        mLocalizer = localizer;
        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        mFile = new File(folder, "PoseRecording_" + time + ".java.txt");
        mSaveButton = new Debouncer(properties.debounce);
        mLaunchButton = new Debouncer(properties.debounce);
        mFlowerButton = new Debouncer(properties.debounce);
        mIntakeOnButton = new Debouncer(properties.debounce);
        mIntakeOffButton = new Debouncer(properties.debounce);
        mUndoButton = new Debouncer(properties.debounce);
    }

    /** Where the robot is at INIT is (0, 0), facing 0. Everything is measured from here. */
    @Override
    public boolean Init() {
        if (!mStarted) {
            mStarted = true;
            mLocalizer.setPose(new Pose(0, 0, 0));
        }
        mOp.mTelemetry.addLine("POSE RECORDER: the robot's spot right now is the start (0, 0).");
        mOp.mTelemetry.addLine("Cross: save spot. Triangle: save + LAUNCH. Circle: save + FLOWER. Square: undo.");
        mOp.mTelemetry.addLine("D-pad up: save + INTAKE_ON. D-pad down: save + INTAKE_OFF.");
        return true;
    }

    @Override
    public void HandleUserInput() {
        if (Pressed(mSaveButton, mGamepad.cross)) {
            SaveSpot(false);
        }
        if (Pressed(mLaunchButton, mGamepad.triangle)) {
            SaveSpot(true);
        }
        if (Pressed(mFlowerButton, mGamepad.circle)) {
            SaveSpot(false, true);
        }
        if (Pressed(mIntakeOnButton, mGamepad.dpad_up)) {
            SaveSpot(false, false, true, false);
        }
        if (Pressed(mIntakeOffButton, mGamepad.dpad_down)) {
            SaveSpot(false, false, false, true);
        }
        if (Pressed(mUndoButton, mGamepad.square) && !mSpots.isEmpty()) {
            mSpots.remove(mSpots.size() - 1);
            mGamepad.rumbleBlips(2);
            WriteFile();
        }
    }

    /** true once per press: the button was held long enough, and this press is now used up. */
    private static boolean Pressed(Debouncer button, boolean down) {
        button.In(down);
        if (button.Out()) {
            button.Used();
            return true;
        }
        return false;
    }

    @Override
    public void Process() {
        mLocalizer.update();
        Pose here = mLocalizer.pose();
        mOp.mTelemetry.addData("Robot is at", "x %.1f, y %.1f in, heading %.0f deg",
                here.x(), here.y(), Math.toDegrees(here.heading()));
        mOp.mTelemetry.addData("Spots saved", mSpots.size());
        // The last three, so the driver can see the press counted.
        int first = Math.max(0, mSpots.size() - 3);
        for (int i = first; i < mSpots.size(); i++) {
            mOp.mTelemetry.addLine("  " + SpotLine(i + 1, mSpots.get(i)));
        }
        mOp.mTelemetry.addData("File", mFileStatus);
    }

    /** Remember where the robot is now, rumble once, and write the file. */
    private void SaveSpot(boolean launch) {
        SaveSpot(launch, false);
    }

    /** Remember where the robot is now, rumble once, and write the file. */
    private void SaveSpot(boolean launch, boolean flower) {
        SaveSpot(launch, flower, false, false);
    }

    /** Remember where the robot is now, rumble once, and write the file. */
    private void SaveSpot(boolean launch, boolean flower, boolean intakeOn, boolean intakeOff) {
        Pose here = mLocalizer.pose();
        double previousDeg = 0;
        if (!mSpots.isEmpty()) {
            previousDeg = mSpots.get(mSpots.size() - 1).headingDeg;
        }
        double headingDeg = Unwrap(previousDeg, Math.toDegrees(here.heading()));
        mSpots.add(new Spot(here.x(), here.y(), headingDeg, launch, flower, intakeOn, intakeOff));
        mGamepad.rumbleBlips(1);
        WriteFile();
    }

    /**
     * The heading, written as close as possible to the last one. The sensor may say 350
     * for what is really 10 degrees right of 0. Written as -10, the auto turns 10 degrees
     * right instead of 350 left.
     */
    static double Unwrap(double previousDeg, double headingDeg) {
        double change = headingDeg - previousDeg;
        while (change > 180) {
            change = change - 360;
        }
        while (change <= -180) {
            change = change + 360;
        }
        return previousDeg + change;
    }

    public int CountSpots() {
        return mSpots.size();
    }

    /** Write every spot, as Java, to the file. Called after every change. */
    private void WriteFile() {
        try {
            File folder = mFile.getParentFile();
            if (folder != null && !folder.exists() && !folder.mkdirs()) {
                mFileStatus = "COULD NOT MAKE FOLDER " + folder;
                return;
            }
            FileWriter writer = new FileWriter(mFile);
            writer.write(ToJava(mSpots));
            writer.close();
            mFileStatus = mFile.getPath();
        } catch (IOException e) {
            mFileStatus = "COULD NOT WRITE: " + e.getMessage();
        }
    }

    /** "spot 2: x 24.0, y -3.5, heading -90, LAUNCH" for the Driver Station. */
    static String SpotLine(int number, Spot spot) {
        String line = String.format(Locale.US, "spot %d: x %.1f, y %.1f, heading %.0f",
                number, spot.x, spot.y, spot.headingDeg);
        if (spot.launch) {
            line = line + ", LAUNCH";
        }
        if (spot.flower) {
            line = line + ", FLOWER";
        }
        if (spot.intakeOn) {
            line = line + ", INTAKE_ON";
        }
        if (spot.intakeOff) {
            line = line + ", INTAKE_OFF";
        }
        return line;
    }

    /**
     * The spots as Java for a PedroAuto's {@code BuildPaths()}. One Pose per spot, a
     * straight line from each spot to the next, a LAUNCH after any spot saved with
     * triangle, a FLOWER after any spot saved with circle, and an INTAKE_ON or INTAKE_OFF
     * after any spot saved with D-pad up or down. A path keeps its heading when the robot
     * did not turn, else it turns smoothly along the way.
     */
    static String ToJava(List<Spot> spots) {
        StringBuilder java = new StringBuilder();
        java.append("// Recorded with Tool: Pose Recorder. ").append(spots.size()).append(" spots.\n");
        java.append("// Paste into BuildPaths() of a copy of AutoShapeZ. It also needs:\n");
        java.append("//     import static com.pedropathing.api.Paths.line;\n");
        java.append("//     import com.pedropathing.paths.interpolator.Interpolator;\n");
        java.append("// Start the auto from the spot the recording started from, facing the same way.\n\n");

        for (int i = 0; i < spots.size(); i++) {
            Spot spot = spots.get(i);
            java.append(String.format(Locale.US, "Pose spot%d = new Pose(%.1f, %.1f);   // heading %.0f deg\n",
                    i + 1, spot.x, spot.y, spot.headingDeg));
        }
        java.append("\n");

        String from = "StartPose()";
        double fromHeading = 0;
        for (int i = 0; i < spots.size(); i++) {
            Spot spot = spots.get(i);
            String to = "spot" + (i + 1);
            java.append("Path to").append(Capital(to)).append(" = line(").append(from).append(", ").append(to)
                    .append(")").append(Heading(fromHeading, spot.headingDeg)).append(";\n");
            from = to;
            fromHeading = spot.headingDeg;
        }
        java.append("\nList<Path> paths = new ArrayList<>();\n");
        for (int i = 0; i < spots.size(); i++) {
            java.append("paths.add(toSpot").append(i + 1).append(");\n");
            if (spots.get(i).intakeOn) {
                java.append("paths.add(INTAKE_ON);\n");
            }
            if (spots.get(i).intakeOff) {
                java.append("paths.add(INTAKE_OFF);\n");
            }
            if (spots.get(i).flower) {
                java.append("paths.add(FLOWER);\n");
            }
            if (spots.get(i).launch) {
                java.append("paths.add(LAUNCH);\n");
            }
        }
        java.append("return paths;\n");
        return java.toString();
    }

    /** ".constant(...)" when the heading stays the same, else a smooth turn. */
    static String Heading(double fromDeg, double toDeg) {
        if (Math.abs(toDeg - fromDeg) < noTurnDeg) {
            return String.format(Locale.US, ".constant(Math.toRadians(%.0f))", toDeg);
        }
        return String.format(Locale.US, ".heading(Interpolator.linear(Math.toRadians(%.0f), Math.toRadians(%.0f)))",
                fromDeg, toDeg);
    }

    /** "spot1" becomes "Spot1", for the path names. */
    private static String Capital(String word) {
        return word.substring(0, 1).toUpperCase(Locale.US) + word.substring(1);
    }
}
