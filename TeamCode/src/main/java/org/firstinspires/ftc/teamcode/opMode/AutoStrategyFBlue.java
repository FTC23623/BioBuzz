package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link AutoStrategyF}, always on the blue alliance. The alliance magnet is
 * not read. Pick this only if the magnet sensor is not working (docs/decisions/0021). At the
 * end it picks HyDrive Blue (backup), the TeleOp that is also always blue.
 */
@Autonomous(name = "Blue: " + AutoStrategyF.NAME, group = "Blue",
        preselectTeleOp = HyDrive.BLUE_NAME)
public class AutoStrategyFBlue extends AutoStrategyF {
    public AutoStrategyFBlue() {
        botProperties.allianceOverride = Alliance.BLUE;
    }
}
