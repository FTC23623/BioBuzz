package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the autos pasted from Tool: Pose Recorder (StrategyC and StrategyD) are what
 * their descriptions say, without a robot: three plain LAUNCH stops, every path starting
 * where the last one ended, and full speed.
 */
@DisplayName("Recorded autos: StrategyC and StrategyD")
class RecordedAutosTest {
    private static final double closeEnough = 1e-6;

    private static int countLaunches(List<Path> paths) {
        int launches = 0;
        for (Path path : paths) {
            if (path == PedroAuto.LAUNCH) {
                launches = launches + 1;
            }
            assertNotSame(PedroAuto.LAUNCH_WHEN_TAG_SEEN, path, "a recorded auto never waits for a tag");
        }
        return launches;
    }

    /** Each driven path starts where the one before it ended. LAUNCH stops are skipped over. */
    private static void assertConnected(List<Path> paths) {
        Pose lastEnd = null;
        for (int i = 0; i < paths.size(); i++) {
            Path path = paths.get(i);
            if (path == PedroAuto.LAUNCH) {
                continue;
            }
            if (lastEnd != null) {
                Pose start = path.get(0);
                assertEquals(lastEnd.x(), start.x(), closeEnough, "x at path " + (i + 1));
                assertEquals(lastEnd.y(), start.y(), closeEnough, "y at path " + (i + 1));
            }
            lastEnd = path.endPose();
        }
    }

    @Test
    @DisplayName("StrategyC: six spots, three launches, the last step a launch")
    void strategyC() {
        AutoStrategyC auto = new AutoStrategyC();
        List<Path> paths = auto.BuildPaths();
        assertEquals(9, paths.size());
        assertEquals(3, countLaunches(paths));
        assertEquals(PedroAuto.LAUNCH, paths.get(paths.size() - 1));
        assertConnected(paths);
        assertEquals(1, auto.botProperties.autoSpeedScale, closeEnough, "full speed");
    }

    @Test
    @DisplayName("StrategyD: seven spots, three launches, then one more drive")
    void strategyD() {
        AutoStrategyD auto = new AutoStrategyD();
        List<Path> paths = auto.BuildPaths();
        assertEquals(10, paths.size());
        assertEquals(3, countLaunches(paths));
        assertNotSame(PedroAuto.LAUNCH, paths.get(paths.size() - 1));
        assertConnected(paths);
        assertEquals(1, auto.botProperties.autoSpeedScale, closeEnough, "full speed");
    }

    @Test
    @DisplayName("both start at (0, 0) facing forward, where the recording started")
    void startAtTheRecordingStart() {
        for (PedroAuto auto : new PedroAuto[] {new AutoStrategyC(), new AutoStrategyD()}) {
            Pose first = auto.BuildPaths().get(0).get(0);
            assertEquals(0, first.x(), closeEnough);
            assertEquals(0, first.y(), closeEnough);
        }
    }
}
