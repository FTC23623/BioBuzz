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
        assertEquals("Auto: Garden Side A", auto(AutoStrategyA.class).name());
        assertEquals("Auto: Flower Side B", auto(AutoStrategyB.class).name());
        assertEquals("Auto: Flower Side C", auto(AutoStrategyC.class).name());
        assertEquals("Red: Garden Side A", auto(AutoStrategyARed.class).name());
        assertEquals("Blue: Flower Side C", auto(AutoStrategyCBlue.class).name());
        assertEquals("Auto: Garden Side D", auto(AutoStrategyGardenD.class).name());
        assertEquals("Blue: Garden Side D", auto(AutoStrategyGardenDBlue.class).name());
        assertEquals("Red: Garden Side D", auto(AutoStrategyGardenDRed.class).name());
        assertEquals("Auto: Garden Side E", auto(AutoStrategyGardenE.class).name());
        assertEquals("Blue: Garden Side E", auto(AutoStrategyGardenEBlue.class).name());
        assertEquals("Red: Garden Side E", auto(AutoStrategyGardenERed.class).name());
    }

    @Test
    @DisplayName("groups Auto (reads the magnet), Blue and Red, in that order on the Driver Station")
    void order() {
        // The Driver Station sorts by group: Auto, then Blue, then Red.
        Class<?>[] sensing = {AutoStrategyA.class, AutoStrategyB.class, AutoStrategyC.class,
                AutoStrategyGardenD.class, AutoStrategyGardenE.class};
        for (Class<?> opMode : sensing) {
            assertEquals("Auto", auto(opMode).group(), opMode.getSimpleName());
        }
        Class<?>[] blue = {AutoStrategyABlue.class, AutoStrategyBBlue.class, AutoStrategyCBlue.class,
                AutoStrategyGardenDBlue.class, AutoStrategyGardenEBlue.class};
        for (Class<?> opMode : blue) {
            assertEquals("Blue", auto(opMode).group(), opMode.getSimpleName());
            assertTrue(auto(opMode).name().startsWith("Blue: "), opMode.getSimpleName());
        }
        Class<?>[] red = {AutoStrategyARed.class, AutoStrategyBRed.class, AutoStrategyCRed.class,
                AutoStrategyGardenDRed.class, AutoStrategyGardenERed.class};
        for (Class<?> opMode : red) {
            assertEquals("Red", auto(opMode).group(), opMode.getSimpleName());
            assertTrue(auto(opMode).name().startsWith("Red: "), opMode.getSimpleName());
        }
    }

    @Test
    @DisplayName("a color-sensing auto goes on to HyDrive, which also reads the magnet")
    void sensingGoesToHyDrive() {
        String hyDrive = teleOpName(HyDrive.class);
        assertEquals(hyDrive, auto(AutoStrategyA.class).preselectTeleOp());
        assertEquals(hyDrive, auto(AutoStrategyB.class).preselectTeleOp());
        assertEquals(hyDrive, auto(AutoStrategyC.class).preselectTeleOp());
        assertEquals(hyDrive, auto(AutoStrategyGardenD.class).preselectTeleOp());
        assertEquals(hyDrive, auto(AutoStrategyGardenE.class).preselectTeleOp());
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
        assertEquals(red, auto(AutoStrategyGardenDRed.class).preselectTeleOp());
        assertEquals(blue, auto(AutoStrategyGardenDBlue.class).preselectTeleOp());
        assertEquals(red, auto(AutoStrategyGardenERed.class).preselectTeleOp());
        assertEquals(blue, auto(AutoStrategyGardenEBlue.class).preselectTeleOp());
    }

    @Test
    @DisplayName("start facing, seen from the driver: A to the left, B and C to the right")
    void startFacing() {
        assertEquals(90, new AutoStrategyA().StartFacingDeg(), 1e-9);
        assertEquals(-90, new AutoStrategyB().StartFacingDeg(), 1e-9);
        assertEquals(-90, new AutoStrategyC().StartFacingDeg(), 1e-9);
        assertEquals(90, new AutoStrategyGardenD().StartFacingDeg(), 1e-9);
        assertEquals(90, new AutoStrategyGardenE().StartFacingDeg(), 1e-9);
        // The backups start the same way as the auto they copy.
        assertEquals(90, new AutoStrategyARed().StartFacingDeg(), 1e-9);
        assertEquals(-90, new AutoStrategyCBlue().StartFacingDeg(), 1e-9);
    }

    @Test
    @DisplayName("an auto that does not say how it starts hands TeleOp nothing")
    void unknownStart() {
        assertTrue(Double.isNaN(new AutoStrategyD().StartFacingDeg()));
        assertTrue(Double.isNaN(new AutoShapeZ().StartFacingDeg()));
    }
}
