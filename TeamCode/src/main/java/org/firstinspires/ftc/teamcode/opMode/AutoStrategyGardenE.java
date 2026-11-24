package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.*;

import com.pedropathing.api.Paths;
import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import java.util.ArrayList;
import java.util.List;

/**
 * A copy of {@link AutoStrategyGardenD} (Garden Side D) that skips the second flower. Same
 * start, same spots.
 *
 * <p>The route: from the start, back up to spot 1 and launch. Out past the hive (spot 2, the
 * stop that keeps the robot clear of the hive) to the first flower (spot 3) and work it. Back
 * the same way, through spot 2, to spot 1, and launch again there. Then the difference from
 * D: no second flower. It heads onto A's way across the field (spot 4) and straight on to the
 * final spot, the loading zone (spot 9).
 *
 * <p>Pedro measures in inches: x grows as the robot drives forward, y grows as it drives left.
 * The robot starts facing the wall, so backing up is minus x and the garden (to its right) is
 * minus y. See {@link PedroAuto} for more.
 */
// Group "Auto" (it reads the alliance magnet), like A. At the end it picks HyDrive.
@Autonomous(name = "Auto: " + AutoStrategyGardenE.NAME, group = "Auto", preselectTeleOp = HyDrive.NAME)
public class AutoStrategyGardenE extends PedroAuto {
    /** The auto's name. On the Driver Station: "Auto: ", "Blue: " or "Red: " in front. */
    public static final String NAME = "Garden Side E";

