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
            if (id >= lowest && id <= highest) {
                total = total + bearings.get(i);
                count = count + 1;
            }
        }
        if (count == 0) {
            return 0;
        }
        return total / count;
    }
}
