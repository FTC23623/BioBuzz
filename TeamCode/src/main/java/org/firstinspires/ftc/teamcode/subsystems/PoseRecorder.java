package org.firstinspires.ftc.teamcode.subsystems;

import com.pedropathing.localization.Localizer;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
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
 *   <li><b>Back / Share</b>: switch the sticks between robot-centric and field-centric
 *       (docs/decisions/0055). One rumble field-centric, two robot-centric.
 * </ul>
 *
 * <p>A spot saved with a step <b>blips</b> that step for {@code recorderBlipSeconds}, so you
 * can see it counted: FLOWER swings the extractor out, INTAKE_ON runs the intake forward,
 * INTAKE_OFF runs it backward, LAUNCH blinks the light yellow. The launch gates never open.
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
    private final Debouncer mCentricButton;
    private final BotProperties mProperties;
    // The parts a step blips. Any may be null: that step just doesn't blip.
    private final Intake mIntake;
    private final FlowerExtractor mExtractor;
    private final Indicator mIndicator;
    /** The step blipping now: "LAUNCH", "FLOWER", "INTAKE_ON", "INTAKE_OFF", or null. */
    private String mBlip = null;
    private final ElapsedTime mBlipTimer = new ElapsedTime();

    private boolean mStarted = false;
    private String mFileStatus = "nothing saved yet";

    /**
     * @param folder where the recording files go. The Bot passes /sdcard/FIRST/recordings.
     */
    public PoseRecorder(HydraOpMode op, BotProperties properties, Gamepad gamepad,
                        Localizer localizer, File folder) {
        this(op, properties, gamepad, localizer, folder, null, null, null);
    }

    /**
     * @param intake    blipped for INTAKE_ON / INTAKE_OFF; null for none
     * @param extractor blipped for FLOWER; null for none
     * @param indicator blinked for LAUNCH; null for none
     */
    public PoseRecorder(HydraOpMode op, BotProperties properties, Gamepad gamepad,
                        Localizer localizer, File folder, Intake intake,
                        FlowerExtractor extractor, Indicator indicator) {
        mProperties = properties;
        mIntake = intake;
        mExtractor = extractor;
        mIndicator = indicator;
        mCentricButton = new Debouncer(properties.debounce);
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
        mOp.mTelemetry.addLine("Back / share: robot-centric or field-centric sticks.");
        return true;
    }

    @Override
    public void HandleUserInput() {
        if (Pressed(mSaveButton, mGamepad.cross)) {
            SaveSpot(false);
        }
        if (Pressed(mLaunchButton, mGamepad.triangle)) {
            SaveSpot(true);
            StartBlip("LAUNCH");
        }
        if (Pressed(mFlowerButton, mGamepad.circle)) {
            SaveSpot(false, true);
            StartBlip("FLOWER");
        }
        if (Pressed(mIntakeOnButton, mGamepad.dpad_up)) {
            SaveSpot(false, false, true, false);
            StartBlip("INTAKE_ON");
        }
        if (Pressed(mIntakeOffButton, mGamepad.dpad_down)) {
            SaveSpot(false, false, false, true);
            StartBlip("INTAKE_OFF");
        }
        // Back / share: robot-centric or field-centric sticks. One rumble field, two robot.
        if (Pressed(mCentricButton, mGamepad.back || mGamepad.share)) {
            mProperties.fieldCentricDrive = !mProperties.fieldCentricDrive;
            if (mProperties.fieldCentricDrive) {
                mGamepad.rumbleBlips(1);
            } else {
                mGamepad.rumbleBlips(2);
            }
        }
        // After the parts read their own buttons, so the blip wins for its length.
        ApplyBlip();
        if (Pressed(mUndoButton, mGamepad.square) && !mSpots.isEmpty()) {
            mSpots.remove(mSpots.size() - 1);
            mGamepad.rumbleBlips(2);
            WriteFile();
        }
    }

    /** Show a step that was just saved: blip its mechanism for recorderBlipSeconds. */
    private void StartBlip(String step) {
        EndBlip();
        mBlip = step;
        mBlipTimer.reset();
    }

    /**
     * Every loop while a blip runs: hold its mechanism on. Runs after the parts read their
     * buttons this loop and before they act, so it wins; when it ends, they go back to
     * their buttons by themselves.
     */
    private void ApplyBlip() {
        if (mBlip == null) {
            return;
        }
        if (mBlipTimer.seconds() >= mProperties.recorderBlipSeconds) {
            EndBlip();
            return;
        }
        if (mBlip.equals("FLOWER") && mExtractor != null) {
            mExtractor.SetDeployed(true);
        } else if (mBlip.equals("INTAKE_ON") && mIntake != null) {
            mIntake.BlipIntakeRpm(mProperties.intakeRpm);
        } else if (mBlip.equals("INTAKE_OFF") && mIntake != null) {
            mIntake.BlipIntakeRpm(-mProperties.intakeReverseRpm);
        } else if (mBlip.equals("LAUNCH") && mIndicator != null) {
            mIndicator.SetLaunching(true);
        }
    }

    private void EndBlip() {
        if ("LAUNCH".equals(mBlip) && mIndicator != null) {
            mIndicator.SetLaunching(false);
        }
        mBlip = null;
    }

    /** The step blipping now, or null. For tests. */
    String Blipping() {
        return mBlip;
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
        mOp.mTelemetry.addData("Sticks", mProperties.fieldCentricDrive
                ? "FIELD-centric (forward = the way it faced at INIT). Back / share switches."
                : "ROBOT-centric. Back / share switches.");
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
