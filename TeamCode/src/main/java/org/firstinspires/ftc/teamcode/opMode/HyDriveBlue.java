package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link HyDrive}, always on the blue alliance. The alliance magnet is not read.
 *
 * <p>Pick this only if the magnet sensor is not working. A missing sensor already reads
 * BLUE, but a sensor that is wired wrong or stuck on "magnet" reads RED, and then the
 * camera drops our own hive's tags. This OpMode sets the alliance itself, the same way
 * the backup autos do (docs/decisions/0021).
 */
@TeleOp(name = "HyDrive Blue (backup)", group = "Backup")
public class HyDriveBlue extends HyDrive {
    @Override
    protected void ChooseDriveOptions(BotProperties properties) {
        properties.allianceOverride = Alliance.BLUE;
    }
}
