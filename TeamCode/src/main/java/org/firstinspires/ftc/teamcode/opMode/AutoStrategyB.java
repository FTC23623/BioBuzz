package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts on the far side of the field, facing the hive. Creeps forward a few inches, then
 * waits. The hive's AprilTags only show once its basket tips up, so seeing a tag means it
 * tipped. When the camera sees one, the robot aims at the middle of the hive's tags and
 * launches.
 *
 * <p>Use this when our alliance partner can't wait: they launch first, and we launch after
 * the hive tips.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * See {@link PedroAuto} for more.
 */
// No group: the Driver Station lists these first, before the "Backup" ones. At the end it
// picks HyDrive, so the drivers only press INIT and START (docs/decisions/0046).
@Autonomous(name = AutoStrategyB.NAME, preselectTeleOp = HyDrive.NAME)
public class AutoStrategyB extends PedroAuto {
    /** The name on the Driver Station. The backups add their color to it. */
    public static final String NAME = "Flower Side Auto B";

    /** The robot starts facing the driver's left. */
    @Override
    protected double StartFacingDeg() {
        return 90;
    }

    /** How far the robot creeps forward off the start, in inches. */
    private static final double creepForwardIn = 3;

    @Override
    protected List<Path> BuildPaths() {
        // Half speed while we check this path on the field. Put it back to 1 for matches.
        botProperties.autoSpeedScale = 0.5;

        double faceForward = 0;
        Pose start = StartPose();
        Pose creptForward = new Pose(-creepForwardIn, 0);

        // Step 1: drive forward a few inches, still facing the hive.
        Path creep = line(start, creptForward).constant(faceForward);

        List<Path> paths = new ArrayList<>();
        paths.add(creep);
        paths.add(LAUNCH_WHEN_TAG_SEEN);    // Step 2: wait for the hive to tip, aim, then launch
        return paths;
    }
}
