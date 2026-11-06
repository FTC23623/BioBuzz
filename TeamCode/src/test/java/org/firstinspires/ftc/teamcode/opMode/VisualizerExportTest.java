package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.firstinspires.ftc.teamcode.objects.FieldPose;
import org.firstinspires.ftc.teamcode.tools.PedroVisualizerFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The autos on the field, for the Pedro Pathing Visualizer (docs/decisions/0051).
 *
 * <p>Every test run writes one {@code .pp} file per auto and alliance to
 * {@code TeamCode/build/visualizer/}. Open visualizer.pedropathing.com, pick the BIOBUZZ
 * field, and open a file with its file menu to see the route and play it.
 *
 * <p>It also checks each route stays on the field, so a wrong start spot (or a recording
 * that drives through a wall) shows up here, not on the field.
 */
@DisplayName("Autos on the field (Pedro Pathing Visualizer files)")
class VisualizerExportTest {
    /** The robot's center may come this close to a wall, in inches: half the robot, less a little. */
    private static final double wallMarginIn = 5;

    private static final PedroAuto[] AUTOS = {new AutoStrategyA(), new AutoStrategyB(), new AutoStrategyC()};

    /** One auto's route as field spots: the start, then where each path ends. */
    static List<Pose> Route(PedroAuto auto, Pose start) {
        List<Pose> spots = new ArrayList<>();
        spots.add(start);
        for (Path path : auto.BuildPaths()) {
            if (!IsStep(path)) {
                Pose end = path.endPose();
                spots.add(FieldPose.ToField(start, new Pose(end.x(), end.y(), path.heading(1))));
            }
        }
        return spots;
    }

    /** true for a LAUNCH, FLOWER or INTAKE step: a stop, not a path to drive. */
    static boolean IsStep(Path path) {
        return StepName(path) != null;
    }

    static String StepName(Path path) {
        if (path == PedroAuto.LAUNCH) {
            return "LAUNCH";
        }
        if (path == PedroAuto.LAUNCH_WHEN_TAG_SEEN) {
            return "LAUNCH WHEN TAG SEEN";
        }
        if (path == PedroAuto.FLOWER) {
            return "FLOWER";
        }
        if (path == PedroAuto.INTAKE_ON) {
            return "INTAKE_ON";
        }
        if (path == PedroAuto.INTAKE_OFF) {
            return "INTAKE_OFF";
        }
        return null;
    }

    /** How long each stop holds, roughly, so the visualizer's timing is about right. */
    static double StepSeconds(String step, PedroAuto auto) {
        if (step.startsWith("LAUNCH")) {
            return auto.botProperties.autoLaunchSeconds;
        }
        if (step.equals("FLOWER")) {
            return auto.botProperties.autoFlowerSettleSeconds + 2 * auto.botProperties.autoExtractorSeconds
                    + auto.botProperties.autoFlowerPasses * auto.botProperties.autoFlowerDwellSeconds;
        }
        return 0;
    }

    /** The whole auto as a visualizer file, from this field start. */
    static String VisualizerFile(PedroAuto auto, Pose start) {
        PedroVisualizerFile file = new PedroVisualizerFile(start,
                auto.botProperties.frameLengthIn, auto.botProperties.frameWidthIn);
        int pathNumber = 0;
        for (Path path : auto.BuildPaths()) {
            String step = StepName(path);
            if (step != null) {
                file.AddWait(step, StepSeconds(step, auto));
                continue;
            }
            pathNumber = pathNumber + 1;
            Pose end = path.endPose();
            Pose fieldEnd = FieldPose.ToField(start, new Pose(end.x(), end.y(), 0));
            // Pedro keeps headings 0 to 360. Write the start between -180 and 180, and the end
            // as the start plus how far the path really turns, so the turn keeps its direction.
            double startDeg = FieldPose.WrapDegrees(Math.toDegrees(start.heading() + path.heading(0)));
            double endDeg = startDeg + TurnDegrees(path);
            file.AddPath("to spot " + pathNumber, fieldEnd, startDeg, endDeg);
        }
        return file.ToJson();
    }

