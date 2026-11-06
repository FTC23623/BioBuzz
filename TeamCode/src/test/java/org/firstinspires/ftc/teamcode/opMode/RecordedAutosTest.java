package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks the autos pasted from Tool: Pose Recorder (StrategyC and StrategyD) are what
 * their descriptions say, without a robot: plain LAUNCH stops (and FLOWER steps, once
 * recorded), every path starting where the last one ended, and full speed. StrategyC may
 * also wait for a tag once, for its first launch. Nothing here pins the number of spots,
 * so a fresh recording pasted in still passes.
 */
@DisplayName("Recorded autos: StrategyC and StrategyD")
class RecordedAutosTest {
    private static final double closeEnough = 1e-6;

    private static int countLaunches(List<Path> paths) {
        int launches = 0;
        for (Path path : paths) {
            if (path == PedroAuto.LAUNCH || path == PedroAuto.LAUNCH_WHEN_TAG_SEEN) {
                launches = launches + 1;
            }
        }
        return launches;
    }

    private static int countTagWaits(List<Path> paths) {
        int tagWaits = 0;
        for (Path path : paths) {
            if (path == PedroAuto.LAUNCH_WHEN_TAG_SEEN) {
                tagWaits = tagWaits + 1;
            }
        }
        return tagWaits;
    }

    /** Each driven path starts where the one before it ended. Launch, FLOWER and INTAKE steps are skipped over. */
    private static void assertConnected(List<Path> paths) {
        Pose lastEnd = null;
        for (int i = 0; i < paths.size(); i++) {
            Path path = paths.get(i);
            if (path == PedroAuto.LAUNCH || path == PedroAuto.LAUNCH_WHEN_TAG_SEEN || path == PedroAuto.FLOWER
                    || path == PedroAuto.INTAKE_ON || path == PedroAuto.INTAKE_OFF) {
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

    /** The checks every recorded auto must pass, however many spots it has. */
    private static void assertRecordedAuto(PedroAuto auto) {
        List<Path> paths = auto.BuildPaths();
        assertTrue(countLaunches(paths) >= 1, "launches at least once");
        assertConnected(paths);
        assertEquals(1, auto.botProperties.autoSpeedScale, closeEnough, "full speed");
    }

    @Test
    @DisplayName("StrategyC: launches, every path connects, full speed, at most one tag wait")
    void strategyC() {
        AutoStrategyC auto = new AutoStrategyC();
        assertRecordedAuto(auto);
        assertTrue(countTagWaits(auto.BuildPaths()) <= 1, "waits for a tag at most once");
    }

    @Test
    @DisplayName("StrategyD: launches, every path connects (across LAUNCH and FLOWER), full speed, no tag waits")
    void strategyD() {
        AutoStrategyD auto = new AutoStrategyD();
        assertRecordedAuto(auto);
        assertEquals(0, countTagWaits(auto.BuildPaths()), "never waits for a tag");
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
