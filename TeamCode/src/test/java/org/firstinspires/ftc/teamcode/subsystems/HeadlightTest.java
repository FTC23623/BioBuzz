package org.firstinspires.ftc.teamcode.subsystems;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

/** The headlight with a pretend servo. */
@DisplayName("Headlight")
class HeadlightTest {
    private final BotProperties properties = new BotProperties();
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private HydraOpMode op;
    private Servo light;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        light = mock(Servo.class);
        op = new HydraOpMode(telemetry, hardwareMap, mock(Gamepad.class), mock(Gamepad.class));
    }

    @Test
    @DisplayName("off when built, on at INIT")
    void onAtInit() {
        when(hardwareMap.tryGet(Servo.class, properties.cfgHeadlight)).thenReturn(light);
        Headlight headlight = new Headlight(op, properties);
        verify(light, never()).setPosition(anyDouble());
        headlight.Init();
        verify(light).setPosition(properties.headlightOn);
    }

    @Test
    @DisplayName("no headlight in the robot configuration: nothing crashes, and INIT says so")
    void noLightConfigured() {
        Headlight headlight = new Headlight(op, properties);
        headlight.Init();
        headlight.Process();
        verify(telemetry).addLine(contains("No headlight"));
    }
}
