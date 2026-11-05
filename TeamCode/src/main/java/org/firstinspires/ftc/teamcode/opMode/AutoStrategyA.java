package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts with its front (the intake) on the wall. Backs up just far enough to spin without a
 * corner hitting the wall, launches, turns right 90, then puts its left side on the wall and
 * drives along it to collect pollen. Then it moves off the wall, turns right again to face the
 * middle of the field, and drives on toward the far side. Last, it slides right, turns left
 * 20 degrees to look at the hive's AprilTags, waits until it sees one, aims at the middle of
 * the hive's tags, and launches.
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * The robot starts facing the wall, so backing up is minus x and the garden (to its right) is
 * minus y. See {@link PedroAuto} for more.
 */
// No group: the Driver Station lists these first, before the "Backup" ones. At the end it
// picks HyDrive, so the drivers only press INIT and START (docs/decisions/0046).
@Autonomous(name = AutoStrategyA.NAME, preselectTeleOp = HyDrive.NAME)
public class AutoStrategyA extends PedroAuto {
    /** The name on the Driver Station. The backups add their color to it. */
    public static final String NAME = "Garden Side Auto A";

    /** The robot starts facing the driver's right. */
    @Override
    protected double StartFacingDeg() {
        return -90;
    }

    /** How far right the robot slides while it turns, in inches. */
    private static final double turnSlideIn = 3;
    /** How far right of the start the pollen run starts, in inches. */
    private static final double collectStartIn = 24;
    /** How far right of the start the pollen run ends, in inches. */
    private static final double collectEndIn = 48;
    /** How far the robot drives toward the far side of the field, in inches. Clears the hive stand. */
    private static final double driveToMiddleIn = 102;
    /** How far the robot slides back toward the start while turning, to miss the flower. */
    private static final double flowerClearanceIn = 12;
    /** How far right the robot slides before it aims at the tag, in inches. */
    private static final double strafeRightIn = 24;
    /** Part of that slide happens during the aiming turn, since Pedro can't turn in place. */
    private static final double aimTurnSlideIn = 2;
    /** How far LEFT the robot turns to see the AprilTag, in degrees. Left is toward the hive. */
    private static final double aimTurnDeg = 20;

    @Override
    protected List<Path> BuildPaths() {
        // Half speed while we check this path on the field. Put it back to 1 for matches.
        botProperties.autoSpeedScale = 0.5;

        double length = botProperties.frameLengthIn;
        double width = botProperties.frameWidthIn;
        double faceWall = 0;
        double faceRight = Math.toRadians(-90);
        double faceMiddle = Math.toRadians(-180);
        // Heading grows turning left, so add.
        double faceTag = faceMiddle + Math.toRadians(aimTurnDeg);

        // A spinning robot sweeps a circle out to its corners. Its front starts on the wall, so
        // it must back up by (half the diagonal - half the length) before it can turn.
        double halfDiagonal = Math.hypot(length, width) / 2;
        double distanceFromWall = halfDiagonal - (length / 2) + botProperties.turnClearanceIn;

        // After turning right 90, the LEFT side faces the wall. The front was half the length
        // from the center, the left side is half the width, so this x puts the left side on it.
        double leftSideOnWallX = (length / 2) - (width / 2);

        // Right of the start is minus y, so every garden y is negative.
        Pose start = StartPose();
        Pose launchSpot = new Pose(-distanceFromWall, 0);
        Pose turned = new Pose(-distanceFromWall, -turnSlideIn);
        Pose collectStart = new Pose(leftSideOnWallX, -collectStartIn);
        Pose collectEnd = new Pose(leftSideOnWallX, -collectEndIn);
        Pose offWallAgain = new Pose(-distanceFromWall, -collectEndIn);
        // Sliding back toward the start adds to y.
        Pose turnedAgain = new Pose(-distanceFromWall, -collectEndIn + flowerClearanceIn);
        Pose nearMiddle = new Pose(-distanceFromWall - driveToMiddleIn, -collectEndIn + flowerClearanceIn);
        // Facing the middle, the robot's right is plus y.
        Pose strafed = new Pose(nearMiddle.x(), nearMiddle.y() + strafeRightIn - aimTurnSlideIn);
        Pose aimed = new Pose(nearMiddle.x(), nearMiddle.y() + strafeRightIn);

        // Step 1: back up off the wall, still facing it.
        Path backUpToLaunch = line(start, launchSpot).constant(faceWall);
        // Step 3: turn right while sliding a little right. Off the wall, so no corner hits.
        Path turnRightOffWall = line(launchSpot, turned).heading(Interpolator.linear(faceWall, faceRight));
        // Step 4: drive right and toward the wall, ending with the left side on it at the garden.
        Path driveToGarden = line(turned, collectStart).constant(faceRight);
        // Step 5: drive forward along the wall to collect pollen.
        Path collectPollen = line(collectStart, collectEnd).constant(faceRight);
        // Step 6: move off the wall again, far enough to turn.
        Path moveOffWall = line(collectEnd, offWallAgain).constant(faceRight);
        // Step 7: turn right again to face the middle, sliding toward the start to miss the flower.
        Path turnToMiddle = line(offWallAgain, turnedAgain).heading(Interpolator.linear(faceRight, faceMiddle));
        // Step 8: drive forward, toward the middle of the field.
        Path driveToMiddle = line(turnedAgain, nearMiddle).constant(faceMiddle);
        // Step 9: slide right, still facing the far side.
        Path strafeRight = line(nearMiddle, strafed).constant(faceMiddle);
        // Step 10: turn left 20, toward the hive, to see the AprilTag, sliding the last bit of the strafe.
        Path aimAtTag = line(strafed, aimed).heading(Interpolator.linear(faceMiddle, faceTag));

        List<Path> paths = new ArrayList<>();
        paths.add(backUpToLaunch);
        paths.add(LAUNCH);              // Step 2: stop and launch
        paths.add(turnRightOffWall);
        paths.add(driveToGarden);
        paths.add(collectPollen);
        paths.add(moveOffWall);
        paths.add(turnToMiddle);
        paths.add(driveToMiddle);
        paths.add(strafeRight);
        paths.add(aimAtTag);
        paths.add(LAUNCH_WHEN_TAG_SEEN);    // Step 11: wait for the tag, aim, then launch
        return paths;
    }
}
