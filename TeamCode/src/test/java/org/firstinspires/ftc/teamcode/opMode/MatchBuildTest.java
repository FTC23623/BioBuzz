package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The match build's list (TeamCode/match-build-excludes.txt, docs/decisions/0053) against
 * the OpModes themselves: everything for testing, tools and practice is on it, nothing a
 * match needs is.
 */
@DisplayName("Match build: which OpModes are left out")
class MatchBuildTest {
    private static final String PACKAGE = "org.firstinspires.ftc.teamcode.opMode.";

    /** The groups a match never uses. */
    private static final String[] NOT_FOR_MATCHES = {"Test", "Tool", "Practice", "Shapes", "Basic"};

    /** The OpModes a match build keeps. */
    private static final String[] MATCH = {
            "AutoStrategyA", "AutoStrategyB", "AutoStrategyC",
            "AutoStrategyABlue", "AutoStrategyBBlue", "AutoStrategyCBlue",
            "AutoStrategyARed", "AutoStrategyBRed", "AutoStrategyCRed",
            "AutoStrategyGardenD", "AutoStrategyGardenDBlue", "AutoStrategyGardenDRed",
            "AutoStrategyGardenE", "AutoStrategyGardenEBlue", "AutoStrategyGardenERed",
            "HyDrive", "HyDriveBlue", "HyDriveRed"};

    private static List<String> Excluded() throws IOException {
        List<String> names = new ArrayList<>();
        for (String line : Files.readAllLines(new File("match-build-excludes.txt").toPath())) {
            String name = line.trim();
            if (!name.isEmpty() && !name.startsWith("#")) {
                names.add(name);
            }
        }
        return names;
    }

    /** Every OpMode class: each .java in the opMode folder with @TeleOp or @Autonomous. */
    private static List<Class<?>> OpModes() throws ClassNotFoundException {
        List<Class<?>> opModes = new ArrayList<>();
        File folder = new File("src/main/java/org/firstinspires/ftc/teamcode/opMode");
        for (File file : folder.listFiles((dir, name) -> name.endsWith(".java"))) {
            Class<?> type = Class.forName(PACKAGE + file.getName().replace(".java", ""));
            if (type.isAnnotationPresent(TeleOp.class) || type.isAnnotationPresent(Autonomous.class)) {
                opModes.add(type);
            }
        }
        return opModes;
    }

    private static String Group(Class<?> type) {
        if (type.isAnnotationPresent(TeleOp.class)) {
            return type.getAnnotation(TeleOp.class).group();
        }
        return type.getAnnotation(Autonomous.class).group();
    }

    @Test
    @DisplayName("every name on the list is a real OpMode in the opMode folder")
    void listedAreReal() throws Exception {
        for (String name : Excluded()) {
            Class<?> type = Class.forName(PACKAGE + name);
            assertTrue(type.isAnnotationPresent(TeleOp.class) || type.isAnnotationPresent(Autonomous.class), name);
        }
    }

    @Test
    @DisplayName("every Test / Tool / Practice / Shapes / Basic OpMode is left out of a match build")
    void testAndToolLeftOut() throws Exception {
        List<String> excluded = Excluded();
        for (Class<?> type : OpModes()) {
            for (String group : NOT_FOR_MATCHES) {
                if (group.equals(Group(type))) {
                    assertTrue(excluded.contains(type.getSimpleName()),
                            type.getSimpleName() + " (group " + group + ") is missing from match-build-excludes.txt");
                }
            }
        }
    }

    @Test
    @DisplayName("the match autos and the HyDrives stay in")
    void matchKept() throws Exception {
        List<String> excluded = Excluded();
        for (String name : MATCH) {
            assertFalse(excluded.contains(name), name + " must stay in a match build");
        }
    }
}
