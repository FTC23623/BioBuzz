package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A roller run by speed (today) or, with rollerUsePower, by plain power (0050). */
@DisplayName("Roller: speed or power")
class RollerTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private DcMotorEx motor;
    private Roller roller;

    @BeforeEach
    void setUp() {
        properties.intakeMaxRpm = 1150;
        properties.rollerUsePower = false;
        HardwareMap hardwareMap = mock(HardwareMap.class);
        motor = mock(DcMotorEx.class);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgTransfer)).thenReturn(motor);
        HydraOpMode op = new HydraOpMode(mock(Telemetry.class), hardwareMap, mock(Gamepad.class), mock(Gamepad.class));
        roller = new Roller("Transfer", op, properties, properties.cfgTransfer, false);
    }

    @Test
    @DisplayName("speed mode (today): the hub holds a speed; no plain power is sent")
    void speedMode() {
        roller.SetRpm(575);
        verify(motor).setVelocity(575.0 / 60 * properties.intakeTicksPerRev);
        verify(motor, never()).setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    @Test
    @DisplayName("power mode: the speed becomes a share of 1150; 575 RPM is half power")
    void powerMode() {
        properties.rollerUsePower = true;
        roller.SetRpm(575);
        verify(motor).setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        verify(motor).setPower(0.5);
        verify(motor, never()).setVelocity(anyDouble());
    }

    @Test
    @DisplayName("switched back on the dashboard: speed control again, and the speed is sent anew")
    void switchBack() {
        properties.rollerUsePower = true;
        roller.SetRpm(575);
        properties.rollerUsePower = false;
        roller.SetRpm(575);
        // Once at INIT, once now.
        verify(motor, times(2)).setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        verify(motor).setVelocity(575.0 / 60 * properties.intakeTicksPerRev);
    }

    @Test
    @DisplayName("power for a speed: a share of the top, never past full either way")
    void powerForRpm() {
        assertEquals(1.0, Roller.PowerForRpm(1150, properties), EPS);
        assertEquals(-400.0 / 1150, Roller.PowerForRpm(-400, properties), EPS);
        assertEquals(1.0, Roller.PowerForRpm(2000, properties), EPS);
        assertEquals(-1.0, Roller.PowerForRpm(-2000, properties), EPS);
    }
}
