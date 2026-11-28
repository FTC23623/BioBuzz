package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import org.firstinspires.ftc.teamcode.bot.Bot;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.TipTuning;
import org.firstinspires.ftc.teamcode.types.HiveUp;
import org.firstinspires.ftc.teamcode.types.TagLook;

/**
 * Tunes tip detection: how the camera tells a hive cell that is UP from one that is DOWN
 * (docs/decisions/0057). Nothing moves. Only the camera runs.
 *
 * <p>How to use it:
 * <ol>
 *   <li>Put the robot where an auto does its CHECK_TIPPED, facing the hive, with a cell tipped
 *       UP. Press and HOLD cross on gamepad 1 for a few seconds: it records what the camera
 *       reads ("UP" readings).
 *   <li>Tip the cell DOWN, same spot. Press and HOLD square ("DOWN" readings).
 *   <li>Do it again from other spots an auto checks from. Circle clears everything.
 *   <li>Read "Suggested" on the Driver Station: it says which angle (roll or pitch) tells UP
 *       from DOWN best and which numbers to put in BotProperties.
 * </ol>
 *
 * <p>Every tag in view is listed with its own angles, so you can see whether the nearest tag
 * (the one CHECK_TIPPED uses) is behaving like the others. "CHECK_TIPPED would say" is exactly
 * what an auto would decide right now with today's numbers.
 */
@TeleOp(name = "Test: Tip Detection", group = "Test")
public class TipDetectionTest extends LinearOpMode {
    /** How many readings of each (UP and DOWN) before a suggestion is made. */
    static final int ENOUGH_READINGS = 10;
    /** The most room to leave around the readings on each side, in degrees. */
    static final double MAX_MARGIN_DEG = 10;

    /** The readings of one angle while the cell is UP or DOWN. */
    private final TipTuning.Stats mUpRoll = new TipTuning.Stats();
    private final TipTuning.Stats mUpPitch = new TipTuning.Stats();
    private final TipTuning.Stats mUpYaw = new TipTuning.Stats();
    private final TipTuning.Stats mDownRoll = new TipTuning.Stats();
    private final TipTuning.Stats mDownPitch = new TipTuning.Stats();
    private final TipTuning.Stats mDownYaw = new TipTuning.Stats();
    /** Which tags the readings came from. */
    private final TreeSet<Integer> mUpTags = new TreeSet<>();
    private final TreeSet<Integer> mDownTags = new TreeSet<>();

    /**
     * What to say about one set of readings. Roll or pitch, from "UP" and "DOWN" readings:
     * the gap between them and, if they stay apart, the numbers to use.
     *
     * @return the line to show. Says so when there are too few readings, or the two overlap.
     */
    static String Suggest(String angleName, boolean fromRoll, TipTuning.Stats up, TipTuning.Stats down) {
        if (!TipTuning.Enough(up, down, ENOUGH_READINGS)) {
            return String.format(Locale.US, "%s: need %d readings of each (UP %d, DOWN %d)",
                    angleName, ENOUGH_READINGS, up.Count(), down.Count());
        }
        double gap = TipTuning.GapDeg(up, down);
        if (gap <= 0) {
            return String.format(Locale.US,
                    "%s: UP %.0f to %.0f and DOWN %.0f to %.0f OVERLAP: this angle can't tell them apart",
                    angleName, up.Min(), up.Max(), down.Min(), down.Max());
        }
        double margin = TipTuning.MarginDeg(gap, MAX_MARGIN_DEG);
        double[] upWindow = TipTuning.Window(up, margin);
        double[] downWindow = TipTuning.Window(down, margin);
        return String.format(Locale.US,
                "%s: gap %.0f deg. autoTipFromRoll = %b, autoTagUpMinDeg = %.0f, autoTagUpMaxDeg = %.0f, "
                        + "autoTagDownMinDeg = %.0f, autoTagDownMaxDeg = %.0f",
                angleName, gap, fromRoll, upWindow[0], upWindow[1], downWindow[0], downWindow[1]);
    }

    /** UP, DOWN, or "between" for one angle, by today's windows in BotProperties. */
    static String Classify(double angleDeg, BotProperties p) {
        if (Double.isNaN(angleDeg)) {
            return "no angle";
        }
        if (angleDeg >= p.autoTagUpMinDeg && angleDeg <= p.autoTagUpMaxDeg) {
            return "UP";
        }
        if (angleDeg >= p.autoTagDownMinDeg && angleDeg <= p.autoTagDownMaxDeg) {
            return "DOWN";
        }
        return "between (no answer)";
    }

    private static void AddReading(TagLook tag, TipTuning.Stats roll, TipTuning.Stats pitch,
                                   TipTuning.Stats yaw, TreeSet<Integer> ids) {
        roll.Add(tag.rollDeg);
        pitch.Add(tag.pitchDeg);
        yaw.Add(tag.yawDeg);
        ids.add(tag.id);
    }

