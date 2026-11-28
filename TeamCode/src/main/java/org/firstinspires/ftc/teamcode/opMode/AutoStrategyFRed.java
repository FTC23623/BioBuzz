package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link AutoStrategyF}, always on the red alliance. The alliance magnet is
 * not read. Pick this only if the magnet sensor is not working (docs/decisions/0021). At the
 * end it picks HyDrive Red (backup), the TeleOp that is also always red.
 */
@Autonomous(name = "Red: " + AutoStrategyF.NAME, group = "Red",
        preselectTeleOp = HyDrive.RED_NAME)
public class AutoStrategyFRed extends AutoStrategyF {
    public AutoStrategyFRed() {
        botProperties.allianceOverride = Alliance.RED;
    }
}
