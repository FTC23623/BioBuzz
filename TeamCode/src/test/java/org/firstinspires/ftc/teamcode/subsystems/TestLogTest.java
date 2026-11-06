package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The test log and test telemetry (docs/decisions/0050). */
@DisplayName("TestLog")
class TestLogTest {
    private final BotProperties properties = new BotProperties();
    private Telemetry telemetry;
    private Gamepad driver;
    private Gamepad operator;
    private Vision vision;
    private Launcher launcher;
    private Intake intake;
    private Roller transfer;
    private TestLog log;

    @TempDir
    File folder;

    @BeforeEach
    void setUp() {
        properties.testLog = false;
        properties.testTelemetry = false;
        telemetry = mock(Telemetry.class);
        driver = mock(Gamepad.class);
        operator = mock(Gamepad.class);
        HydraOpMode op = new HydraOpMode(telemetry, mock(HardwareMap.class), driver, operator);
        vision = mock(Vision.class);
        launcher = mock(Launcher.class);
        intake = mock(Intake.class);
        transfer = mock(Roller.class);
        when(intake.GetTransferRoller()).thenReturn(transfer);
        log = new TestLog(op, properties, vision, launcher, null, intake, new File(folder, "logs"));
    }

    private List<String> fileLines() throws Exception {
        return Files.readAllLines(log.GetFile().toPath());
    }

    @Test
    @DisplayName("both off (matches): no file, no lines on the screen")
    void offDoesNothing() {
        log.Process();
        assertNull(log.GetFile());
        verify(telemetry, never()).addData(eq("Test: driver held"), any(Object.class));
    }

    @Test
    @DisplayName("testLog on: column names first, then one line per loop with the buttons and numbers")
    void writesLines() throws Exception {
        properties.testLog = true;
        driver.right_trigger = 1;   // launch
        driver.right_bumper = true; // aim
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetRange()).thenReturn(124.5);
        when(transfer.GetTargetRpm()).thenReturn(690.0);
        when(transfer.GetPower()).thenReturn(0.6);
        for (int i = 0; i < TestLog.linesPerFlush; i++) {
            log.Process();
        }
        List<String> lines = fileLines();
        assertEquals(1 + TestLog.linesPerFlush, lines.size());
        List<String> names = Arrays.asList(lines.get(0).split(",", -1));
        List<String> row = Arrays.asList(lines.get(1).split(",", -1));
        assertEquals(names.size(), row.size());
        assertEquals("1", row.get(names.indexOf("launch")));
        assertEquals("1", row.get(names.indexOf("aim")));
        assertEquals("0", row.get(names.indexOf("slow")));
        assertEquals("124.500", row.get(names.indexOf("range_in")));
        assertEquals("690", row.get(names.indexOf("transfer_target_rpm")));
        assertEquals("0.600", row.get(names.indexOf("transfer_power")));
    }

    @Test
    @DisplayName("testTelemetry on: the buttons held, by what they do, and the transfer on the screen")
    void showsOnScreen() {
        properties.testTelemetry = true;
        driver.right_trigger = 1;
        operator.left_bumper = true;
        log.Process();
        assertEquals("launch", log.Held(0, 100).split(", ")[0]);
        verify(telemetry).addData("Test: driver held", "launch");
        verify(telemetry).addData("Test: operator held", "intake_toggle");
        assertNull(log.GetFile(), "the screen alone writes no file");
    }

    @Test
    @DisplayName("numbers: whole ones plain, others to three places, unknown (NaN) empty")
    void numbers() {
        assertEquals("1150", TestLog.Number(1150));
        assertEquals("-0.348", TestLog.Number(-400.0 / 1150));
        assertEquals("", TestLog.Number(Double.NaN));
        assertTrue(TestLog.CsvLine(Arrays.asList("a", "b", "")).equals("a,b,"));
    }
}
