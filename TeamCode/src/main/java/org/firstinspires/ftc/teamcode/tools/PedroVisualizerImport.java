package org.firstinspires.ftc.teamcode.tools;

import com.pedropathing.math.Pose;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.firstinspires.ftc.teamcode.objects.FieldPose;

/**
 * Turns a {@code .pp} file drawn in the Pedro Pathing Visualizer (visualizer.pedropathing.com)
 * into Java for one of our autos' {@code BuildPaths()}, the same shape Tool: Pose Recorder
 * writes (docs/decisions/0051).
 *
 * <p>The visualizer measures from the field's corner; our autos measure from their start
 * spot. So every spot is moved to be "from the start", and the file's start spot becomes
 * the auto's {@code FieldStartBlue()}, for drawing it again later.
 *
 * <p>Named waits in the file become our steps: a wait named {@code LAUNCH}, {@code LAUNCH
 * WHEN TAG SEEN}, {@code FLOWER}, {@code INTAKE_ON} or {@code INTAKE_OFF} (any case) becomes
 * that step. Any other wait becomes a comment: our autos have no plain wait step.
 *
 * <p>Laptop only: the robot never uses this.
 */
public final class PedroVisualizerImport {
    private PedroVisualizerImport() {
    }

    /**
     * The Java for a visualizer file.
     *
     * @param json     the {@code .pp} file's text
     * @param fileName shown in the first comment, so the Java says where it came from
     */
    @SuppressWarnings("unchecked")
    public static String ToJava(String json, String fileName) {
        Map<String, Object> file = (Map<String, Object>) MiniJson.Parse(json);
        Map<String, Object> startPoint = (Map<String, Object>) file.get("startPoint");
        if (startPoint == null || !(file.get("lines") instanceof List)) {
            throw new IllegalArgumentException(fileName + " isn't a visualizer path file: no start point or paths");
        }
        double startX = Number(startPoint, "x", 0);
        double startY = Number(startPoint, "y", 0);
        double startDeg = Number(startPoint, "headingDeg", 0);
        Pose start = new Pose(startX, startY, Math.toRadians(startDeg));

        // Every path by its id, groups opened up into their parts, in file order.
        Map<String, Map<String, Object>> paths = new LinkedHashMap<>();
        for (Object line : (List<Object>) file.get("lines")) {
            AddPath(paths, (Map<String, Object>) line);
        }
        // The order to run them in. A file with no sequence runs its paths in order.
        List<Map<String, Object>> sequence = new ArrayList<>();
        if (file.get("sequence") instanceof List && !((List<Object>) file.get("sequence")).isEmpty()) {
            for (Object item : (List<Object>) file.get("sequence")) {
                sequence.add((Map<String, Object>) item);
            }
        } else {
            for (String id : paths.keySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("kind", "path");
                item.put("lineId", id);
                sequence.add(item);
            }
        }

        StringBuilder poses = new StringBuilder();
        StringBuilder pathLines = new StringBuilder();
        StringBuilder adds = new StringBuilder();
        String from = "StartPose()";
        double fromDeg = 0;
        int spot = 0;
        boolean curves = false;
        for (Map<String, Object> item : sequence) {
            if ("wait".equals(item.get("kind"))) {
                adds.append(StepLine(String.valueOf(item.get("name")), Number(item, "durationMs", 0)));
                continue;
            }
            List<Map<String, Object>> parts = Parts(paths, String.valueOf(item.get("lineId")));
            for (Map<String, Object> path : parts) {
                spot = spot + 1;
                String to = "spot" + spot;
                Map<String, Object> end = (Map<String, Object>) path.get("endPoint");
                Pose endFromStart = FieldPose.FromField(start,
                        new Pose(Number(end, "x", 0), Number(end, "y", 0), 0));
                double[] heading = Heading((Map<String, Object>) path.get("heading"), startDeg, fromDeg);
                poses.append(String.format(Locale.US, "Pose %s = new Pose(%.1f, %.1f);   // heading %.0f deg\n",
                        to, endFromStart.x(), endFromStart.y(), heading[1]));

                List<String> controls = new ArrayList<>();
                if (path.get("controlPoints") instanceof List) {
                    for (Object control : (List<Object>) path.get("controlPoints")) {
                        Map<String, Object> point = (Map<String, Object>) control;
                        Pose c = FieldPose.FromField(start, new Pose(Number(point, "x", 0), Number(point, "y", 0), 0));
                        controls.add(String.format(Locale.US, "new Pose(%.1f, %.1f)", c.x(), c.y()));
                    }
                }
                String shape;
                if (controls.isEmpty()) {
                    shape = "line(" + from + ", " + to + ")";
                } else {
                    curves = true;
                    StringBuilder points = new StringBuilder();
                    for (String control : controls) {
                        points.append(control).append(", ");
                    }
                    shape = "curve(" + from + ", " + points + to + ")";
                }
                pathLines.append("Path to").append(Capital(to)).append(" = ").append(shape)
                        .append(HeadingCode((Map<String, Object>) path.get("heading"), heading)).append(";\n");
                adds.append("paths.add(to").append(Capital(to)).append(");\n");
                from = to;
                fromDeg = heading[1];
            }
        }

        StringBuilder java = new StringBuilder();
        java.append("// From the Pedro Pathing Visualizer: ").append(fileName).append(". ")
                .append(spot).append(" paths.\n");
        java.append("// Paste into BuildPaths() of a copy of AutoShapeZ. It also needs:\n");
        java.append("//     import static com.pedropathing.api.Paths.line;\n");
        if (curves) {
            java.append("//     import static com.pedropathing.api.Paths.curve;\n");
        }
        java.append("//     import com.pedropathing.paths.interpolator.Interpolator;\n");
        java.append(String.format(Locale.US,
                "// The file's start spot, for the auto's FieldStartBlue() (blue; red is turned half a turn):\n"
                        + "//     return new Pose(%.2f, %.2f, Math.toRadians(%.0f));\n\n",
                startX, startY, startDeg));
        java.append(poses).append("\n").append(pathLines);
        java.append("\nList<Path> paths = new ArrayList<>();\n").append(adds).append("return paths;\n");
        return java.toString();
    }

