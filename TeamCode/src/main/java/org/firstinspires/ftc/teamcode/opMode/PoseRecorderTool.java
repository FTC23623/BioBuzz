package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Plan an auto by driving it. HyDrive, plus a recorder on gamepad 1:
 * <ul>
 *   <li><b>Cross / A</b>: save this spot.
 *   <li><b>Triangle / Y</b>: save this spot and LAUNCH here.
 *   <li><b>Square / X</b>: forget the last spot.
 * </ul>
 *
 * <p>Steps:
 * <ol>
 *   <li>Put the robot exactly on the auto's start spot, facing the way the auto starts.
 *   <li>INIT. That spot is now (0, 0).
 *   <li>START, drive the route slowly, and press cross (or triangle) at each spot.
 *   <li>STOP. The Java is in {@code /sdcard/FIRST/recordings/} on the hub. Copy it into a
 *       copy of AutoShapeZ, run it at half speed first.
 * </ol>
 *
 * <p>Robot-centric, so circle does nothing. See docs/decisions/0024.
 */
@TeleOp(name = "Tool: Pose Recorder", group = "Tool")
public class PoseRecorderTool extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.poseRecorderDisabled = false;
        properties.fieldCentricDrive = false;
    }
}
