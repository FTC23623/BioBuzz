package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Plan an auto by driving it. HyDrive, plus a recorder on gamepad 1:
 * <ul>
 *   <li><b>Cross / A</b>: save this spot.
 *   <li><b>Triangle / Y</b>: save this spot and LAUNCH here.
 *   <li><b>Circle / B</b>: save this spot and work the FLOWER here (docs/decisions/0042).
 *   <li><b>D-pad up</b>: save this spot and turn the INTAKE_ON here (docs/decisions/0047).
 *   <li><b>D-pad down</b>: save this spot and turn the INTAKE_OFF here.
 *   <li><b>Square / X</b>: forget the last spot.
 *   <li><b>Back / Share</b>: switch the sticks between robot-centric (at START) and
 *       field-centric, where forward is the way the robot faced at INIT (0055).
 * </ul>
 *
 * <p>Saving a spot with a step blips that step's mechanism, so you can see the press
 * counted: FLOWER swings the extractor out and back, INTAKE_ON runs the intake forward,
 * INTAKE_OFF runs it backward, LAUNCH blinks the light yellow (the gates stay shut).
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
 * <p>Circle is the FLOWER step here, never "reset forward". See docs/decisions/0024.
 */
@TeleOp(name = "Tool: Pose Recorder", group = "Tool")
public class PoseRecorderTool extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.poseRecorderDisabled = false;
        // Starts robot-centric; back / share switches (0055). Circle is FLOWER here, never
        // "reset forward".
        properties.fieldCentricDrive = false;
        properties.circleResetsForward = false;
    }
}
