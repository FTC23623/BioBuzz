package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link AutoStrategyGardenD}, always on the blue alliance. The alliance magnet is
 * not read. Pick this only if the magnet sensor is not working (docs/decisions/0021). At the
 * end it picks HyDrive Blue (backup), the TeleOp that is also always blue.
 */
@Autonomous(name = "Blue: " + AutoStrategyGardenD.NAME, group = "Blue",
        preselectTeleOp = HyDrive.BLUE_NAME)
public class AutoStrategyGardenDBlue extends AutoStrategyGardenD {
    public AutoStrategyGardenDBlue() {
        botProperties.allianceOverride = Alliance.BLUE;
    }
}
