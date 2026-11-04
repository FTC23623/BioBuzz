package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.pedropathing.math.Pose;
import java.util.List;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The FLOWER step's moves, without a robot (docs/decisions/0042): back 9, then forward 8 and
 * back 7 twice, all along the way the robot faces.
 */
@DisplayName("FLOWER step")
class FlowerStepTest {
    private static final double closeEnough = 1e-9;

    private static void assertPose(double x, double y, double headingRad, Pose actual) {
        assertEquals(x, actual.x(), closeEnough, "x in " + actual);
        assertEquals(y, actual.y(), closeEnough, "y in " + actual);
        assertEquals(headingRad, actual.heading(), closeEnough, "heading in " + actual);
    }

    @Test
    @DisplayName("facing forward from (0, 0): back to -9, then -1, -9, -1, -9")
    void facingForward() {
        List<Pose> moves = PedroAuto.FlowerMoves(new Pose(0, 0, 0), new BotProperties());
        assertEquals(5, moves.size());
        assertPose(-9, 0, 0, moves.get(0));
        assertPose(-1, 0, 0, moves.get(1));
        assertPose(-9, 0, 0, moves.get(2));
        assertPose(-1, 0, 0, moves.get(3));
        assertPose(-9, 0, 0, moves.get(4));
    }

    @Test
    @DisplayName("facing left (90 deg) from (10, 20): the same moves, along y")
    void facingLeft() {
        double left = Math.toRadians(90);
        List<Pose> moves = PedroAuto.FlowerMoves(new Pose(10, 20, left), new BotProperties());
        assertPose(10, 11, left, moves.get(0));
        assertPose(10, 19, left, moves.get(1));
        assertPose(10, 11, left, moves.get(4));
    }

    @Test
    @DisplayName("the passes setting sets how many forward-and-back pairs")
    void passes() {
        BotProperties p = new BotProperties();
        p.autoFlowerPasses = 1;
        assertEquals(3, PedroAuto.FlowerMoves(new Pose(0, 0, 0), p).size());
        p.autoFlowerPasses = 3;
        assertEquals(7, PedroAuto.FlowerMoves(new Pose(0, 0, 0), p).size());
    }

    @Test
    @DisplayName("FLOWER is its own marker, not LAUNCH")
    void ownMarker() {
        assertNotSame(PedroAuto.LAUNCH, PedroAuto.FLOWER);
        assertNotSame(PedroAuto.LAUNCH_WHEN_TAG_SEEN, PedroAuto.FLOWER);
    }

    @Test
    @DisplayName("the starting numbers are the coach's: back 9, settle 0.3 s, strokes of 8, a 1 s hold at the flower, 2 passes, half a second for the arm")
    void startingNumbers() {
        BotProperties p = new BotProperties();
        assertEquals(9, p.autoFlowerBackIn, closeEnough);
        assertEquals(0.3, p.autoFlowerSettleSeconds, closeEnough);
        assertEquals(8, p.autoFlowerStrokeIn, closeEnough);
        assertEquals(1, p.autoFlowerDwellSeconds, closeEnough);
        assertEquals(2, p.autoFlowerPasses);
        assertEquals(0.5, p.autoExtractorSeconds, closeEnough);
    }

    @Test
    @DisplayName("the order: back up, settle, arm out, then forward, hold, back twice, then arm in")
    void actionOrder() {
        List<PedroAuto.FlowerAction> actions = PedroAuto.FlowerActions(new Pose(0, 0, 0), new BotProperties());
        String[] expected = {"back up", "settle", "arm out",
                "stroke forward", "at the flower", "stroke back",
                "stroke forward", "at the flower", "stroke back", "arm in"};
        assertEquals(expected.length, actions.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actions.get(i).name, "action " + (i + 1));
        }
        // The arm goes out at the backed-up spot, and the hold at the flower is a full second.
        assertPose(-9, 0, 0, actions.get(2).spot);
        assertEquals(1, actions.get(4).seconds, closeEnough);
        assertPose(-1, 0, 0, actions.get(4).spot);
    }

    @Test
    @DisplayName("a hold of 0 at the flower is left out")
    void noDwell() {
        BotProperties p = new BotProperties();
        p.autoFlowerDwellSeconds = 0;
        assertEquals(8, PedroAuto.FlowerActions(new Pose(0, 0, 0), p).size());
    }
}
