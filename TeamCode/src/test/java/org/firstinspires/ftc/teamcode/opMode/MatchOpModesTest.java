package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The match autos: their names on the Driver Station, their order, the TeleOp each one picks
 * when it ends, and which way each starts facing (docs/decisions/0046).
 */
@DisplayName("Match OpModes: names, order and the TeleOp after the auto")
class MatchOpModesTest {

    private static Autonomous auto(Class<?> opMode) {
        return opMode.getAnnotation(Autonomous.class);
    }

    private static String teleOpName(Class<?> opMode) {
        return opMode.getAnnotation(TeleOp.class).name();
    }

    @Test
    @DisplayName("the autos are named by field side")
    void names() {
        assertEquals("Garden Side Auto A", auto(AutoStrategyA.class).name());
        assertEquals("Flower Side Auto B", auto(AutoStrategyB.class).name());
        assertEquals("Flower Side Auto C", auto(AutoStrategyC.class).name());
        assertEquals("Garden Side Auto A Red (backup)", auto(AutoStrategyARed.class).name());
        assertEquals("Flower Side Auto C Blue (backup)", auto(AutoStrategyCBlue.class).name());
    }

    @Test
    @DisplayName("the color-sensing autos have no group, so they list first; the color ones are in Backup")
    void order() {
        Class<?>[] sensing = {AutoStrategyA.class, AutoStrategyB.class, AutoStrategyC.class};
        for (Class<?> opMode : sensing) {
            assertEquals("", auto(opMode).group(), opMode.getSimpleName());
        }
        Class<?>[] backups = {AutoStrategyARed.class, AutoStrategyABlue.class, AutoStrategyBRed.class,
                AutoStrategyBBlue.class, AutoStrategyCRed.class, AutoStrategyCBlue.class};
        for (Class<?> opMode : backups) {
            assertEquals("Backup", auto(opMode).group(), opMode.getSimpleName());
        }
    }

    @Test
    @DisplayName("a color-sensing auto goes on to HyDrive, which also reads the magnet")
    void sensingGoesToHyDrive() {
        String hyDrive = teleOpName(HyDrive.class);
        assertEquals(hyDrive, auto(AutoStrategyA.class).preselectTeleOp());
        assertEquals(hyDrive, auto(AutoStrategyB.class).preselectTeleOp());
        assertEquals(hyDrive, auto(AutoStrategyC.class).preselectTeleOp());
    }

    @Test
    @DisplayName("a color backup auto goes on to the HyDrive backup of the same color")
    void backupsGoToTheirColor() {
        String red = teleOpName(HyDriveRed.class);
        String blue = teleOpName(HyDriveBlue.class);
        assertEquals(red, auto(AutoStrategyARed.class).preselectTeleOp());
        assertEquals(red, auto(AutoStrategyBRed.class).preselectTeleOp());
        assertEquals(red, auto(AutoStrategyCRed.class).preselectTeleOp());
        assertEquals(blue, auto(AutoStrategyABlue.class).preselectTeleOp());
        assertEquals(blue, auto(AutoStrategyBBlue.class).preselectTeleOp());
        assertEquals(blue, auto(AutoStrategyCBlue.class).preselectTeleOp());
    }

    @Test
    @DisplayName("start facing, seen from the driver: A to the right, B and C to the left")
    void startFacing() {
        assertEquals(-90, new AutoStrategyA().StartFacingDeg(), 1e-9);
        assertEquals(90, new AutoStrategyB().StartFacingDeg(), 1e-9);
        assertEquals(90, new AutoStrategyC().StartFacingDeg(), 1e-9);
        // The backups start the same way as the auto they copy.
        assertEquals(-90, new AutoStrategyARed().StartFacingDeg(), 1e-9);
        assertEquals(90, new AutoStrategyCBlue().StartFacingDeg(), 1e-9);
    }

    @Test
    @DisplayName("an auto that does not say how it starts hands TeleOp nothing")
    void unknownStart() {
        assertTrue(Double.isNaN(new AutoStrategyD().StartFacingDeg()));
        assertTrue(Double.isNaN(new AutoShapeZ().StartFacingDeg()));
    }
}
