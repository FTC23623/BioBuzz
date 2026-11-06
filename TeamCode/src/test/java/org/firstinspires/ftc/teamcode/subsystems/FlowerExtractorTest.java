package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

/** The flower extractor with a pretend servo and a pretend operator gamepad. */
@DisplayName("FlowerExtractor")
class FlowerExtractorTest {
    private final BotProperties properties = new BotProperties();
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private Servo servo;
    private Gamepad gamepad;
    private HydraOpMode op;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        servo = mock(Servo.class);
        gamepad = mock(Gamepad.class);
        op = new HydraOpMode(telemetry, hardwareMap, mock(Gamepad.class), gamepad);
    }

    private FlowerExtractor servoConfigured() {
        when(hardwareMap.tryGet(Servo.class, properties.cfgFlowerExtractor)).thenReturn(servo);
        return new FlowerExtractor(op, properties, gamepad);
    }

    private static void loop(FlowerExtractor extractor) {
        extractor.HandleUserInput();
        extractor.Process();
    }

    @Test
    @DisplayName("nothing moves during INIT")
    void stillAtInit() {
        FlowerExtractor extractor = servoConfigured();
        extractor.Init();
        verify(servo, never()).setPosition(anyDouble());
    }

    @Test
    @DisplayName("no triangle: stored")
    void storedByDefault() {
        FlowerExtractor extractor = servoConfigured();
        loop(extractor);
        assertFalse(extractor.IsDeployed());
        verify(servo).setPosition(properties.flowerExtractorClosed);
        verify(servo, never()).setPosition(properties.flowerExtractorOpen);
    }

    @Test
    @DisplayName("triangle held: deployed")
    void triangleDeploys() {
        FlowerExtractor extractor = servoConfigured();
        gamepad.triangle = true;
        loop(extractor);
        assertTrue(extractor.IsDeployed());
        verify(servo).setPosition(properties.flowerExtractorOpen);
    }

    @Test
    @DisplayName("hold to deploy: letting go stores it right away, no second press")
    void releaseStores() {
        FlowerExtractor extractor = servoConfigured();
        gamepad.triangle = true;
        loop(extractor);
        gamepad.triangle = false;
        loop(extractor);
        assertFalse(extractor.IsDeployed());
        verify(servo).setPosition(properties.flowerExtractorOpen);
        verify(servo).setPosition(properties.flowerExtractorClosed);
    }

    @Test
    @DisplayName("no servo in the robot configuration: nothing crashes, and INIT says so")
    void noServo() {
        FlowerExtractor extractor = new FlowerExtractor(op, properties, gamepad);
        extractor.Init();
        gamepad.triangle = true;
        loop(extractor);
        verify(servo, never()).setPosition(anyDouble());
        verify(telemetry).addLine(contains("Flower extractor: MISSING"));
    }
}
