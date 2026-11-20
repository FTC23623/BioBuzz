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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

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
        // These tests check the plain underglow during INIT; the snake tests turn it back on.
        properties.stripInitSnake = false;
        // The stop numbers below are written one past the last LED; stopIsLastLed checks the other way.
        properties.prismStopIsOnePastLast = true;
    }

    // ---- The INIT snake (stripInitSnake) ----

    @Test
    @DisplayName("snake is on at INIT by default")
    void snakeOnByDefault() {
        Assertions.assertTrue(new BotProperties().stripInitSnake);
    }

    @Test
    @DisplayName("INIT snake: drawn in the alliance color with one yellow pollen LED; the plain underglow is not")
    void snakeDuringInit() {
        properties.stripInitSnake = true;
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        // Starts 3 long on LEDs 0 to 2 (stop 3, one past the last).
        verify(prism).SolidColor(LightStrip.SNAKE_LAYER, 0, 3, properties.stripBrightness, 255, 0, 0);
        // One pollen LED: start and one-past-last differ by exactly one.
        verify(prism, times(1)).SolidColor(eq(LightStrip.SNAKE_POLLEN_LAYER), anyInt(), anyInt(),
                anyInt(), eq(255), eq(200), eq(0));
        verify(prism, never()).SolidColor(eq(LightStrip.ALLIANCE_LAYER), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("INIT snake: a step only moves its LEDs (no new color, so no flash); a new alliance sends the color fresh")
    void snakeMoves() {
        properties.stripInitSnake = true;
        properties.snakeStepSeconds = 0;   // every loop is a step
        op.mAlliance = Alliance.BLUE;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();   // drawn blue on 0 to 2
        strip.ShowStatus();   // one step: head on 3 (stop 4), whether or not it ate
        verify(prism, times(1)).SolidColor(eq(LightStrip.SNAKE_LAYER), anyInt(), anyInt(), anyInt(),
                eq(0), eq(0), eq(255));
        verify(prism).MoveRun(eq(LightStrip.SNAKE_LAYER), anyInt(), eq(4), ArgumentMatchers.anyBoolean());

        op.mAlliance = Alliance.RED;
        strip.ShowStatus();
        verify(prism, times(1)).SolidColor(eq(LightStrip.SNAKE_LAYER), anyInt(), eq(5), anyInt(),
                eq(255), eq(0), eq(0));
    }

    @Test
    @DisplayName("START: the snake stops for good, every layer cleared, the plain underglow back")
    void snakeStopsAtStart() {
        properties.stripInitSnake = true;
        properties.snakeStepSeconds = 0;
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        strip.ShowStatus();
        strip.Process();   // START
        InOrder order = inOrder(prism);
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            order.verify(prism).ClearLayer(layer);
        }
        order.verify(prism).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);
        // More loops after START: the snake never draws again.
        Mockito.clearInvocations(prism);
        for (int loop = 0; loop < 20; loop++) {
            strip.Process();
        }
        verify(prism, never()).SolidColor(eq(LightStrip.SNAKE_LAYER), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt());
        verify(prism, never()).SolidColor(eq(LightStrip.SNAKE_POLLEN_LAYER), anyInt(), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt());
    }

    private LightStrip stripConfigured() {
        when(hardwareMap.tryGet(PrismLedDriver.class, properties.cfgStrip)).thenReturn(prism);
        return new LightStrip(op, properties, pollenSensor);
    }

    @Test
    @DisplayName("Init reads the device id and status once")
    void initOnce() {
        LightStrip strip = stripConfigured();
        strip.Init();
        strip.Init();
        verify(prism, times(1)).ReadDeviceId();
        verify(prism, times(1)).ReadStatus();
    }

    @Test
    @DisplayName("the gamepad does nothing to the strip: the D-pad belongs to the drive now")
    void noButtons() {
        LightStrip strip = stripConfigured();
        gamepad.dpad_up = true;
        gamepad.dpad_left = true;
        for (int loop = 0; loop < 30; loop++) {
            strip.HandleUserInput();
        }
        verify(prism, never()).SolidColor(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(prism, never()).ClearLayer(anyInt());
    }

    @Test
    @DisplayName("the underglow shows the alliance from INIT on, sent only when it changes")
    void allianceOnUnderglow() {
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        strip.ShowStatus();
        // LEDs 0 to 35, both lit: the Prism is sent stop 36, one past the last.
        verify(prism, times(1)).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);

        op.mAlliance = Alliance.BLUE;
        strip.ShowStatus();
        verify(prism, times(1)).SolidColor(1, 0, 36, properties.stripBrightness, 0, 0, 255);
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
        verify(prism, times(1)).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);
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
        verify(prism, times(1)).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);

        when(pollenTouch.isPressed()).thenReturn(false);
        strip.Process();
        strip.Process();
        // Cleared again (layer 2 for the second time), then the alliance comes back.
        order.verify(prism).ClearLayer(2);
        order.verify(prism).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);
        verify(prism, times(2)).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);
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
    @DisplayName("a full clear: EVERY layer is cleared, then the underglow comes back")
    void fullClear() {
        op.mAlliance = Alliance.BLUE;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        strip.ClearEverything();
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            verify(prism, times(1)).ClearLayer(layer);
        }
        strip.ShowStatus();
        verify(prism, times(2)).SolidColor(1, 0, 36, properties.stripBrightness, 0, 0, 255);
    }

    @Test
    @DisplayName("INIT: every layer cleared first, then the alliance drawn on 0 to 35")
    void initClearsThenDraws() {
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.Init();
        strip.Init();
        InOrder order = inOrder(prism);
        for (int layer = 0; layer < PrismLedDriver.LAYERS; layer++) {
            order.verify(prism).ClearLayer(layer);
        }
        order.verify(prism).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);
        // Cleared once only, not every INIT loop.
        verify(prism, times(1)).ClearLayer(9);
    }

    @Test
    @DisplayName("pollen already loaded at INIT: the alliance shows until START, then yellow")
    void pollenWaitsForStart() {
        properties.pollenHoldMs = 60_000;   // a long hold, so this test never times out
        when(pollenTouch.isPressed()).thenReturn(true);
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.Init();
        strip.Init();
        // INIT: the alliance color, so the drive team can check it. No yellow.
        verify(prism, times(1)).SolidColor(1, 0, 36, properties.stripBrightness, 255, 0, 0);
        verify(prism, never()).SolidColor(eq(2), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        // START: the first Process() shows the pollen.
        strip.Process();
        verify(prism, times(1)).SolidColor(2, 0, 36, properties.stripBrightness, 255, 200, 0);
    }

    @Test
    @DisplayName("prismStopIsOnePastLast false: the last LED itself is sent as stop")
    void stopIsLastLed() {
        properties.prismStopIsOnePastLast = false;
        op.mAlliance = Alliance.BLUE;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        verify(prism).SolidColor(1, 0, 35, properties.stripBrightness, 0, 0, 255);
    }

    @Test
    @DisplayName("today (stop is the last LED): the INIT snake's pollen is ONE LED, start and stop the same")
    void snakePollenIsOneLed() {
        properties.prismStopIsOnePastLast = new BotProperties().prismStopIsOnePastLast;
        properties.stripInitSnake = true;
        op.mAlliance = Alliance.RED;
        LightStrip strip = stripConfigured();
        strip.ShowStatus();
        ArgumentCaptor<Integer> start = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> stop = ArgumentCaptor.forClass(Integer.class);
        verify(prism).SolidColor(eq(LightStrip.SNAKE_POLLEN_LAYER), start.capture(), stop.capture(),
                anyInt(), eq(255), eq(200), eq(0));
        assertEquals(start.getValue(), stop.getValue());
        // And the 3-long snake is sent as 0 to 2.
        verify(prism).SolidColor(LightStrip.SNAKE_LAYER, 0, 2, properties.stripBrightness, 255, 0, 0);
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
        LightStrip strip = new LightStrip(op, properties, pollenSensor);
        strip.Init();
        strip.Process();
        verify(prism, never()).SolidColor(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(prism, never()).WriteLayer(anyInt(), anyInt(), any(byte[].class));
        verify(telemetry).addLine(contains("No light strip"));
    }
}
