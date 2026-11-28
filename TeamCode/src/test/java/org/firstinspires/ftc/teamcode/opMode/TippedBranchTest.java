package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.types.HiveUp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which steps an auto runs around IF_TIPPED / IF_NOT_TIPPED / END_IF, without a robot
 * (docs/decisions/0057). Each test walks the list with {@link PedroAuto#NextStep} and checks
 * the step numbers it lands on.
 */
@DisplayName("Tipped / not tipped steps")
class TippedBranchTest {
    /** A pretend drive, so each one is a different Path. */
    private static Path drive(double x) {
        return line(new Pose(x, 0), new Pose(x + 1, 0)).constant(0);
    }

    /** Every step that runs, in order, from the start. */
    private static List<Integer> walk(List<Path> paths, boolean tipped) {
        return walk(paths, tipped ? HiveUp.UNKNOWN_SIDE : HiveUp.NOTHING);
    }

    /** Every step that runs, in order, from the start, for what CHECK_TIPPED saw. */
    private static List<Integer> walk(List<Path> paths, HiveUp tipped) {
        List<Integer> ran = new ArrayList<>();
        int step = PedroAuto.NextStep(paths, -1, tipped);
        while (step < paths.size()) {
            ran.add(step);
            step = PedroAuto.NextStep(paths, step, tipped);
        }
        return ran;
    }

    /** The example in PedroAuto's IF_TIPPED javadoc. */
    private static List<Path> example() {
        return Arrays.asList(
                drive(0),                    // 0 toLaunchSpot
                PedroAuto.LAUNCH,            // 1
                PedroAuto.CHECK_TIPPED,      // 2
                PedroAuto.IF_TIPPED,         // 3
                drive(10),                   // 4 toFlower
                PedroAuto.FLOWER,            // 5
                drive(20),                   // 6 backFromFlower
                PedroAuto.IF_NOT_TIPPED,     // 7
                PedroAuto.LAUNCH,            // 8
                PedroAuto.END_IF,            // 9
                drive(30));                  // 10 toLoadingZone
    }

    @Test
    @DisplayName("tipped: the tipped steps run, the not-tipped ones are skipped, the end runs")
    void tipped() {
        assertEquals(Arrays.asList(0, 1, 2, 4, 5, 6, 10), walk(example(), true));
    }

    @Test
    @DisplayName("not tipped: the tipped steps are skipped, the not-tipped ones run, the end runs")
    void notTipped() {
        assertEquals(Arrays.asList(0, 1, 2, 8, 10), walk(example(), false));
    }

    @Test
    @DisplayName("IF_TIPPED ... END_IF with no other side: not tipped skips straight to after END_IF")
    void tippedSideOnly() {
        List<Path> paths = Arrays.asList(
                drive(0), PedroAuto.IF_TIPPED, drive(10), PedroAuto.END_IF, drive(20));
        assertEquals(Arrays.asList(0, 2, 4), walk(paths, true));
        assertEquals(Arrays.asList(0, 4), walk(paths, false));
    }

    @Test
    @DisplayName("IF_NOT_TIPPED ... END_IF alone: runs only when not tipped")
    void notTippedSideOnly() {
        List<Path> paths = Arrays.asList(
                drive(0), PedroAuto.IF_NOT_TIPPED, drive(10), PedroAuto.END_IF, drive(20));
        assertEquals(Arrays.asList(0, 4), walk(paths, true));
        assertEquals(Arrays.asList(0, 2, 4), walk(paths, false));
    }

    @Test
    @DisplayName("an IF at the very top is skipped like any other; one at the very end leaves nothing")
    void atTheEnds() {
        List<Path> paths = Arrays.asList(
                PedroAuto.IF_TIPPED, drive(0), PedroAuto.END_IF, drive(10), PedroAuto.IF_TIPPED);
        assertEquals(Arrays.asList(1, 3), walk(paths, true));
        assertEquals(Arrays.asList(3), walk(paths, false));
    }

    @Test
    @DisplayName("an END_IF left out: the skipped side runs to the end of the list")
    void missingEndIf() {
        List<Path> paths = Arrays.asList(drive(0), PedroAuto.IF_TIPPED, drive(10), drive(20));
        assertEquals(Arrays.asList(0), walk(paths, false));
    }

    /** The side example in PedroAuto's IF_TIPPED javadoc, with a path after the END_IF. */
    private static List<Path> sides() {
        return Arrays.asList(
                PedroAuto.CHECK_TIPPED,        // 0
                PedroAuto.IF_GARDEN_SIDE_UP,   // 1
                drive(0),                      // 2 toGardenLaunchSpot
                PedroAuto.IF_FLOWER_SIDE_UP,   // 3
                drive(10),                     // 4 toFlowerLaunchSpot
                PedroAuto.IF_NOT_TIPPED,       // 5
                drive(20),                     // 6 toWaitingSpot
                PedroAuto.END_IF,              // 7
                drive(30));                    // 8
    }

    @Test
    @DisplayName("garden side up, flower side up, nothing up: each runs only its own steps, then the end")
    void bySide() {
        assertEquals(Arrays.asList(0, 2, 8), walk(sides(), HiveUp.GARDEN_SIDE));
        assertEquals(Arrays.asList(0, 4, 8), walk(sides(), HiveUp.FLOWER_SIDE));
        assertEquals(Arrays.asList(0, 6, 8), walk(sides(), HiveUp.NOTHING));
    }

    @Test
    @DisplayName("tipped but the side is not known: neither side's steps run; IF_TIPPED still does")
    void unknownSide() {
        assertEquals(Arrays.asList(0, 8), walk(sides(), HiveUp.UNKNOWN_SIDE));
        assertEquals(Arrays.asList(0, 1, 2, 4, 5, 6, 10), walk(example(), HiveUp.UNKNOWN_SIDE));
    }

    @Test
    @DisplayName("IF_TIPPED is true for either side")
    void tippedEitherSide() {
        assertEquals(Arrays.asList(0, 1, 2, 4, 5, 6, 10), walk(example(), HiveUp.GARDEN_SIDE));
        assertEquals(Arrays.asList(0, 1, 2, 4, 5, 6, 10), walk(example(), HiveUp.FLOWER_SIDE));
    }

    @Test
    @DisplayName("a tag says which side by the lists in BotProperties: blue garden 42-45, red garden 34-37 (coach, 2026-11-25)")
    void sideFromTag() {
        BotProperties p = new BotProperties();
        int[][] garden = p.gardenSideTags;
        int[][] flower = p.flowerSideTags;
        assertEquals(HiveUp.GARDEN_SIDE, HiveUp.FromTag(42, garden, flower));
        assertEquals(HiveUp.GARDEN_SIDE, HiveUp.FromTag(45, garden, flower));
        assertEquals(HiveUp.GARDEN_SIDE, HiveUp.FromTag(34, garden, flower));
        assertEquals(HiveUp.GARDEN_SIDE, HiveUp.FromTag(37, garden, flower));
        assertEquals(HiveUp.FLOWER_SIDE, HiveUp.FromTag(38, garden, flower));
        assertEquals(HiveUp.FLOWER_SIDE, HiveUp.FromTag(41, garden, flower));
        assertEquals(HiveUp.FLOWER_SIDE, HiveUp.FromTag(30, garden, flower));
        assertEquals(HiveUp.FLOWER_SIDE, HiveUp.FromTag(33, garden, flower));
        assertEquals(HiveUp.UNKNOWN_SIDE, HiveUp.FromTag(20, garden, flower));
        assertFalse(HiveUp.NOTHING.Tipped());
        assertTrue(HiveUp.UNKNOWN_SIDE.Tipped());
    }

    @Test
    @DisplayName("one look: an up tag is its own side; a down tag means the other side is up")
    void fromLook() {
        BotProperties p = new BotProperties();
        int[][] garden = p.gardenSideTags;
        int[][] flower = p.flowerSideTags;
        // Blue: 43 is a garden side tag, 39 a flower side tag.
        // Tipped: the tag's own side.
        assertEquals(HiveUp.GARDEN_SIDE, HiveUp.FromLook(true, false, 43, garden, flower));
        assertEquals(HiveUp.FLOWER_SIDE, HiveUp.FromLook(true, false, 39, garden, flower));
        // Down: the garden cell is down, so the flower side is up, and the other way.
        assertEquals(HiveUp.FLOWER_SIDE, HiveUp.FromLook(false, true, 43, garden, flower));
        assertEquals(HiveUp.GARDEN_SIDE, HiveUp.FromLook(false, true, 39, garden, flower));
        // Neither up nor down (the buffer): nothing known.
        assertEquals(HiveUp.NOTHING, HiveUp.FromLook(false, false, 43, garden, flower));
        // A down tag on neither list says nothing; an up one is "side not known".
        assertEquals(HiveUp.NOTHING, HiveUp.FromLook(false, true, 20, garden, flower));
        assertEquals(HiveUp.UNKNOWN_SIDE, HiveUp.FromLook(true, false, 20, garden, flower));
    }

    @Test
    @DisplayName("the measured rolls (up -34, down 25) sit in their windows, with a buffer between")
    void windows() {
        BotProperties p = new BotProperties();
        assertTrue(p.autoTipFromRoll);
        assertTrue(p.autoDownMeansOtherSideUp);
        // The readings, and 15 degrees of wobble either way, still answer.
        for (double wobble = -15; wobble <= 15; wobble += 5) {
            assertTrue(HiveUp.InWindow(-34 + wobble, p.autoTagUpMinDeg, p.autoTagUpMaxDeg));
            assertTrue(HiveUp.InWindow(25 + wobble, p.autoTagDownMinDeg, p.autoTagDownMaxDeg));
            assertFalse(HiveUp.InWindow(-34 + wobble, p.autoTagDownMinDeg, p.autoTagDownMaxDeg));
            assertFalse(HiveUp.InWindow(25 + wobble, p.autoTagUpMinDeg, p.autoTagUpMaxDeg));
        }
        // The buffer: at least 15 degrees between the windows, where nothing is answered.
        assertTrue(p.autoTagDownMinDeg - p.autoTagUpMaxDeg >= 15);
        double middle = (p.autoTagUpMaxDeg + p.autoTagDownMinDeg) / 2;
        assertFalse(HiveUp.InWindow(middle, p.autoTagUpMinDeg, p.autoTagUpMaxDeg));
        assertFalse(HiveUp.InWindow(middle, p.autoTagDownMinDeg, p.autoTagDownMaxDeg));
        // An unknown angle is in no window.
        assertFalse(HiveUp.InWindow(Double.NaN, p.autoTagUpMinDeg, p.autoTagUpMaxDeg));
    }

    @Test
    @DisplayName("IF steps are markers; CHECK_TIPPED is a marker but not an IF")
    void markers() {
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.IF_TIPPED));
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.IF_NOT_TIPPED));
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.END_IF));
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.IF_GARDEN_SIDE_UP));
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.IF_FLOWER_SIDE_UP));
        assertFalse(PedroAuto.IsBranchMarker(PedroAuto.CHECK_TIPPED));
        assertTrue(PedroAuto.IsMarker(PedroAuto.CHECK_TIPPED));
        assertFalse(PedroAuto.IsMarker(drive(0)));
    }
}
