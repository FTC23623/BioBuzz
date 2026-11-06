package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * HyDrive, set up for a launch tuning session with Claude on a laptop
 * (tools/tuning/README.md, docs/decisions/0052). Practice only: it needs FTC Dashboard.
 *
 * <p>Everything drives and launches as in HyDrive; the people at the robot do all the
 * driving and launching. On top:
 * <ul>
 *   <li>Every loop goes to the test log (a CSV on the hub) and to the dashboard as plain
 *       numbers ("T ..." lines), which the laptop records: buttons, distances, aim, speeds.
 *   <li>Each ball is found by its dip in the pollen speed; the last shot's dip, recovery
 *       time and distance stay on screen.
 *   <li><b>Driver cross</b>: the last shot went in. <b>Driver triangle</b>: it missed.
 *   <li>The wheels coast when the sticks are let go, so the robot can be slid around by
 *       hand. Where it is comes from the AprilTags (0054): from the hive (left / out / yaw)
 *       and, with the field map on the Limelight, on the field.
 *   <li>The rest of the screen is quiet, so the dashboard isn't flooded.
 * </ul>
 * Settings changed from the laptop are gone at STOP, like any dashboard change: write the
 * winners into BotProperties.
 */
@TeleOp(name = "Tool: Launch Tuning", group = "Tool")
public class LaunchTuningTool extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.testLog = true;
        properties.testTelemetry = true;
        properties.tuningMarks = true;
        properties.quietTelemetry = true;
        properties.launcherOnlyTelemetry = false;
        // Slide the robot by hand between shots: the wheels coast, the camera says where it is.
        properties.driveCoastWhenIdle = true;
    }
}
