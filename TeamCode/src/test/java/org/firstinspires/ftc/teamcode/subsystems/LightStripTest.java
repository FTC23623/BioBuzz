package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;
import org.firstinspires.ftc.teamcode.drivers.PrismLedDriver;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.types.Alliance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** The strip with a pretend Prism driver: we check exactly which bytes it is sent. */
@DisplayName("LightStrip (Prism driver)")
class LightStripTest {
    private final BotProperties properties = new BotProperties();
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private Gamepad gamepad;
    private PrismLedDriver prism;
    private HydraOpMode op;
    private TouchSensor pollenTouch;
    private PollenSensor pollenSensor;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        gamepad = mock(Gamepad.class);
        prism = mock(PrismLedDriver.class);
        when(prism.ReadStatus()).thenReturn(new byte[] {12, 60, 0, 0});
        op = new HydraOpMode(telemetry, hardwareMap, gamepad, mock(Gamepad.class));
        pollenTouch = mock(TouchSensor.class);
        when(hardwareMap.tryGet(TouchSensor.class, properties.cfgPollenSensor)).thenReturn(pollenTouch);
        // Plain wiring in these tests: the switch closed means pollen. PollenSensorTest covers the flip.
        properties.pollenSensorInverted = false;
        pollenSensor = new PollenSensor(op, properties);
    }

    private LightStrip stripConfigured() {
        when(hardwareMap.tryGet(PrismLedDriver.class, properties.cfgStrip)).thenReturn(prism);
        return new LightStrip(op, properties, gamepad, pollenSensor);
    }

    @Test
    @DisplayName("Init reads the device id and status once, and shows them")
    void initOnce() {
        LightStrip strip = stripConfigured();
        strip.Init();
        strip.Init();
        verify(prism, times(1)).ReadDeviceId();
        verify(prism, times(1)).ReadStatus();
        verify(telemetry, times(2)).addLine(contains("D-pad: up red"));
    }

    @Test
    @DisplayName("solid red: the LEDs before the underglow, layer 0, at the set brightness")
    void solidRed() {
        properties.underglowFirstLed = 12;
        properties.stripBrightness = 50;
        LightStrip strip = stripConfigured();
        strip.Solid(255, 0, 0);
        verify(prism).SolidColor(0, 0, 12, 50, 255, 0, 0);
    }

    @Test
    @DisplayName("the underglow shows the alliance from INIT on, sent only when it changes")
    void allianceOnUnderglow() {
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        strip.ShowStatus();
        // LEDs 12 to 35, both lit: the Prism is sent stop 36, one past the last.
        verify(prism, times(1)).SolidColor(1, 12, 36, properties.stripBrightness, 255, 0, 0);

        op.mAlliance = Alliance.BLUE;
        strip.ShowStatus();
        verify(prism, times(1)).SolidColor(1, 12, 36, properties.stripBrightness, 0, 0, 255);
    }

    @Test
    @DisplayName("START changes nothing: the underglow is not sent again")
    void startChangesNothing() {
        op.mAlliance = Alliance.BLUE;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        strip.Process();
        strip.Process();
        verify(prism, times(1)).SolidColor(eq(1), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("pollen: every layer cleared, then the whole strip yellow, once; gone: cleared again, underglow back")
    void pollenYellow() {
        properties.pollenHoldMs = 0;   // no hold, so letting go clears right away
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        verify(prism, times(1)).SolidColor(1, 12, 36, properties.stripBrightness, 255, 0, 0);
        verify(prism, never()).SolidColor(eq(2), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        when(pollenTouch.isPressed()).thenReturn(true);
        strip.Process();
        strip.Process();
        InOrder order = inOrder(prism);
        // Every layer cleared first, so nothing else shows whichever way the Prism stacks them.
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            order.verify(prism).ClearLayer(layer);
        }
        // LEDs 0 to 35, both ends lit: stop 36, one past the last. One layer for the lot. Sent once.
        order.verify(prism).SolidColor(2, 0, 36, properties.stripBrightness, 255, 200, 0);
        verify(prism, times(1)).SolidColor(2, 0, 36, properties.stripBrightness, 255, 200, 0);
        // The underglow is not drawn back on top while the pollen shows.
        verify(prism, times(1)).SolidColor(1, 12, 36, properties.stripBrightness, 255, 0, 0);

        when(pollenTouch.isPressed()).thenReturn(false);
        strip.Process();
        strip.Process();
        // Cleared again (layer 2 for the second time), then the alliance comes back.
        order.verify(prism).ClearLayer(2);
        order.verify(prism).SolidColor(1, 12, 36, properties.stripBrightness, 255, 0, 0);
        verify(prism, times(2)).SolidColor(1, 12, 36, properties.stripBrightness, 255, 0, 0);
    }

    @Test
    @DisplayName("pollen hold: the switch opens for a moment (a hole in the ball) and the yellow stays")
    void pollenHoldBridgesTheHoles() {
        properties.pollenHoldMs = 60_000;   // a long hold, so this test never times out
        LightStrip strip = stripConfigured();
        when(pollenTouch.isPressed()).thenReturn(true);
        strip.Process();
        when(pollenTouch.isPressed()).thenReturn(false);
        strip.Process();
        strip.Process();
        verify(prism, times(1)).SolidColor(2, 0, 36, properties.stripBrightness, 255, 200, 0);
        // One clear only: the one before the yellow was drawn. Nothing cleared it since.
        verify(prism, times(1)).ClearLayer(2);
    }

    @Test
    @DisplayName("pollen hold: once the switch has stayed open for the hold time, the yellow clears")
    void pollenClearsAfterTheHold() throws InterruptedException {
        properties.pollenHoldMs = 20;
        LightStrip strip = stripConfigured();
        when(pollenTouch.isPressed()).thenReturn(true);
        strip.Process();
        when(pollenTouch.isPressed()).thenReturn(false);
        Thread.sleep(60);
        strip.Process();
        // Once before the yellow, once to take it down.
        verify(prism, times(2)).ClearLayer(2);
    }

    @Test
    @DisplayName("D-pad left (Off): EVERY layer is cleared, not just the D-pad layer, then the underglow comes back")
    void off() {
        op.mAlliance = Alliance.BLUE;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        strip.Off();
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            verify(prism, times(1)).ClearLayer(layer);
        }
        strip.ShowStatus();
        verify(prism, times(2)).SolidColor(1, 12, 36, properties.stripBrightness, 0, 0, 255);
    }

    @Test
    @DisplayName("a D-pad press sends one color, however long it is held")
    void dpadSendsOnce() {
        LightStrip strip = stripConfigured();
        gamepad.dpad_right = true;
        for (int loop = 0; loop < 30; loop++) {
            strip.HandleUserInput();
        }
        verify(prism, times(1)).SolidColor(0, 0, properties.underglowFirstLed, properties.stripBrightness, 0, 255, 0);
    }

    @Test
    @DisplayName("INIT: every layer cleared first, then the alliance drawn on 12 to 35")
    void initClearsThenDraws() {
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.Init();
        strip.Init();
        InOrder order = inOrder(prism);
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            order.verify(prism).ClearLayer(layer);
        }
        order.verify(prism).SolidColor(1, 12, 36, properties.stripBrightness, 255, 0, 0);
        // Cleared once only, not every INIT loop.
        verify(prism, times(1)).ClearLayer(9);
    }

    @Test
    @DisplayName("pollen already pressed at INIT: yellow comes back after the clear")
    void pollenRedrawnAfterClear() {
        when(pollenTouch.isPressed()).thenReturn(true);
        LightStrip strip = stripConfigured();
        strip.Init();
        InOrder order = inOrder(prism);
        order.verify(prism).ClearLayer(2);
        order.verify(prism).SolidColor(2, 0, 36, properties.stripBrightness, 255, 200, 0);
    }

    @Test
    @DisplayName("prismStopIsOnePastLast false: the last LED itself is sent as stop")
    void stopIsLastLed() {
        properties.prismStopIsOnePastLast = false;
        op.mAlliance = Alliance.BLUE;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        verify(prism).SolidColor(1, 12, 35, properties.stripBrightness, 0, 0, 255);
    }

    @Test
    @DisplayName("a 4-byte value goes lowest byte first")
    void bytes32() {
        assertArrayEquals(new byte[] {(byte) 0xD0, 0x07, 0, 0}, LightStrip.Bytes32(2000));
        assertArrayEquals(new byte[] {1, 0, 0, 0}, LightStrip.Bytes32(1));
    }

    @Test
    @DisplayName("no strip in the robot configuration: nothing is sent, nothing crashes, INIT says so")
    void noStrip() {
        LightStrip strip = new LightStrip(op, properties, gamepad, pollenSensor);
        strip.Init();
        strip.Solid(255, 255, 255);
        strip.Process();
        verify(prism, never()).SolidColor(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(prism, never()).WriteLayer(anyInt(), anyInt(), any(byte[].class));
        verify(telemetry).addLine(contains("No light strip"));
    }
}