    /** Remember a path by its id; a group (compound) is remembered as its parts, in order. */
    @SuppressWarnings("unchecked")
    private static void AddPath(Map<String, Map<String, Object>> paths, Map<String, Object> path) {
        paths.put(String.valueOf(path.get("id")), path);
        if (path.get("segments") instanceof List) {
            for (Object part : (List<Object>) path.get("segments")) {
                AddPath(paths, (Map<String, Object>) part);
            }
        }
    }

    /** The straight or curved parts to drive for one sequence item. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> Parts(Map<String, Map<String, Object>> paths, String id) {
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> path = paths.get(id);
        if (path == null) {
            return parts;
        }
        if (path.get("segments") instanceof List) {
            for (Object part : (List<Object>) path.get("segments")) {
                parts.addAll(Parts(paths, String.valueOf(((Map<String, Object>) part).get("id"))));
            }
        } else {
            parts.add(path);
        }
        return parts;
    }

    /**
     * The heading at a path's start and end, in degrees from the auto's start heading.
     * Facing along the path (tangential) and piecewise headings keep the last heading here;
     * the code still says what the file asked for.
     */
    private static double[] Heading(Map<String, Object> heading, double startDeg, double fromDeg) {
        String type = heading == null ? "" : String.valueOf(heading.get("type"));
        if (type.equals("constant")) {
            double deg = FieldPose.WrapDegrees(Number(heading, "degrees", startDeg) - startDeg);
            return new double[] {deg, deg};
        }
        if (type.equals("linear")) {
            double first = FieldPose.WrapDegrees(Number(heading, "startDeg", startDeg) - startDeg);
            double turn = Number(heading, "endDeg", startDeg) - Number(heading, "startDeg", startDeg);
            return new double[] {first, first + turn};
        }
        return new double[] {fromDeg, fromDeg};
    }

    /** The heading part of the path's code. */
    private static String HeadingCode(Map<String, Object> heading, double[] deg) {
        String type = heading == null ? "" : String.valueOf(heading.get("type"));
        if (type.equals("tangential")) {
            if (Boolean.TRUE.equals(heading.get("reverse"))) {
                return ".reverseTangent()";
            }
            return ".tangent()";
        }
        if (type.equals("piecewise")) {
            return String.format(Locale.US,
                    ".constant(Math.toRadians(%.0f))   // was a piecewise heading in the visualizer: set it by hand", deg[1]);
        }
        if (Math.abs(deg[1] - deg[0]) < 1) {
            return String.format(Locale.US, ".constant(Math.toRadians(%.0f))", deg[1]);
        }
        return String.format(Locale.US, ".heading(Interpolator.linear(Math.toRadians(%.0f), Math.toRadians(%.0f)))",
                deg[0], deg[1]);
    }

    /** A named wait as one of our steps, or as a comment when no step matches. */
    public static String StepLine(String name, double durationMs) {
        String step = name.trim().toUpperCase(Locale.US).replace(' ', '_');
        if (step.equals("LAUNCH") || step.equals("FLOWER") || step.equals("INTAKE_ON")
                || step.equals("INTAKE_OFF") || step.equals("LAUNCH_WHEN_TAG_SEEN")) {
            return "paths.add(" + step + ");\n";
        }
        return String.format(Locale.US, "// wait \"%s\", %.0f ms: no step by that name, so nothing here\n",
                name, durationMs);
    }

    private static double Number(Map<String, Object> object, String key, double fallback) {
        Object value = object == null ? null : object.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        return fallback;
    }

    /** "spot1" becomes "Spot1", for the path names. */
    private static String Capital(String word) {
        return word.substring(0, 1).toUpperCase(Locale.US) + word.substring(1);
    }
}
