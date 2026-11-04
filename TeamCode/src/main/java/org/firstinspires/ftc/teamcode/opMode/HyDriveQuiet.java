package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;

/**
 * HyDrive with almost no telemetry after START: only the loop time, its average and the
 * battery. Everything drives exactly the same. Run it and HyDrive one after the other and
 * compare "LoopTime average (ms)" to see what the Driver Station lines cost
 * (docs/decisions/0041). INIT shows every check as usual.
 */
@TeleOp(name = "Test: HyDrive Quiet", group = "Test")
public class HyDriveQuiet extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.quietTelemetry = true;
    }
}
