package org.firstinspires.ftc.teamcode.tools;

import com.pedropathing.math.Pose;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.firstinspires.ftc.teamcode.objects.FieldPose;

/**
 * Writes a {@code .pp} file for the Pedro Pathing Visualizer (visualizer.pedropathing.com):
 * open it there with its file menu to see an auto on the BIOBUZZ field, play it, and change
 * it (docs/decisions/0051).
 *
 * <p>A {@code .pp} file is JSON: where the robot starts, a list of paths (each a straight
 * line to an end point, with how the heading turns along it), and the order to run them in,
 * which can also hold named waits. Our LAUNCH, FLOWER and INTAKE steps go in as waits with
 * those names. Every number is on the field (see {@link
 * org.firstinspires.ftc.teamcode.objects.FieldPose}), in inches and degrees.
 *
 * <p>Laptop only: the robot never uses this. It is the visualizer's format 2.0.0.
 */
public class PedroVisualizerFile {
    /** The format version the visualizer reads. */
    public static final String VERSION = "2.0.0";
    /** The path colors, used in turn, so neighboring paths look different. */
    private static final String[] COLORS = {"#d99a00", "#2f6fdf", "#cc3d3d", "#2e9e5b", "#8a4fd1"};

    private final Pose mStart;
    private final double mRobotLengthIn;
    private final double mRobotWidthIn;
    private final List<String> mLines = new ArrayList<>();
    private final List<String> mSequence = new ArrayList<>();
    private int mWaits = 0;

    /**
     * @param start where the robot starts on the field, heading in radians
     */
    public PedroVisualizerFile(Pose start, double robotLengthIn, double robotWidthIn) {
        mStart = start;
        mRobotLengthIn = robotLengthIn;
        mRobotWidthIn = robotWidthIn;
    }

    /**
     * One straight path, to an end point on the field.
     *
     * @param startHeadingDeg the heading where the path starts, degrees
     * @param endHeadingDeg   the heading where it ends; the same number means it doesn't turn
     */
    public void AddPath(String name, Pose end, double startHeadingDeg, double endHeadingDeg) {
        int number = mLines.size() + 1;
        String id = "path" + number;
        String heading;
        if (Math.abs(endHeadingDeg - startHeadingDeg) < 0.5) {
            heading = String.format(Locale.US, "{\"type\":\"constant\",\"degrees\":%s}", Num(endHeadingDeg));
        } else {
            heading = String.format(Locale.US, "{\"type\":\"linear\",\"startDeg\":%s,\"endDeg\":%s}",
                    Num(startHeadingDeg), Num(endHeadingDeg));
        }
        mLines.add(String.format(Locale.US,
                "{\"id\":\"%s\",\"name\":\"%s\",\"color\":\"%s\",\"kind\":\"atomic\","
                        + "\"endPoint\":{\"x\":%s,\"y\":%s,\"name\":\"%s\"},\"controlPoints\":[],\"heading\":%s}",
                id, Text(name), COLORS[(number - 1) % COLORS.length], Num(end.x()), Num(end.y()),
                Text(name), heading));
        mSequence.add(String.format(Locale.US, "{\"kind\":\"path\",\"lineId\":\"%s\"}", id));
    }

    /** A named stop where the robot holds still: "LAUNCH", "FLOWER", "INTAKE_ON", ... */
    public void AddWait(String name, double seconds) {
        mWaits = mWaits + 1;
        mSequence.add(String.format(Locale.US,
                "{\"kind\":\"wait\",\"id\":\"wait%d\",\"name\":\"%s\",\"durationMs\":%d}",
                mWaits, Text(name), Math.round(seconds * 1000)));
    }

    /** The whole file. */
    public String ToJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": \"").append(VERSION).append("\",\n");
        json.append(String.format(Locale.US,
                "  \"startPoint\": {\"x\":%.2f,\"y\":%.2f,\"headingDeg\":%s},\n",
                mStart.x(), mStart.y(),
                Num(FieldPose.WrapDegrees(Math.toDegrees(mStart.heading())))));
        json.append("  \"lines\": [\n    ").append(Join(mLines, ",\n    ")).append("\n  ],\n");
        json.append("  \"shapes\": [],\n");
        json.append("  \"sequence\": [\n    ").append(Join(mSequence, ",\n    ")).append("\n  ],\n");
        json.append("  \"fieldPoints\": [],\n");
        json.append(String.format(Locale.US, "  \"robot\": {\"rLength\":%s,\"rWidth\":%s},\n",
                Num(mRobotLengthIn), Num(mRobotWidthIn)));
        json.append("  \"activePaths\": []\n");
        json.append("}\n");
        return json.toString();
    }

    /** A number to two decimal places: enough that reading a file back loses nothing that matters. */
    static String Num(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /** Text for JSON: quotes and backslashes escaped. */
    static String Text(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String Join(List<String> items, String between) {
        StringBuilder joined = new StringBuilder();
        for (String item : items) {
            if (joined.length() > 0) {
                joined.append(between);
            }
            joined.append(item);
        }
        return joined.toString();
    }
}
