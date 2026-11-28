package org.firstinspires.ftc.teamcode.types;

/**
 * What an auto's CHECK_TIPPED step saw: which of our hive's two cells has tipped up, if any
 * (docs/decisions/0057).
 *
 * <p>Each cell has its own four AprilTags, so the tag the camera sees says which cell it is.
 * {@code gardenSideTags} and {@code flowerSideTags} in BotProperties say which tags are on
 * which cell.
 */
public enum HiveUp {
    /** No tipped tag of ours was seen. Also the answer before any CHECK_TIPPED. */
    NOTHING,
    /** The cell on the garden side has tipped up. */
    GARDEN_SIDE,
    /** The cell on the flower side has tipped up. */
    FLOWER_SIDE,
    /** A tipped tag was seen, but it is on neither list, so the side is not known. */
    UNKNOWN_SIDE;

    /** true for any answer but NOTHING: some cell of ours has tipped up. */
    public boolean Tipped() {
        return this != NOTHING;
    }

    /**
     * Which side a tipped tag is on.
     *
     * <pre>
     *     gardenTags = {{30, 33}, {38, 41}}     each pair is a lowest and highest tag id
     *     tag 39: GARDEN_SIDE.   tag 44: not in there, so look in flowerTags.
     * </pre>
     *
     * @param tagId      the tipped tag the camera saw
     * @param gardenTags the garden side cells' tags, as {lowest id, highest id} pairs
     * @param flowerTags the flower side cells' tags, the same way
     */
    public static HiveUp FromTag(int tagId, int[][] gardenTags, int[][] flowerTags) {
        if (InAny(tagId, gardenTags)) {
            return GARDEN_SIDE;
        }
        if (InAny(tagId, flowerTags)) {
            return FLOWER_SIDE;
        }
        return UNKNOWN_SIDE;
    }

    /** The side across from this one. NOTHING and UNKNOWN_SIDE have none: they stay as they are. */
    public HiveUp OtherSide() {
        if (this == GARDEN_SIDE) {
            return FLOWER_SIDE;
        }
        if (this == FLOWER_SIDE) {
            return GARDEN_SIDE;
        }
        return this;
    }

    /**
     * Is an angle inside a window, from {@code minDeg} to {@code maxDeg}? An unknown angle
     * (NaN) never is.
     */
    public static boolean InWindow(double deg, double minDeg, double maxDeg) {
        if (Double.isNaN(deg)) {
            return false;
        }
        return deg >= minDeg && deg <= maxDeg;
    }

    /**
     * What one look at the nearest tag of ours says about the hive.
     *
     * <pre>
     *     the tag's cell is up:      that tag's side is up
     *     the tag's cell is down:    the OTHER side is up
     *     anything else, or a tag on neither list seen down:   NOTHING
     * </pre>
     *
     * @param up   the tag reads as up (its angle is in the UP window)
     * @param down the tag reads as down (in the DOWN window), and that counts
     *             (autoDownMeansOtherSideUp)
     */
    public static HiveUp FromLook(boolean up, boolean down, int tagId,
                                  int[][] gardenTags, int[][] flowerTags) {
        HiveUp tagSide = FromTag(tagId, gardenTags, flowerTags);
        if (up) {
            return tagSide;
        }
        if (down && tagSide != UNKNOWN_SIDE) {
            return tagSide.OtherSide();
        }
        return NOTHING;
    }

    private static boolean InAny(int tagId, int[][] ranges) {
        for (int[] range : ranges) {
            if (tagId >= range[0] && tagId <= range[1]) {
                return true;
            }
        }
        return false;
    }
}
