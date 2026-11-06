package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link AutoStrategyD}, always on the red alliance. The alliance magnet is not
 * read. Pick this only if the magnet sensor is not working (docs/decisions/0021).
 */
@Autonomous(name = "Auto: StrategyD Red (backup)", group = "Backup")
public class AutoStrategyDRed extends AutoStrategyD {
    public AutoStrategyDRed() {
        botProperties.allianceOverride = Alliance.RED;
    }
}
