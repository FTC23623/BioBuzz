package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Driver practice: HyDrive, robot-centric, with the sticks swapped. The left stick drives
 * and turns, the right stick strafes. Drift assist is on (hold the left trigger while
 * turning). From Decode's DrivePracticeRobotCentricSwapped. Runs on last season's robot too.
 */
@TeleOp(name = "Practice: Robot Centric Swapped", group = "Practice")
public class PracticeRobotCentricSwapped extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.fieldCentricDrive = false;
        properties.driveSwapStrafeRotate = true;
        properties.driveDriftAssist = true;
    }
}
