package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * Driver practice: HyDrive, field-centric, with tight turns and drift assist (hold the
 * left trigger while turning). From Decode's DrivePracticeFieldCentric. Runs on last
 * season's robot too.
 */
@TeleOp(name = "Practice: Field Centric", group = "Practice")
public class PracticeFieldCentric extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.fieldCentricDrive = true;
        properties.driveTightTurns = true;
        properties.driveDriftAssist = true;
    }
}
