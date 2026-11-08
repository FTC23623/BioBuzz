package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.firstinspires.ftc.teamcode.tools.MiniJson;
import org.firstinspires.ftc.teamcode.tools.PedroVisualizerImport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Visualizer files into our autos' Java (docs/decisions/0051). Every test run also turns
 * each {@code .pp} file in {@code TeamCode/visualizer/} into
 * {@code TeamCode/build/visualizer-import/<name>.java.txt}.
 */
@DisplayName("Pedro Pathing Visualizer files into autos")
class VisualizerImportTest {

    @Test
    @DisplayName("converts every .pp file in TeamCode/visualizer/ to TeamCode/build/visualizer-import/")
    void convertFolder() throws IOException {
        File in = new File("visualizer");
        File out = new File("build/visualizer-import");
        assertTrue(out.exists() || out.mkdirs());
        File[] files = in.listFiles((dir, name) -> name.endsWith(".pp"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            String json = new String(Files.readAllBytes(file.toPath()), "UTF-8");
            String java = PedroVisualizerImport.ToJava(json, file.getName());
            FileWriter writer = new FileWriter(new File(out, file.getName().replace(".pp", ".java.txt")));
            writer.write(java);
            writer.close();
        }
    }

    @Test
    @DisplayName("an auto drawn out and read back gives the same spots and steps as its code")
    void roundTrip() {
        PedroAuto auto = new AutoStrategyB();
        String json = VisualizerExportTest.VisualizerFile(auto, auto.FieldStartBlue());
        String java = PedroVisualizerImport.ToJava(json, "B.pp");
        // AutoStrategyB's own spots and steps, from its code.
        assertTrue(java.contains("Pose spot1 = new Pose(-15.4, 1.1);"), java);
        // Only the paths in the list are drawn, so B's last spot (spot4 in its code) reads back as spot2.
        assertTrue(java.contains("Pose spot2 = new Pose(-14.4, 49.9);"), java);
        assertTrue(java.contains("paths.add(toSpot1);\npaths.add(LAUNCH_WHEN_TAG_SEEN);\npaths.add(toSpot2);\nreturn paths;"), java);
        assertTrue(java.contains("return new Pose(55.75, 132.50, Math.toRadians(90));"), java);
        assertTrue(java.contains("Path toSpot1 = line(StartPose(), spot1)"
                + ".heading(Interpolator.linear(Math.toRadians(0), Math.toRadians(-12)));"), java);
    }

    @Test
    @DisplayName("a curve keeps its control point, measured from the start; an unknown wait is a comment")
    void curveAndUnknownWait() {
        String json = "{\"startPoint\":{\"x\":10,\"y\":10,\"headingDeg\":90},"
                + "\"lines\":[{\"id\":\"a\",\"kind\":\"atomic\",\"endPoint\":{\"x\":10,\"y\":40},"
                + "\"controlPoints\":[{\"x\":20,\"y\":25}],\"heading\":{\"type\":\"tangential\",\"reverse\":false}}],"
                + "\"sequence\":[{\"kind\":\"path\",\"lineId\":\"a\"},{\"kind\":\"wait\",\"id\":\"w\",\"name\":\"park\",\"durationMs\":500}]}";
        String java = PedroVisualizerImport.ToJava(json, "test.pp");
        // Facing up from (10, 10): 30 in up is 30 in forward; 10 in right is -10 in left.
        assertTrue(java.contains("Pose spot1 = new Pose(30.0, 0.0);"), java);
        assertTrue(java.contains("curve(StartPose(), new Pose(15.0, -10.0), spot1).tangent();"), java);
        assertTrue(java.contains("// wait \"park\", 500 ms: no step by that name"), java);
        assertTrue(java.contains("import static com.pedropathing.api.Paths.curve;"), java);
    }

    @Test
    @DisplayName("wait names: any case, spaces or underscores")
    void stepNames() {
        assertEquals("paths.add(LAUNCH);\n", PedroVisualizerImport.StepLine("launch", 0));
        assertEquals("paths.add(INTAKE_ON);\n", PedroVisualizerImport.StepLine("Intake on", 0));
        assertEquals("paths.add(LAUNCH_WHEN_TAG_SEEN);\n", PedroVisualizerImport.StepLine("LAUNCH WHEN TAG SEEN", 0));
    }

    @Test
    @DisplayName("JSON: objects, arrays, numbers, text with escapes, true / false / null; bad text throws")
    @SuppressWarnings("unchecked")
    void json() {
        Map<String, Object> value = (Map<String, Object>) MiniJson.Parse(
                "{\"a\": [1, -2.5e1, true, null], \"b\": \"say \\\"hi\\\"\\n\"}");
        List<Object> a = (List<Object>) value.get("a");
        assertEquals(1.0, a.get(0));
        assertEquals(-25.0, a.get(1));
        assertEquals(Boolean.TRUE, a.get(2));
        assertEquals(null, a.get(3));
        assertEquals("say \"hi\"\n", value.get("b"));
        assertThrows(IllegalArgumentException.class, () -> MiniJson.Parse("{\"a\": }"));
    }
}
