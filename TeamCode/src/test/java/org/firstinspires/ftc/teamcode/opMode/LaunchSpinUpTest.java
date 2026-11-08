package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** When an auto's LAUNCH may open the gates: the pollen wheel fast enough, or after waiting as long as allowed. */
@DisplayName("Auto LAUNCH spin-up")
class LaunchSpinUpTest {
    private final BotProperties properties = new BotProperties();

    @Test
    @DisplayName("pollen wheel at 2950 RPM or more: open, first launch or not")
    void fastEnoughOpens() {
        assertTrue(PedroAuto.ReadyToOpen(true, 0, true, properties));
        assertTrue(PedroAuto.ReadyToOpen(true, 0, false, properties));
    }

    @Test
    @DisplayName("the first launch keeps waiting past the usual 2 s, up to 5 s")
    void firstLaunchWaitsLonger() {
        assertFalse(PedroAuto.ReadyToOpen(false, 2.5, true, properties));
        assertFalse(PedroAuto.ReadyToOpen(false, 4.9, true, properties));
        assertTrue(PedroAuto.ReadyToOpen(false, 5.0, true, properties));
    }

    @Test
    @DisplayName("later launches give up after 2 s, as before")
    void laterLaunches() {
        assertFalse(PedroAuto.ReadyToOpen(false, 1.9, false, properties));
        assertTrue(PedroAuto.ReadyToOpen(false, 2.0, false, properties));
    }

    @Test
    @DisplayName("the launch speed is 2735 RPM, never above the near shot")
    void minRpm() {
        Assertions.assertEquals(2735, properties.autoLaunchMinRpm, 1e-9);
        assertTrue(properties.autoLaunchMinRpm <= properties.launchNearRpm);
    }

    @Test
    @DisplayName("settle (today): LAUNCH_WHEN_TAG_SEEN settles, a plain LAUNCH does not")
    void tagLaunchSettles() {
        assertTrue(PedroAuto.SettlesBeforeLaunch(PedroAuto.LAUNCH_WHEN_TAG_SEEN, properties));
        assertFalse(PedroAuto.SettlesBeforeLaunch(PedroAuto.LAUNCH, properties));
    }

    @Test
    @DisplayName("settle: autoLaunchSettles covers every launch; autoTagLaunchSettles off skips the tag launch")
    void settleSwitches() {
        properties.autoLaunchSettles = true;
        assertTrue(PedroAuto.SettlesBeforeLaunch(PedroAuto.LAUNCH, properties));
        properties.autoLaunchSettles = false;
        properties.autoTagLaunchSettles = false;
        assertFalse(PedroAuto.SettlesBeforeLaunch(PedroAuto.LAUNCH_WHEN_TAG_SEEN, properties));
    }

    @Test
    @DisplayName("auto aim, hive dead ahead: left of it turns 1 deg right, right of it 1 deg left, in front none")
    void autoAimHeading() {
        properties.aimHiveBearingDeg = 1;
        Assertions.assertEquals(Math.toRadians(-1), PedroAuto.AimHeading(0, 0, 20, properties), 1e-9);
        Assertions.assertEquals(Math.toRadians(1), PedroAuto.AimHeading(0, 0, -20, properties), 1e-9);
        Assertions.assertEquals(0, PedroAuto.AimHeading(0, 0, 0, properties), 1e-9);
    }
}
