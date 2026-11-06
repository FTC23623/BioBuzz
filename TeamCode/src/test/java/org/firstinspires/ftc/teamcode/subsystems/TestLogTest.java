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
    private LaunchGates gates;
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
        gates = mock(LaunchGates.class);
        properties.tuningMarks = false;
        properties.shotDipRpm = 100;
        properties.shotRecoveredRpm = 30;
        properties.shotTimeoutSeconds = 1.5;
        log = new TestLog(op, properties, vision, launcher, gates, intake, null, new File(folder, "logs"));
    }

    private List<String> fileLines() throws Exception {
        return Files.readAllLines(log.GetFile().toPath());
    }

    @Test
    @DisplayName("both off (matches): no file, no lines on the screen")
    void offDoesNothing() {
        log.Process();
        assertNull(log.GetFile());
        verify(telemetry, never()).addData(eq("T launch"), any(Object.class));
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
    @DisplayName("testTelemetry on: one number per line, buttons 0 or 1, so the dashboard can graph them")
    void showsOnScreen() {
        properties.testTelemetry = true;
        driver.right_trigger = 1;
        operator.left_bumper = true;
        when(transfer.GetPower()).thenReturn(0.6);
        log.Process();
        verify(telemetry).addData("T launch", 1.0);
        verify(telemetry).addData("T aim", 0.0);
        verify(telemetry).addData("T intake_toggle", 1.0);
        verify(telemetry).addData("T transfer_power", 0.6);
        // No tag: the tilt is unknown (NaN), which a graph can't draw, so it is left out.
        verify(telemetry, never()).addData(eq("T tag_tilt_deg"), any(Object.class));
        assertNull(log.GetFile(), "the screen alone writes no file");
    }

    /** A TeleOp loop for the log: the gamepads read, then the log's Process. */
    private void loop() {
        log.HandleUserInput();
        log.Process();
    }

    @Test
    @DisplayName("a shot: the speed dips with the gates open, comes back; the driver marks it made")
    void shotAndMark() {
        properties.testTelemetry = true;
        properties.tuningMarks = true;
        when(launcher.GetPollenTargetRpm()).thenReturn(3000.0);
        when(gates.IsOpen()).thenReturn(true);
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetGroupRange()).thenReturn(126.0);
        when(launcher.GetPollenMeasuredRpm()).thenReturn(2990.0);
        loop();
        when(launcher.GetPollenMeasuredRpm()).thenReturn(2750.0);   // a ball going through
        loop();
        when(launcher.GetPollenMeasuredRpm()).thenReturn(2620.0);
        loop();
        when(launcher.GetPollenMeasuredRpm()).thenReturn(2985.0);   // back
        loop();
        assertEquals("1", log.Value("shots"));
        assertEquals("380", log.Value("shot_dip_rpm"));
        assertEquals("126", log.Value("shot_range_in"));
        assertEquals("0", log.Value("shot_mark"));
        for (int i = 0; i < properties.debounce + 2; i++) {
            driver.cross = true;
            loop();
        }
        driver.cross = false;
        loop();
        assertEquals("1", log.Value("shot_mark"));
        verify(driver).rumbleBlips(1);
    }

    @Test
    @DisplayName("marks off (every OpMode but Tool: Launch Tuning): cross does nothing here")
    void marksOff() {
        properties.testTelemetry = true;
        for (int i = 0; i < properties.debounce + 2; i++) {
            driver.cross = true;
            loop();
        }
        verify(driver, never()).rumbleBlips(1);
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
