package org.firstinspires.ftc.teamcode.opMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** When an auto's LAUNCH may open the gates: the pollen wheel fast enough, or after waiting as long as allowed. */
@DisplayName("Auto LAUNCH spin-up")
class LaunchSpinUpTest {
    private final BotProperties properties = new BotProperties();

    @Test
    @DisplayName("pollen wheel at 2800 RPM or more: open, first launch or not")
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
    @DisplayName("the launch speed is 2800 RPM")
    void minRpm() {
        org.junit.jupiter.api.Assertions.assertEquals(2800, properties.autoLaunchMinRpm, 1e-9);
    }
}
