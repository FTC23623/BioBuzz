package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.Alliance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The backup OpModes set the alliance themselves instead of reading the magnet
 * (docs/decisions/0021). These checks run without a robot: they only look at the
 * settings each OpMode hands the Bot.
 */
@DisplayName("Backup OpModes set the alliance")
class BackupOpModesTest {

    private static Alliance teleOpAlliance(HyDrive opMode) {
        BotProperties properties = new BotProperties();
        opMode.ChooseDriveOptions(properties);
        return properties.allianceOverride;
    }

    @Test
    @DisplayName("HyDrive reads the magnet: no override")
    void hyDriveReadsTheMagnet() {
        assertEquals(Alliance.UNKNOWN, teleOpAlliance(new HyDrive()));
    }

    @Test
    @DisplayName("HyDrive Red (backup) is red, magnet or no magnet")
    void hyDriveRed() {
        assertEquals(Alliance.RED, teleOpAlliance(new HyDriveRed()));
    }

    @Test
    @DisplayName("HyDrive Blue (backup) is blue, magnet or no magnet")
    void hyDriveBlue() {
        assertEquals(Alliance.BLUE, teleOpAlliance(new HyDriveBlue()));
    }

    @Test
    @DisplayName("the backup autos set their color the same way")
    void backupAutos() {
        assertEquals(Alliance.RED, new AutoStrategyARed().botProperties.allianceOverride);
        assertEquals(Alliance.BLUE, new AutoStrategyABlue().botProperties.allianceOverride);
        assertEquals(Alliance.RED, new AutoStrategyBRed().botProperties.allianceOverride);
        assertEquals(Alliance.BLUE, new AutoStrategyBBlue().botProperties.allianceOverride);
    }

    @Test
    @DisplayName("the normal autos read the magnet: no override")
    void normalAutosReadTheMagnet() {
        assertEquals(Alliance.UNKNOWN, new AutoStrategyA().botProperties.allianceOverride);
        assertEquals(Alliance.UNKNOWN, new AutoStrategyB().botProperties.allianceOverride);
    }
}
