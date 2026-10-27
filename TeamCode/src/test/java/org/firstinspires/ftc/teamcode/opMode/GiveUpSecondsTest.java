package org.firstinspires.ftc.teamcode.opMode;

import static com.pedropathing.api.Paths.line;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import java.util.List;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks how long an auto waits before giving up on a path, without a robot. The formula is
 * (drive time + turn time) / speed scale x margin + extra; see PedroAuto.GiveUpSeconds.
 * These tests use a top speed of 50 inches per second so the numbers come out round.
 */
@DisplayName("Give-up time for a path")
class GiveUpSecondsTest {
    private static final double closeEnough = 1e-6;
    private static final double topSpeed = 50;

    @Test
    @DisplayName("a straight 50-inch path: 1 s of driving, x 1.5, + 1 = 2.5 s")
    void straight() {
        Path path = line(new Pose(0, 0), new Pose(50, 0)).constant(0);
        assertEquals(2.5, PedroAuto.GiveUpSeconds(path, new BotProperties(), topSpeed), closeEnough);
    }

    @Test
    @DisplayName("at half speed the same path gets twice the time, plus the same extra: 4 s")
    void halfSpeed() {
        BotProperties properties = new BotProperties();
        properties.autoSpeedScale = 0.5;
        Path path = line(new Pose(0, 0), new Pose(50, 0)).constant(0);
        assertEquals(4, PedroAuto.GiveUpSeconds(path, properties, topSpeed), closeEnough);
    }

    @Test
    @DisplayName("a 90-degree turn adds one quarter turn: (1 + 1) x 1.5 + 1 = 4 s")
    void turning() {
        Path path = line(new Pose(0, 0), new Pose(50, 0)).heading(Interpolator.linear(0, Math.toRadians(90)));
        assertEquals(4, PedroAuto.GiveUpSeconds(path, new BotProperties(), topSpeed), closeEnough);
    }

    @Test
    @DisplayName("a turn from -90 to -180 counts as 90 degrees, not 270")
    void turnAcross180() {
        Path path = line(new Pose(0, 0), new Pose(50, 0))
                .heading(Interpolator.linear(Math.toRadians(-90), Math.toRadians(-180)));
        assertEquals(4, PedroAuto.GiveUpSeconds(path, new BotProperties(), topSpeed), closeEnough);
    }

    @Test
    @DisplayName("StrategyA: every path builds and gets more than the extra second")
    void strategyA() {
        List<Path> paths = new AutoStrategyA().BuildPaths();
        for (Path path : paths) {
            if (path != PedroAuto.LAUNCH) {
                assertTrue(PedroAuto.GiveUpSeconds(path, new BotProperties(), topSpeed) > 1);
            }
        }
    }
}
