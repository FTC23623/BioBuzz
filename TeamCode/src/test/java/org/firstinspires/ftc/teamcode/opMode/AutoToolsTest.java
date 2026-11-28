package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.pedropathing.api.Paths.line;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.List;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.types.TelemetryGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The auto speed-up steps (docs/decisions/0058): FLOWER_OUT / FLOWER_IN, LAUNCH_NECTAR, LAUNCH_UNTIL_EMPTY, step text, telemetry groups. */
@DisplayName("Auto tools")
class AutoToolsTest {
    private final BotProperties p = new BotProperties();

    @Test
    @DisplayName("every new step is a marker, and the launches are launches")
    void markers() {
        assertTrue(PedroAuto.IsMarker(PedroAuto.FLOWER_OUT));
        assertTrue(PedroAuto.IsMarker(PedroAuto.FLOWER_IN));
        assertTrue(PedroAuto.IsMarker(PedroAuto.LAUNCH_NECTAR));
        assertTrue(PedroAuto.IsMarker(PedroAuto.LAUNCH_UNTIL_EMPTY));
        assertTrue(PedroAuto.IsLaunch(PedroAuto.LAUNCH_NECTAR));
        assertTrue(PedroAuto.IsLaunch(PedroAuto.LAUNCH_UNTIL_EMPTY));
        assertTrue(PedroAuto.IsPlainLaunch(PedroAuto.LAUNCH_NECTAR));
        assertFalse(PedroAuto.IsPlainLaunch(PedroAuto.LAUNCH_WHEN_TAG_SEEN));
        assertFalse(PedroAuto.IsLaunch(PedroAuto.FLOWER_OUT));
    }

    @Test
    @DisplayName("LAUNCH and LAUNCH_NECTAR end after autoLaunchSeconds, whatever the switches say")
    void plainLaunchTimes() {
        assertFalse(PedroAuto.LaunchDone(PedroAuto.LAUNCH_NECTAR, 1.9, 99, true, p));
        assertTrue(PedroAuto.LaunchDone(PedroAuto.LAUNCH_NECTAR, 2.0, 0, true, p));
        assertTrue(PedroAuto.LaunchDone(PedroAuto.LAUNCH, 2.0, 0, true, p));
    }

    @Test
    @DisplayName("LAUNCH_UNTIL_EMPTY ends once empty long enough, after the gates are open, or at the max")
    void untilEmpty() {
        // Balls still on a switch (empty for 0 s): keeps going, up to the max.
        assertFalse(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, 3.0, 0, true, p));
        assertTrue(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, p.autoLaunchUntilEmptyMaxSeconds, 0, true, p));
        // Empty, but not for long enough.
        assertFalse(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, 1.0, p.autoEmptyClearSeconds - 0.01, true, p));
        assertTrue(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, 1.0, p.autoEmptyClearSeconds, true, p));
        // Empty from the start, but the gates have not swung open yet.
        assertFalse(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, 0.05, 5, true, p));
    }

    @Test
    @DisplayName("LAUNCH_UNTIL_EMPTY with no ball switch found runs autoLaunchSeconds, like LAUNCH")
    void untilEmptyNoSensors() {
        assertFalse(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, 1.9, 99, false, p));
        assertTrue(PedroAuto.LaunchDone(PedroAuto.LAUNCH_UNTIL_EMPTY, 2.0, 0, false, p));
    }

    @Test
    @DisplayName("the nectar launch speed floor sits below the nectar near shot")
    void nectarFloor() {
        assertTrue(p.autoNectarLaunchMinRpm <= p.nectarNearRpm);
    }

    @Test
    @DisplayName("FLOWER after FLOWER_OUT: the arm-out action takes no time; otherwise autoExtractorSeconds")
    void armAlreadyOut() {
        List<PedroAuto.FlowerAction> cold = PedroAuto.FlowerActions(new Pose(0, 0, 0), p, false);
        List<PedroAuto.FlowerAction> warm = PedroAuto.FlowerActions(new Pose(0, 0, 0), p, true);
        assertEquals(cold.size(), warm.size());
        assertEquals(p.autoExtractorSeconds, cold.get(2).seconds, 1e-9);
        assertEquals(PedroAuto.FlowerAction.ARM_OUT, warm.get(2).kind);
        assertEquals(0, warm.get(2).seconds, 1e-9);
        // The intake still runs up to the arm action, so nothing else changes.
        assertTrue(PedroAuto.FlowerIntakeOn(warm, 2));
        assertFalse(PedroAuto.FlowerIntakeOn(warm, 3));
    }

    @Test
    @DisplayName("step text: markers by name, a drive by where it goes and how far is left")
    void stepText() {
        assertEquals("FLOWER_OUT", PedroAuto.StepText(PedroAuto.FLOWER_OUT, null));
        assertEquals("LAUNCH_UNTIL_EMPTY", PedroAuto.StepText(PedroAuto.LAUNCH_UNTIL_EMPTY, null));
        Path drive = line(new Pose(0, 0), new Pose(30, 40)).constant(Math.PI / 2);
        assertEquals("drive to (30.0, 40.0) facing 90 deg", PedroAuto.StepText(drive, null));
        assertEquals("drive to (30.0, 40.0) facing 90 deg, 25.0 in left",
                PedroAuto.StepText(drive, new Pose(30, 15, 0)));
    }

    @Test
    @DisplayName("telemetry groups: each switch hides only its own group; quiet hides all")
    void telemetryGroups() {
        for (TelemetryGroup group : TelemetryGroup.values()) {
            assertTrue(p.TelemetryOn(group), group + " on by default");
        }
        p.tlmVision = false;
        assertFalse(p.TelemetryOn(TelemetryGroup.VISION));
        assertTrue(p.TelemetryOn(TelemetryGroup.LAUNCHER));

        HydraOpMode op = new HydraOpMode(null, null, null, null);
        assertTrue(op.Shows(TelemetryGroup.VISION), "no properties: everything shows");
        op.mProperties = p;
        assertFalse(op.Shows(TelemetryGroup.VISION));
        assertTrue(op.Shows(TelemetryGroup.DRIVE));
        op.mQuiet = true;
        assertFalse(op.Shows(TelemetryGroup.DRIVE));
    }
}