    private void ClearAll() {
        mUpRoll.Clear();
        mUpPitch.Clear();
        mUpYaw.Clear();
        mDownRoll.Clear();
        mDownPitch.Clear();
        mDownYaw.Clear();
        mUpTags.clear();
        mDownTags.clear();
    }

    private static String Readings(TipTuning.Stats roll, TipTuning.Stats pitch, TipTuning.Stats yaw) {
        if (roll.Count() == 0) {
            return "none yet";
        }
        return String.format(Locale.US, "%d readings. roll %.0f to %.0f (avg %.0f), pitch %.0f to %.0f (avg %.0f), yaw %.0f to %.0f",
                roll.Count(), roll.Min(), roll.Max(), roll.Mean(), pitch.Min(), pitch.Max(), pitch.Mean(),
                yaw.Min(), yaw.Max());
    }

    @Override
    public void runOpMode() {
        BotProperties properties = new BotProperties();
        // Only the camera: nothing may move, and the camera's own long status stays hidden.
        properties.driveDisabled = true;
        properties.launcherDisabled = true;
        properties.intakeDisabled = true;
        properties.launchGatesDisabled = true;
        properties.flowerExtractorDisabled = true;
        properties.quietTelemetry = true;
        properties.launcherOnlyTelemetry = false;

        Bot bot = new Bot(this, properties);
        bot.Init();

        // The readings show before and after START, so the check can happen either way.
        while (opModeInInit() || opModeIsActive()) {
            bot.StartLoop();
            bot.Process();

            List<TagLook> tags = bot.TagsInView();
            int nearestId = bot.GetTagId();
            boolean seesTag = bot.SeesTag();
            TagLook nearest = null;
            for (TagLook tag : tags) {
                if (seesTag && tag.id == nearestId) {
                    nearest = tag;
                }
            }

            // Hold cross to record UP readings, square for DOWN. Circle clears.
            if (gamepad1.circle) {
                ClearAll();
            }
            if (nearest != null && gamepad1.cross) {
                AddReading(nearest, mUpRoll, mUpPitch, mUpYaw, mUpTags);
            }
            if (nearest != null && gamepad1.square) {
                AddReading(nearest, mDownRoll, mDownPitch, mDownYaw, mDownTags);
            }

            telemetry.addLine("HOLD cross: record UP.  HOLD square: record DOWN.  Circle: clear.");
            telemetry.addLine("Stand where an auto does CHECK_TIPPED. Nothing moves.");
            telemetry.addLine();
            telemetry.addData("Tags of ours in view", tags.size());
            for (TagLook tag : tags) {
                HiveUp side = HiveUp.FromTag(tag.id, properties.gardenSideTags, properties.flowerSideTags);
                String sideName = side == HiveUp.GARDEN_SIDE ? "garden side"
                        : side == HiveUp.FLOWER_SIDE ? "flower side" : "side not known";
                String line = String.format(Locale.US,
                        "  tag %d (%s): roll %.0f, pitch %.0f, yaw %.0f. %.0f in, %.0f deg %s, fills %.1f%%. %s says %s",
                        tag.id, sideName, tag.rollDeg, tag.pitchDeg, tag.yawDeg, tag.rangeIn,
                        Math.abs(tag.bearingDeg), tag.bearingDeg >= 0 ? "left" : "right", tag.areaPercent,
                        properties.autoTipFromRoll ? "roll" : "pitch",
                        Classify(tag.Angle(properties.autoTipFromRoll), properties));
                if (nearest != null && tag.id == nearest.id) {
                    line = line + "   <- NEAREST: the one CHECK_TIPPED uses";
                }
                telemetry.addLine(line);
            }
            telemetry.addData("CHECK_TIPPED would say", PedroAuto.HiveUpText(bot.SeesHiveUp()));
            telemetry.addData("Today's numbers", String.format(Locale.US,
                    "reads %s. UP %.0f to %.0f, DOWN %.0f to %.0f. down means other side up: %b. needs tilt: %b",
                    properties.autoTipFromRoll ? "roll" : "pitch",
                    properties.autoTagUpMinDeg, properties.autoTagUpMaxDeg,
                    properties.autoTagDownMinDeg, properties.autoTagDownMaxDeg,
                    properties.autoDownMeansOtherSideUp, properties.autoTippedNeedsTilt));
            if (bot.SeesTag() && tags.isEmpty()) {
                telemetry.addLine("Camera sees a tag but gives no angles: no pose from the Limelight.");
            }
            telemetry.addLine();
            telemetry.addData("UP readings", Readings(mUpRoll, mUpPitch, mUpYaw) + " from tags " + mUpTags);
            telemetry.addData("DOWN readings", Readings(mDownRoll, mDownPitch, mDownYaw) + " from tags " + mDownTags);
            telemetry.addLine("Suggested (put in BotProperties):");
            telemetry.addLine("  " + Suggest("roll", true, mUpRoll, mDownRoll));
            telemetry.addLine("  " + Suggest("pitch", false, mUpPitch, mDownPitch));
            bot.EndLoop();
        }
    }
}
