package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.pedropathing.math.Pose;
import java.util.List;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The FLOWER step's moves, without a robot (docs/decisions/0042): back 6, then forward 7 and
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
    @DisplayName("facing forward from (0, 0): back to -6, then 1, -6, 1, -6")
    void facingForward() {
        List<Pose> moves = PedroAuto.FlowerMoves(new Pose(0, 0, 0), new BotProperties());
        assertEquals(5, moves.size());
        assertPose(-6, 0, 0, moves.get(0));
        assertPose(1, 0, 0, moves.get(1));
        assertPose(-6, 0, 0, moves.get(2));
        assertPose(1, 0, 0, moves.get(3));
        assertPose(-6, 0, 0, moves.get(4));
    }

    @Test
    @DisplayName("facing left (90 deg) from (10, 20): the same moves, along y")
    void facingLeft() {
        double left = Math.toRadians(90);
        List<Pose> moves = PedroAuto.FlowerMoves(new Pose(10, 20, left), new BotProperties());
        assertPose(10, 14, left, moves.get(0));
        assertPose(10, 21, left, moves.get(1));
        assertPose(10, 14, left, moves.get(4));
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
    @DisplayName("the starting numbers are the coach's: back 6, strokes of 7, 2 passes, half a second for the arm")
    void startingNumbers() {
        BotProperties p = new BotProperties();
        assertEquals(6, p.autoFlowerBackIn, closeEnough);
        assertEquals(7, p.autoFlowerStrokeIn, closeEnough);
        assertEquals(2, p.autoFlowerPasses);
        assertEquals(0.5, p.autoExtractorSeconds, closeEnough);
    }
}
