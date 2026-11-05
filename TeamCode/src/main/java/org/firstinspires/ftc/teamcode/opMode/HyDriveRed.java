package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link HyDrive}, always on the red alliance. The alliance magnet is not read.
 *
 * <p>Pick this only if the magnet sensor is not working. With no sensor the robot reads
 * BLUE, and on red that means the camera drops our own hive's tags: aim assist, the launch
 * trigger's aim, the indicator light and the launcher's speed-from-distance all stop
 * working. This OpMode sets the alliance itself, the same way the backup autos do
 * (docs/decisions/0021).
 */
@TeleOp(name = HyDrive.RED_NAME, group = "Backup")
public class HyDriveRed extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.allianceOverride = Alliance.RED;
    }
}
