package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.teamcode.types.Alliance;

/**
 * BACKUP: {@link AutoStrategyB}, always on the blue alliance. The alliance magnet is not
 * read. Pick this only if the magnet sensor is not working (docs/decisions/0021).
 */
@Autonomous(name = "Auto: StrategyB Blue (backup)", group = "Backup")
public class AutoStrategyBBlue extends AutoStrategyB {
    public AutoStrategyBBlue() {
        botProperties.allianceOverride = Alliance.BLUE;
    }
}
