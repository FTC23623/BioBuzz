package org.firstinspires.ftc.teamcode.subsystems;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The light with a pretend servo and a pretend camera. */
@DisplayName("Indicator light")
class IndicatorTest {
    private final BotProperties properties = new BotProperties();
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private HydraOpMode op;
    private Servo light;
    private Vision vision;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        light = mock(Servo.class);
        vision = mock(Vision.class);
        op = new HydraOpMode(telemetry, hardwareMap, mock(Gamepad.class), mock(Gamepad.class));
    }

    private Indicator lightConfigured() {
        when(hardwareMap.tryGet(Servo.class, properties.cfgIndicator)).thenReturn(light);
        return new Indicator(op, properties, vision);
    }

    @Test
    @DisplayName("three or more tags in view: solid green")
    void greenForThree() {
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(3);
        for (int loop = 0; loop < 20; loop++) {
            indicator.Process();
        }
        verify(light).setPosition(properties.indicatorGreen);
        verify(light, never()).setPosition(properties.indicatorBlue);
        verify(light, times(1)).setPosition(properties.indicatorOff);   // only the "off" from being built
    }

    @Test
    @DisplayName("two tags in view: solid blue")
    void blueForTwo() {
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(2);
        for (int loop = 0; loop < 20; loop++) {
            indicator.Process();
        }
        verify(light).setPosition(properties.indicatorBlue);
        verify(light, never()).setPosition(properties.indicatorGreen);
        verify(light, times(1)).setPosition(properties.indicatorOff);
    }

    @Test
    @DisplayName("exactly one tag: blinks, blue and off taking turns")
    void blinksForOne() {
        properties.indicatorBlinkMs = 0;   // every loop is a new half of the blink
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(1);
        for (int loop = 0; loop < 6; loop++) {
            indicator.Process();
        }
        verify(light, atLeast(3)).setPosition(properties.indicatorBlue);
        verify(light, atLeast(3)).setPosition(properties.indicatorOff);
        verify(light, never()).setPosition(properties.indicatorRed);
        verify(light, never()).setPosition(properties.indicatorGreen);
    }

    @Test
    @DisplayName("one tag, blink still on its first half: blue, not off")
    void blinkStartsBlue() {
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(1);
        indicator.Process();
        verify(light).setPosition(properties.indicatorBlue);
    }

    @Test
    @DisplayName("no tag: red")
    void redWhenNot() {
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(false);
        indicator.Process();
        verify(light).setPosition(properties.indicatorRed);
        verify(telemetry).addData(eq("Indicator light"), contains("no tag in view"));
    }

    @Test
    @DisplayName("no tag at all: solid red, never blinking")
    void solidRedWhenNothing() {
        properties.indicatorBlinkMs = 0;
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(false);
        when(vision.CountIgnoredTags()).thenReturn(0);
        for (int loop = 0; loop < 6; loop++) {
            indicator.Process();
        }
        verify(light, times(1)).setPosition(properties.indicatorRed);
        verify(light, times(1)).setPosition(properties.indicatorOff);   // only the "off" from being built
    }

    @Test
    @DisplayName("only the other alliance's tags in view: blinks red, and the Driver Station says to check the magnet")
    void blinksRedForTheirTags() {
        properties.indicatorBlinkMs = 0;   // every loop is a new half of the blink
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(false);
        when(vision.CountTags()).thenReturn(0);
        when(vision.CountIgnoredTags()).thenReturn(2);
        for (int loop = 0; loop < 6; loop++) {
            indicator.Process();
        }
        verify(light, atLeast(3)).setPosition(properties.indicatorRed);
        verify(light, atLeast(3)).setPosition(properties.indicatorOff);
        verify(light, never()).setPosition(properties.indicatorBlue);
        verify(light, never()).setPosition(properties.indicatorGreen);
        verify(telemetry, atLeast(1)).addData(eq("Indicator light"), contains("other alliance"));
        verify(telemetry, atLeast(1)).addData(eq("Indicator light"), contains("Check the alliance magnet"));
    }

    @Test
    @DisplayName("our tags and theirs both in view: ours win, blue not red")
    void oursWinOverTheirs() {
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(2);
        when(vision.CountIgnoredTags()).thenReturn(2);
        indicator.Process();
        verify(light).setPosition(properties.indicatorBlue);
        verify(light, never()).setPosition(properties.indicatorRed);
        verify(telemetry).addData(eq("Indicator light"), contains("2 of our tags"));
    }

    @Test
    @DisplayName("waiting for a tag: blinks orange, whatever the camera sees")
    void orangeWhileWaiting() {
        properties.indicatorBlinkMs = 0;
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(3);
        indicator.SetWaitingForTag(true);
        for (int loop = 0; loop < 6; loop++) {
            indicator.Process();
        }
        verify(light, atLeast(3)).setPosition(properties.indicatorOrange);
        verify(light, never()).setPosition(properties.indicatorGreen);
    }

    @Test
    @DisplayName("launching wins over waiting: yellow, not orange")
    void launchingBeatsWaiting() {
        Indicator indicator = lightConfigured();
        indicator.SetWaitingForTag(true);
        indicator.SetLaunching(true);
        indicator.Process();
        verify(light).setPosition(properties.indicatorYellow);
        verify(light, never()).setPosition(properties.indicatorOrange);
    }

    @Test
    @DisplayName("off when built, before anything is seen")
    void offAtFirst() {
        lightConfigured();
        verify(light).setPosition(properties.indicatorOff);
    }

    @Test
    @DisplayName("the same color is sent once, not fifty times a second")
    void sendsOnlyChanges() {
        Indicator indicator = lightConfigured();
        when(vision.SeesTag()).thenReturn(true);
        when(vision.CountTags()).thenReturn(3);
        for (int loop = 0; loop < 50; loop++) {
            indicator.Process();
        }
        verify(light, times(1)).setPosition(properties.indicatorGreen);
    }

    @Test
    @DisplayName("no light in the robot configuration: nothing crashes, and INIT says so")
    void noLightConfigured() {
        Indicator indicator = new Indicator(op, properties, vision);
        indicator.Init();
        when(vision.SeesTag()).thenReturn(true);
        indicator.Process();
        verify(light, never()).setPosition(anyDouble());
        verify(telemetry).addLine(contains("No indicator light"));
        // Every loop still says what the light would show, so a missing light is noticed.
        verify(telemetry).addData(eq("Indicator light"), contains("NOT CONFIGURED"));
    }
}
