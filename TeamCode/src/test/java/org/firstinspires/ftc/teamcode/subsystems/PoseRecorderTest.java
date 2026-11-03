package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedropathing.localization.Localizer;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The pose recorder with a pretend localizer, and a real folder that is thrown away after. */
@DisplayName("PoseRecorder")
class PoseRecorderTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private Localizer localizer;
    private Gamepad gamepad;
    private PoseRecorder recorder;

    @TempDir
    File folder;

    @BeforeEach
    void setUp() {
        localizer = mock(Localizer.class);
        when(localizer.pose()).thenReturn(new Pose(0, 0, 0));
        gamepad = mock(Gamepad.class);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), mock(HardwareMap.class), gamepad, gamepad);
        recorder = new PoseRecorder(op, properties, gamepad, localizer, folder);
    }

    private void robotIsAt(double x, double y, double headingDeg) {
        when(localizer.pose()).thenReturn(new Pose(x, y, Math.toRadians(headingDeg)));
    }

    private void tap(Consumer<Boolean> button) {
        for (int i = 0; i < properties.debounce + 2; i++) {
            button.accept(true);
            recorder.HandleUserInput();
        }
        button.accept(false);
        recorder.HandleUserInput();
    }

    private String fileText() throws Exception {
        File[] files = folder.listFiles();
        assertEquals(1, files.length);
        return new String(Files.readAllBytes(files[0].toPath()), "UTF-8");
    }

    @Test
    @DisplayName("INIT makes the robot's spot (0, 0)")
    void initZeroes() {
        recorder.Init();
        verify(localizer).setPose(any(Pose.class));
    }

    @Test
    @DisplayName("cross saves a spot, triangle a spot plus LAUNCH, square forgets the last")
    void buttons() throws Exception {
        robotIsAt(24, 0, 0);
        tap(down -> gamepad.cross = down);
        robotIsAt(24, -12, -90);
        tap(down -> gamepad.triangle = down);
        assertEquals(2, recorder.CountSpots());
        assertTrue(fileText().contains("paths.add(LAUNCH);"));

        tap(down -> gamepad.square = down);
        assertEquals(1, recorder.CountSpots());
        assertTrue(!fileText().contains("LAUNCH);"));
    }

    @Test
    @DisplayName("the Java: a Pose per spot, a line between each, LAUNCH after triangle spots")
    void java() {
        List<PoseRecorder.Spot> spots = new ArrayList<>();
        spots.add(new PoseRecorder.Spot(24, 0, 0, false));
        spots.add(new PoseRecorder.Spot(24, -12.25, -90, true));
        String java = PoseRecorder.ToJava(spots);

        assertTrue(java.contains("Pose spot1 = new Pose(24.0, 0.0);"));
        assertTrue(java.contains("Pose spot2 = new Pose(24.0, -12.3);"));
        assertTrue(java.contains("Path toSpot1 = line(StartPose(), spot1).constant(Math.toRadians(0));"));
        assertTrue(java.contains("Path toSpot2 = line(spot1, spot2)"
                + ".heading(Interpolator.linear(Math.toRadians(0), Math.toRadians(-90)));"));
        assertTrue(java.contains("paths.add(toSpot2);\npaths.add(LAUNCH);\nreturn paths;"));
    }

    @Test
    @DisplayName("heading: 350 right after 0 is written as -10, so the auto turns the short way")
    void unwrap() {
        assertEquals(-10, PoseRecorder.Unwrap(0, 350), EPS);
        assertEquals(190, PoseRecorder.Unwrap(170, -170), EPS);
        assertEquals(45, PoseRecorder.Unwrap(0, 45), EPS);
    }
}