    /**
     * How far a path turns the robot, in degrees, left positive, measured the way Pedro turns
     * it: small steps along the path, each the short way. A 12-degree right turn is -12,
     * not the 348 that "end heading less start heading" gives when Pedro wraps at 360.
     */
    static double TurnDegrees(Path path) {
        int steps = 50;
        double turn = 0;
        double last = path.heading(0);
        for (int i = 1; i <= steps; i++) {
            double now = path.heading((double) i / steps);
            turn = turn + FieldPose.WrapDegrees(Math.toDegrees(now - last));
            last = now;
        }
        return turn;
    }

    private static String Name(PedroAuto auto) {
        return auto.getClass().getAnnotation(Autonomous.class).name();
    }

    @Test
    @DisplayName("writes a blue and a red file for every placed auto, to TeamCode/build/visualizer/")
    void writeFiles() throws IOException {
        File folder = new File("build/visualizer");
        assertTrue(folder.exists() || folder.mkdirs(), "could not make " + folder.getAbsolutePath());
        for (PedroAuto auto : AUTOS) {
            Pose blue = auto.FieldStartBlue();
            assertNotNull(blue, Name(auto) + " has no FieldStartBlue()");
            Write(new File(folder, Name(auto) + " Blue.pp"), VisualizerFile(auto, blue));
            Write(new File(folder, Name(auto) + " Red.pp"), VisualizerFile(auto, FieldPose.OtherAlliance(blue)));
        }
    }

    private static void Write(File file, String text) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(text);
        writer.close();
    }

    @Test
    @DisplayName("every route stays on the field, from the start spot each auto gives")
    void routesStayOnTheField() {
        for (PedroAuto auto : AUTOS) {
            for (Pose spot : Route(auto, auto.FieldStartBlue())) {
                String where = String.format("%s at (%.1f, %.1f)", Name(auto), spot.x(), spot.y());
                assertTrue(spot.x() >= wallMarginIn && spot.x() <= FieldPose.FIELD_IN - wallMarginIn, where);
                assertTrue(spot.y() >= wallMarginIn && spot.y() <= FieldPose.FIELD_IN - wallMarginIn, where);
            }
        }
    }

    @Test
    @DisplayName("field spots: backing up 11 in from (105, 9) facing down is (105, 20)")
    void toField() {
        Pose start = new Pose(105, 9, Math.toRadians(-90));
        Pose spot = FieldPose.ToField(start, new Pose(-11, 0, 0));
        assertEquals(105, spot.x(), 1e-9);
        assertEquals(20, spot.y(), 1e-9);
        assertEquals(-90, FieldPose.WrapDegrees(Math.toDegrees(spot.heading())), 1e-9);
    }

    @Test
    @DisplayName("a path's turn keeps its direction: 0 to -12 degrees is 12 right, not 348 left")
    void turnDirection() {
        Path path = com.pedropathing.api.Paths.line(new Pose(0, 0), new Pose(-15, 1))
                .heading(com.pedropathing.paths.interpolator.Interpolator.linear(0, Math.toRadians(-12)));
        assertEquals(-12, TurnDegrees(path), 0.01);
    }

    @Test
    @DisplayName("red is blue turned half a turn about the middle of the field")
    void redFromBlue() {
        Pose red = FieldPose.OtherAlliance(new Pose(105, 9, Math.toRadians(-90)));
        assertEquals(36.5, red.x(), 1e-9);
        assertEquals(132.5, red.y(), 1e-9);
        assertEquals(90, FieldPose.WrapDegrees(Math.toDegrees(red.heading())), 1e-9);
    }

    @Test
    @DisplayName("the file: version, start spot, a path per drive and a named wait per LAUNCH / FLOWER")
    void fileShape() {
        PedroAuto auto = new AutoStrategyA();
        String json = VisualizerFile(auto, auto.FieldStartBlue());
        assertTrue(json.contains("\"version\": \"2.0.0\""));
        assertTrue(json.contains("\"startPoint\": {\"x\":103.75,\"y\":9.00,\"headingDeg\":-90.00}"), json);
        assertTrue(json.contains("\"name\":\"LAUNCH\""));
        assertTrue(json.contains("\"name\":\"FLOWER\""));
        assertTrue(json.contains("\"kind\":\"atomic\""));
    }
}
