package org.firstinspires.ftc.teamcode.pedro;

import static com.pedropathing.api.Paths.curve;
import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The shapes the practice autos drive. No hardware here, so the shapes can be tested on a
 * laptop (see ShapePathsTest).
 *
 * <p><b>How Pedro measures</b>: inches and radians. Every auto starts the robot at
 * {@code (0, 0)} facing heading 0. From there:
 * <ul>
 *   <li>x grows as the robot drives FORWARD
 *   <li>y grows as the robot drives LEFT
 *   <li>heading grows as the robot turns LEFT. 90 degrees is Math.toRadians(90).
 * </ul>
 *
 * <p>Pictures below are from the driver's view, standing behind the robot. "R" is where the
 * robot starts, facing up the page.
 */
public final class ShapePaths {
    private ShapePaths() {
    }

    /** How big each shape is, in inches. One field tile is 24 inches, so 48 is two tiles. */
    public static final double shapeSize = 48;

    /** Where every shape starts: here, facing forward. */
    public static final Pose startPose = new Pose(0, 0, 0);

    /**
     * A C with turning. The robot drives forward, curves left like a car, and comes back
     * toward the driver. It ends two tiles to the left, facing the driver.
     *
     * <pre>
     *    .----.
     *   /      \
     *  |        |
     *  v        R
     * </pre>
     */
    public static List<Path> CWithTurning() {
        Path cCurve = CCurve().tangent();
        List<Path> paths = new ArrayList<>();
        paths.add(cCurve);
        return paths;
    }

    /**
     * The same C, without turning. Mecanum wheels can slide sideways, so the robot faces
     * forward the whole time: forward, then sliding left, then backing up.
     */
    public static List<Path> CWithoutTurning() {
        double faceForward = 0;
        Path cCurve = CCurve().constant(faceForward);
        List<Path> paths = new ArrayList<>();
        paths.add(cCurve);
        return paths;
    }

    /**
     * A Z. The robot faces forward the whole time and makes sharp corners, which only a
     * mecanum robot can do: slide left, drive diagonally forward and right, slide left.
     * (The robot draws the Z starting from its bottom-right corner.)
     *
     * <pre>
     *  4 &lt;--- 3
     *        /
     *      /
     *    /
     *  2 &lt;--- R
     * </pre>
     */
    public static List<Path> Z() {
        double faceForward = 0;
        Pose corner2 = new Pose(0, shapeSize);
        Pose corner3 = new Pose(shapeSize, 0);
        Pose corner4 = new Pose(shapeSize, shapeSize);

        Path bottom = line(startPose, corner2).constant(faceForward);
        Path diagonal = line(corner2, corner3).constant(faceForward);
        Path top = line(corner3, corner4).constant(faceForward);

        List<Path> paths = new ArrayList<>();
        paths.add(bottom);
        paths.add(diagonal);
        paths.add(top);
        return paths;
    }

    /**
     * The curve both C shapes share, with no heading picked yet. A Bezier curve starts at the
     * first point and ends at the last. The middle two are "pull" points: the curve bends
     * toward them but never touches them.
     */
    private static Path CCurve() {
        Pose end = new Pose(0, shapeSize);
        Pose pullForward = new Pose(shapeSize, 0);
        Pose pullForwardAndLeft = new Pose(shapeSize, shapeSize);
        return curve(startPose, pullForward, pullForwardAndLeft, end);
    }
}
