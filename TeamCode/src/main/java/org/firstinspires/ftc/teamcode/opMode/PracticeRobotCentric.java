package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Driver practice: HyDrive, robot-centric, with tight turns and drift assist (hold the
 * left trigger while turning). From Decode's DrivePracticeRobotCentric. Runs on last
 * season's robot too.
 */
@TeleOp(name = "Practice: Robot Centric", group = "Practice")
public class PracticeRobotCentric extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.fieldCentricDrive = false;
        properties.driveTightTurns = true;
        properties.driveDriftAssist = true;
    }
}