    /** The same start as Garden Side A. Blue; red is turned half a turn (0051). */
    @Override
    protected Pose FieldStartBlue() {
        return new Pose(55.75, 9, Math.toRadians(-90));
    }
    /** The robot starts facing the driver's left, like Garden Side A. */
    @Override
    protected double StartFacingDeg() {
        return 90;
    }
    @Override
    protected List<Path> BuildPaths() {
        // Spots copied from AutoStrategyA (recorded with Tool: Pose Recorder).
        PoseFactory poseFactory = PoseFactory.degrees();

        Pose start = poseFactory.of(55.1393, 8.8607, 90);
        Pose launchSpot1 = poseFactory.of(50.8358, 24.983, 265);
        Pose launchSpot1Segment1Start = poseFactory.of(50.8358, 24.983, 270);
        Pose launchSpot1Segment1End = poseFactory.of(50.8358, 24.983, 265);
        Pose gardenFlowerPickup = poseFactory.of(11.9638, 46.8224, 180);
        Pose gardenFlowerPickupSegment1Start = poseFactory.of(11.9638, 46.8224, 270);
        Pose gardenFlowerPickupSegment1End = poseFactory.of(11.9638, 46.8224, 180);
        Pose gardenFlowerPickupSegment2Start = poseFactory.of(11.9638, 46.8224, 180);
        Pose gardenFlowerPickupSegment2End = poseFactory.of(11.9638, 46.8224, 180);
        Pose tipDetectSpot = poseFactory.of(22.0341, 46.9085, 180);
        Pose launchSpot2 = poseFactory.of(43.7238, 126.3516, 95);
        Pose launchSpot2Segment1Start = poseFactory.of(43.7238, 126.3516, 180);
        Pose launchSpot2Segment1End = poseFactory.of(43.7238, 126.3516, 65);
        Pose launchSpot2Segment2Start = poseFactory.of(43.7238, 126.3516, 65);
        Pose launchSpot2Segment2End = poseFactory.of(43.7238, 126.3516, 95);
        Pose homeFlowerPickup = poseFactory.of(46.4065, 130.11, 90);
        Pose homeFlowerPickupSegment1Start = poseFactory.of(46.4065, 130.11, 95);
        Pose homeFlowerPickupSegment1End = poseFactory.of(46.4065, 130.11, 90);
        Pose launchSpot3 = poseFactory.of(31.3301, 120.7353, 120);
        Pose launchSpot3Segment1Start = poseFactory.of(31.3301, 120.7353, 90);
        Pose launchSpot3Segment1End = poseFactory.of(31.3301, 120.7353, 120);
        Pose home = poseFactory.of(15.1484, 117.7445, 120);

        Pose bLaunchSpot2 = poseFactory.of(35.8914, 18.0748, 245);
        Pose bLaunchSpot2Segment1Start = poseFactory.of(35.8914, 18.0748, 180);
        Pose bLaunchSpot2Segment1End = poseFactory.of(35.8914, 18.0748, 245);
        Pose bHiveTunnelStart = poseFactory.of(58.8007, 42.318, 270);
        Pose bHiveTunnelStartSegment1Start = poseFactory.of(58.8007, 42.318, 245);
        Pose bHiveTunnelStartSegment1End = poseFactory.of(58.8007, 42.318, 270);
        Pose bHiveTunnelEnd = poseFactory.of(58.5284, 99.9062, -90);
        Pose bHiveTunnelEndSegment1Heading = poseFactory.of(58.5284, 99.9062, 270);
        Pose bFlowerPickup = poseFactory.of(46.8224, 131.0855, 270);
        Pose bLauchSpot3 = poseFactory.of(33.3954, 119.2938, 300);
        Pose bLauchSpot3Segment1Start = poseFactory.of(33.3954, 119.2938, 270);
        Pose bLauchSpot3Segment1End = poseFactory.of(33.3954, 119.2938, 300);

        Path launchSpot1Path = Paths.line(start, launchSpot1).heading(Interpolator.piecewise().until(1, Interpolator.linear(launchSpot1Segment1Start, launchSpot1Segment1End)));
        Path gardenFlowerPickupPath = Paths.line(launchSpot1, gardenFlowerPickup).heading(Interpolator.piecewise().until(0.5, Interpolator.linear(gardenFlowerPickupSegment1Start, gardenFlowerPickupSegment1End)).until(1, Interpolator.linear(gardenFlowerPickupSegment2Start, gardenFlowerPickupSegment2End)));
        Path tipDetectSpotPath = Paths.line(gardenFlowerPickup, tipDetectSpot).constant(tipDetectSpot);
        Path launchSpot2Path = Paths.line(tipDetectSpot, launchSpot2).heading(Interpolator.piecewise().until(0.25, Interpolator.linear(launchSpot2Segment1Start, launchSpot2Segment1End)).until(1, Interpolator.linear(launchSpot2Segment2Start, launchSpot2Segment2End)));
        Path homeFlowerPickupPath = Paths.line(launchSpot2, homeFlowerPickup).heading(Interpolator.piecewise().until(1, Interpolator.linear(homeFlowerPickupSegment1Start, homeFlowerPickupSegment1End)));
        Path launchSpot3Path = Paths.line(homeFlowerPickup, launchSpot3).heading(Interpolator.piecewise().until(1, Interpolator.linear(launchSpot3Segment1Start, launchSpot3Segment1End)));
        Path homePath = Paths.line(launchSpot3, home).constant(home);

        Path bLaunchSpot2Path = Paths.line(tipDetectSpot, bLaunchSpot2).heading(Interpolator.piecewise().until(1, Interpolator.linear(bLaunchSpot2Segment1Start, bLaunchSpot2Segment1End)));

        Path bHiveTunnelStartPath = Paths.line(bLaunchSpot2, bHiveTunnelStart).heading(Interpolator.piecewise().until(1, Interpolator.linear(bHiveTunnelStartSegment1Start, bHiveTunnelStartSegment1End)));

        Path bHiveTunnelEndPath = Paths.line(bHiveTunnelStart, bHiveTunnelEnd).heading(Interpolator.piecewise().until(1, Interpolator.constant(bHiveTunnelEndSegment1Heading)));

        Path bFlowerPickupPath = Paths.line(bHiveTunnelEnd, bFlowerPickup).constant(bFlowerPickup);

        Path bLauchSpot3Path = Paths.line(bFlowerPickup, bLauchSpot3).heading(Interpolator.piecewise().until(1, Interpolator.linear(bLauchSpot3Segment1Start, bLauchSpot3Segment1End)));

        List<Path> pathsList = new ArrayList<>();
        pathsList.add(launchSpot1Path);
        pathsList.add(LAUNCH);
        pathsList.add(INTAKE_ON);
        pathsList.add(gardenFlowerPickupPath);
        pathsList.add(FLOWER);
        pathsList.add(INTAKE_OFF);
        pathsList.add(tipDetectSpotPath);
        pathsList.add(CHECK_TIPPED);     // look at the hive: has it tipped
        pathsList.add(IF_TIPPED);
        pathsList.add(launchSpot2Path);
        pathsList.add(LAUNCH);
        pathsList.add(INTAKE_ON);
        pathsList.add(homeFlowerPickupPath);
        pathsList.add(FLOWER);
        pathsList.add(INTAKE_OFF);
        pathsList.add(launchSpot3Path);
        pathsList.add(LAUNCH);
        pathsList.add(IF_NOT_TIPPED);
        pathsList.add(bLaunchSpot2Path);
        pathsList.add(LAUNCH);
        pathsList.add(bHiveTunnelStartPath);
        pathsList.add(bHiveTunnelEndPath);
        pathsList.add(bFlowerPickupPath);
        pathsList.add(FLOWER);
        pathsList.add(bLauchSpot3Path);
        pathsList.add(LAUNCH);
        pathsList.add(END_IF);
        pathsList.add(homePath);

        return pathsList;
    }
}
