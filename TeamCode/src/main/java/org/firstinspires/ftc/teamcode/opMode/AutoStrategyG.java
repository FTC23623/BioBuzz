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
 * A garden side auto drawn in the Pedro Pathing Visualizer (visualizer.pedropathing.com). Its
 * spots are FIELD spots, copied straight from the visualizer, not measured from the start.
 * See {@link FieldAuto}.
 *
 * <p>To build the route:
 * <ol>
 *   <li>Draw it in the visualizer on the BIOBUZZ field, for BLUE.
 *   <li>Copy the visualizer's start point into {@link #START}. This is the one number the
 *       auto can't work out by itself.
 *   <li>Copy its poses and paths into {@link #BuildPaths()}, and add the stops between
 *       them: LAUNCH, FLOWER, INTAKE_ON, INTAKE_OFF, CHECK_TIPPED and the IF steps.
 * </ol>
 *
 * <p>Right now it only backs 12 inches off the wall, so there is something to run.
 */
// Group "Auto" (it reads the alliance magnet), like the others. At the end it picks HyDrive.
@Autonomous(name = "Auto: " + AutoStrategyG.NAME, group = "Auto", preselectTeleOp = HyDrive.NAME)
public class AutoStrategyG extends FieldAuto {
    /** The auto's name. On the Driver Station: "Auto: ", "Blue: " or "Red: " in front. */
    public static final String NAME = "Garden Side G";

    /**
     * PROVIDE THIS: where the robot starts, from the visualizer (blue): x, y in inches,
     * heading in degrees. This one is the garden side start the other autos use: front against
     * the bottom wall, center 9 in off it.
     */
    static final Pose START = PoseFactory.degrees().of(53, 9, 270);

    @Override
    protected Pose FieldStart() {
        return START;
    }

    /**
     * The robot's front (the intake) points to the driver's RIGHT (-90), at the garden side, like
     * every garden side auto. Seen from the driver, NOT the field heading in START. Change it
     * only if START faces a different way.
     */
    @Override
    protected double StartFacingDeg() {
        return -90;
    }

    @Override
    protected List<Path> BuildPaths() {
        PoseFactory poseFactory = PoseFactory.degrees();

        // Field spots from the visualizer: x, y in inches, heading in degrees.
        Pose launchSpot1 = poseFactory.of(41, 12, 250);
        Pose launchSpot1Segment1Start = poseFactory.of(41, 12, 270);
        Pose launchSpot1Segment1End = poseFactory.of(41, 12, 250);
        Pose gardenFlowerPickup = poseFactory.of(13, 46.8224, 180);
        Pose gardenFlowerPickupSegment1Start = poseFactory.of(13, 46.8224, 270);
        Pose gardenFlowerPickupSegment1End = poseFactory.of(13, 46.8224, 180);
        Pose gardenFlowerPickupSegment2Start = poseFactory.of(13, 46.8224, 180);
        Pose gardenFlowerPickupSegment2End = poseFactory.of(13, 46.8224, 180);
        Pose tipDetectSpot = poseFactory.of(22.0341, 46.9085, 180);
        Pose launchSpot2 = poseFactory.of(43.7238, 127, 100);
        Pose launchSpot2Segment1Start = poseFactory.of(43.7238, 127, 180);
        Pose launchSpot2Segment1End = poseFactory.of(43.7238, 127, 65);
        Pose launchSpot2Segment2Start = poseFactory.of(43.7238, 127, 65);
        Pose launchSpot2Segment2End = poseFactory.of(43.7238, 127, 100);
        Pose homeFlowerPickup = poseFactory.of(46.4065, 129, 90);
        Pose homeFlowerPickupSegment1Start = poseFactory.of(46.4065, 129, 95);
        Pose homeFlowerPickupSegment1End = poseFactory.of(46.4065, 129, 90);
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
        Pose bFlowerPickup = poseFactory.of(46.8224, 130, 270);
        Pose bLauchSpot3 = poseFactory.of(33.3954, 119.2938, 300);
        Pose bLauchSpot3Segment1Start = poseFactory.of(33.3954, 119.2938, 270);
        Pose bLauchSpot3Segment1End = poseFactory.of(33.3954, 119.2938, 300);

        Path launchSpot1Path = line(START, launchSpot1).heading(Interpolator.piecewise().until(1, Interpolator.linear(launchSpot1Segment1Start, launchSpot1Segment1End)));
        Path gardenFlowerPickupPath = line(launchSpot1, gardenFlowerPickup).heading(Interpolator.piecewise().until(0.5, Interpolator.linear(gardenFlowerPickupSegment1Start, gardenFlowerPickupSegment1End)).until(1, Interpolator.linear(gardenFlowerPickupSegment2Start, gardenFlowerPickupSegment2End)));
        Path tipDetectSpotPath = line(gardenFlowerPickup, tipDetectSpot).constant(tipDetectSpot);
        Path launchSpot2Path = line(tipDetectSpot, launchSpot2).heading(Interpolator.piecewise().until(0.25, Interpolator.linear(launchSpot2Segment1Start, launchSpot2Segment1End)).until(1, Interpolator.linear(launchSpot2Segment2Start, launchSpot2Segment2End)));
        Path homeFlowerPickupPath = line(launchSpot2, homeFlowerPickup).heading(Interpolator.piecewise().until(1, Interpolator.linear(homeFlowerPickupSegment1Start, homeFlowerPickupSegment1End)));
        Path launchSpot3Path = line(homeFlowerPickup, launchSpot3).heading(Interpolator.piecewise().until(1, Interpolator.linear(launchSpot3Segment1Start, launchSpot3Segment1End)));
        Path homePath = line(launchSpot3, home).constant(home);


        Path bLaunchSpot2Path = line(tipDetectSpot, bLaunchSpot2).heading(Interpolator.piecewise().until(1, Interpolator.linear(bLaunchSpot2Segment1Start, bLaunchSpot2Segment1End)));

        Path bHiveTunnelStartPath = line(bLaunchSpot2, bHiveTunnelStart).heading(Interpolator.piecewise().until(1, Interpolator.linear(bHiveTunnelStartSegment1Start, bHiveTunnelStartSegment1End)));

        Path bHiveTunnelEndPath = line(bHiveTunnelStart, bHiveTunnelEnd).heading(Interpolator.piecewise().until(1, Interpolator.constant(bHiveTunnelEndSegment1Heading)));

        Path bFlowerPickupPath = line(bHiveTunnelEnd, homeFlowerPickup).heading(Interpolator.piecewise().until(1, Interpolator.linear(bHiveTunnelEnd, homeFlowerPickup)));

        Path bLauchSpot3Path = line(homeFlowerPickup, launchSpot3).heading(Interpolator.piecewise().until(1, Interpolator.linear(homeFlowerPickup, launchSpot3)));
//        Path bHomePath = line(bLauchSpot3, home).heading(Interpolator.piecewise().until(1, Interpolator.linear(launchSpot3, home)));

        List<Path> pathsList = new ArrayList<>();
        pathsList.add(launchSpot1Path);
        pathsList.add(LAUNCH);
        pathsList.add(INTAKE_ON);
        pathsList.add(gardenFlowerPickupPath);
        pathsList.add(FLOWER);
        pathsList.add(INTAKE_OFF);
        pathsList.add(tipDetectSpotPath);
        pathsList.add(CHECK_TIPPED);     // look at the hive: has it tipped
        pathsList.add(IF_FLOWER_SIDE_UP);
        pathsList.add(launchSpot2Path);
        pathsList.add(LAUNCH);
        pathsList.add(INTAKE_ON);
        pathsList.add(homeFlowerPickupPath);
        pathsList.add(FLOWER);
        pathsList.add(INTAKE_OFF);
        pathsList.add(launchSpot3Path);
        pathsList.add(LAUNCH);
        pathsList.add(IF_GARDEN_SIDE_UP);
        pathsList.add(bLaunchSpot2Path);
        pathsList.add(LAUNCH);
        pathsList.add(bHiveTunnelStartPath);
        pathsList.add(bHiveTunnelEndPath);
        pathsList.add(INTAKE_ON);
        pathsList.add(bFlowerPickupPath);
        pathsList.add(FLOWER);
        pathsList.add(bLauchSpot3Path);
        pathsList.add(INTAKE_OFF);
        pathsList.add(LAUNCH);
        pathsList.add(END_IF);
        pathsList.add(homePath);

        return pathsList;
    }
}
