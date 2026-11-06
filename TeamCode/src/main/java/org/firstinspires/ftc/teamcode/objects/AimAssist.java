package org.firstinspires.ftc.teamcode.objects;

import java.util.List;

/**
 * The turn that points the robot at something it can see.
 *
 * <p>The further off center the target is, the harder the robot turns, up to a limit.
 * Close enough to center, it stops turning, so it does not wobble back and forth. No
 * hardware here, so it can be tested on a laptop.
 */
public final class AimAssist {
    private AimAssist() {
    }

    /**
     * @param bearingDegrees degrees from straight ahead to the target. Left is positive.
     * @param kP             turn power per degree
     * @param maxRotate      the turn never goes past this, either way
     * @param doneDegrees    within this many degrees of center, stop turning
     * @return the rotate power for the drive. Positive turns left. A broken number (NaN)
     *     turns nothing.
     */
    public static double RotateToward(double bearingDegrees, double kP, double maxRotate,
                                      double doneDegrees) {
        if (Double.isNaN(bearingDegrees)) {
            return 0;
        }
        if (Math.abs(bearingDegrees) <= doneDegrees) {
            return 0;
        }
        double rotate = kP * bearingDegrees;
        return Math.max(-maxRotate, Math.min(rotate, maxRotate));
    }

    /**
     * How far the robot still has to turn, in degrees, left positive: the hive's bearing in
     * the camera minus where it should sit once aimed ({@code aimHiveBearingDeg}). 0 = aimed.
     *
     * <pre>
     *     hive 5 deg left, should sit 1 deg left: 4 deg still to turn, left
     *     hive dead ahead, should sit 1 deg left: -1, turn 1 deg right
     * </pre>
     */
    public static double AimError(double hiveBearingDeg, double aimHiveBearingDeg) {
        return hiveBearingDeg - aimHiveBearingDeg;
    }

    /**
     * Where the hive should sit in the camera once aimed, by which side of the hive the robot
     * stands (docs/decisions/0044). Feed it to {@link #AimError}.
     *
     * <pre>
     *     offset 3, robot 20 deg to the LEFT of the hive:   3   hive sits left, robot turns 3 more right
     *     offset 3, robot 20 deg to the RIGHT of the hive: -3   hive sits right, robot turns 3 more left
     *     offset 3, robot 2 deg off (inside center 5):      0   aim at the middle
     * </pre>
     *
     * @param sideDeg   which side of the hive the robot stands, left positive. 0 = not known.
     * @param offsetDeg how far to aim off the middle ({@code aimHiveBearingDeg})
     * @param centerDeg within this of straight in front, no offset ({@code aimSideCenterDeg})
     */
    public static double SideOffset(double sideDeg, double offsetDeg, double centerDeg) {
        if (sideDeg > centerDeg) {
            return offsetDeg;
        }
        if (sideDeg < -centerDeg) {
            return -offsetDeg;
        }
        return 0;
    }

    /**
     * The drive that keeps the robot the same distance from the tag while it arcs around
     * it. Strafing sideways on its own slowly carries the robot away from the tag; this
     * pulls it back in.
     *
     * @param rangeIn  inches to the tag right now. 0 when the camera can't tell.
     * @param wantedIn inches to hold, measured the moment the arc started. 0 = not known.
     * @param kP       drive power per inch off
     * @param maxDrive the drive never goes past this, either way
     * @return the drive for the robot. Negative is forward (too far away: close in),
     *     positive is backward (too near: back off). 0 when either distance is unknown.
     */
    public static double HoldRange(double rangeIn, double wantedIn, double kP, double maxDrive) {
        if (rangeIn <= 0 || wantedIn <= 0) {
            return 0;
        }
        double tooFarIn = rangeIn - wantedIn;
        // Forward is negative drive, so being too far away means a negative number.
        double drive = -kP * tooFarIn;
        return Math.max(-maxDrive, Math.min(drive, maxDrive));
    }

    /**
     * true when a tag id is one we care about.
     *
     * @param id    the tag's id
     * @param range the lowest and highest id we care about, like {@code {30, 37}}
     */
    public static boolean InRange(int id, int[] range) {
        return id >= range[0] && id <= range[1];
    }

    /** "tag 24, tag 20" from a list of ids, for the Driver Station. An empty list is "none". */
    public static String TagList(List<Integer> ids) {
        if (ids.isEmpty()) {
            return "none";
        }
        StringBuilder out = new StringBuilder();
        for (int id : ids) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append("tag ").append(id);
        }
        return out.toString();
    }

    /**
     * Where to aim when tags come in groups, like four tags on one target. Finds the group
     * the nearest tag is in, then averages the bearings of every tag in view from that same
     * group. That points at the middle of the group, not at one corner of it.
     *
     * <pre>
     *     groups = {{30, 33}, {34, 37}}     each pair is a lowest and highest tag id
     *     nearest tag 31, also see 30 and 33, all from group 30-33
     *     aim = average of the bearings of 30, 31 and 33
     * </pre>
     *
     * @param ids       the id of every tag in view
     * @param bearings  the bearing of every tag in view, in the same order. Left is positive.
     * @param nearestId the id of the nearest tag
     * @param groups    each group's lowest and highest tag id
     * @return degrees to the middle of the group. If the nearest tag is in no group, its own
     *     bearing. 0 when it is not in the list at all.
     */
    public static double GroupCenterBearing(List<Integer> ids, List<Double> bearings,
                                            int nearestId, int[][] groups) {
        return GroupAverage(ids, bearings, nearestId, groups, false);
    }

    /**
     * The average of one number per tag (a bearing, or a distance) over every tag in view
     * from the nearest tag's group, the same tags {@link #GroupCenterBearing} uses.
     *
     * <pre>
     *     tags 30, 31, 33 (one group) at 100, 104 and 0 in (0 = no distance), skipZeros:
     *     (100 + 104) / 2 = 102
     * </pre>
     *
     * @param skipZeros true: a value of 0 means "not known" and is left out (distances).
     * @return the average, or 0 when no tag counts
     */
    public static double GroupAverage(List<Integer> ids, List<Double> values, int nearestId,
                                      int[][] groups, boolean skipZeros) {
        // Start with the group that is just the nearest tag, in case it is in no group.
        int lowest = nearestId;
        int highest = nearestId;
        for (int[] group : groups) {
            if (nearestId >= group[0] && nearestId <= group[1]) {
                lowest = group[0];
                highest = group[1];
            }
        }
        double total = 0;
        int count = 0;
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            double value = values.get(i);
            boolean unknown = skipZeros && value == 0;
            if (id >= lowest && id <= highest && !unknown) {
                total = total + value;
                count = count + 1;
            }
        }
        if (count == 0) {
            return 0;
        }
        return total / count;
    }
}
