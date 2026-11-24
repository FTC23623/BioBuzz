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

    @Test
    @DisplayName("IF steps are markers; CHECK_TIPPED is a marker but not an IF")
    void markers() {
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.IF_TIPPED));
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.IF_NOT_TIPPED));
        assertTrue(PedroAuto.IsBranchMarker(PedroAuto.END_IF));
        assertFalse(PedroAuto.IsBranchMarker(PedroAuto.CHECK_TIPPED));
        assertTrue(PedroAuto.IsMarker(PedroAuto.CHECK_TIPPED));
        assertFalse(PedroAuto.IsMarker(drive(0)));
    }
}
