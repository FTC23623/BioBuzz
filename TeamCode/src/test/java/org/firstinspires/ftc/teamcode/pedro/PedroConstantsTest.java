package org.firstinspires.ftc.teamcode.pedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pedropathing.revhub.localizers.PinpointConfig;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pedro's Pinpoint settings come from the same BotProperties numbers the drive uses. */
@DisplayName("PedroConstants")
class PedroConstantsTest {
    private final BotProperties properties = new BotProperties();

    @Test
    @DisplayName("the Pinpoint config uses the drive's pod offsets, in inches")
    void pinpointOffsetsMatchTheDrive() {
        PinpointConfig c = PedroConstants.PinpointLocalizerConfig(properties);
        assertEquals(properties.cfgPinpoint, c.name.get());
        assertEquals(DistanceUnit.INCH, c.offsetUnits.get());
        // Decode's numbers: -1209.2 ticks x 0.00195 in/tick = -2.36 in; -2661.4 x 0.00195 = -5.20 in.
        assertEquals(-2.3612, c.xPodOffset.get(), 0.001);
        assertEquals(-5.1968, c.yPodOffset.get(), 0.001);
    }

    @Test
    @DisplayName("ticks per inch is the drive's inches per tick, turned over")
    void ticksPerInch() {
        PinpointConfig c = PedroConstants.PinpointLocalizerConfig(properties);
        assertEquals(DistanceUnit.INCH, c.encoderResolutionUnit.get());
        assertTrue(c.ticksPerUnit.get().isPresent());
        assertEquals(1 / properties.pinpointInPerTick, c.ticksPerUnit.get().getAsDouble(), 1e-9);
        assertEquals(512.1, c.ticksPerUnit.get().getAsDouble(), 0.1);
    }

    @Test
    @DisplayName("a 'pinpoint' in the robot configuration means the Pinpoint localizer")
    void picksPinpointWhenConfigured() {
        HardwareMap hardwareMap = mock(HardwareMap.class);
        assertFalse(PedroConstants.UsesPinpoint(hardwareMap, properties));
        when(hardwareMap.tryGet(GoBildaPinpointDriver.class, properties.cfgPinpoint))
                .thenReturn(mock(GoBildaPinpointDriver.class));
        assertTrue(PedroConstants.UsesPinpoint(hardwareMap, properties));
    }
}
