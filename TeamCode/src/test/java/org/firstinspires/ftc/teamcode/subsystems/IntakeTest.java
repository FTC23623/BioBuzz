package org.firstinspires.ftc.teamcode.subsystems;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The intake and transfer with pretend motors. */
@DisplayName("Intake")
class IntakeTest {
    private final BotProperties properties = new BotProperties();
    private DcMotor intakeMotor;
    private DcMotor transferMotor;
    private HydraOpMode op;

    @BeforeEach
    void setUp() {
        intakeMotor = mock(DcMotor.class);
        transferMotor = mock(DcMotor.class);
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.get(DcMotor.class, "intake")).thenReturn(intakeMotor);
        when(hardwareMap.get(DcMotor.class, "transfer")).thenReturn(transferMotor);
        Gamepad gamepad = mock(Gamepad.class);
        op = new HydraOpMode(mock(Telemetry.class), hardwareMap, gamepad, gamepad);
    }

    @Test
    @DisplayName("Init: both motors at 0, nothing spins before START")
    void initStopped() {
        new Intake(op, properties).Init();
        verify(intakeMotor).setPower(0);
        verify(transferMotor).setPower(0);
    }

    @Test
    @DisplayName("Process: both motors at full power")
    void processFullPower() {
        Intake intake = new Intake(op, properties);
        intake.Process();
        verify(intakeMotor).setPower(1.0);
        verify(transferMotor).setPower(1.0);
    }

    @Test
    @DisplayName("a changed power is used, and the reversed settings flip the direction")
    void settingsReachMotors() {
        properties.intakePower = 0.5;
        properties.transferReversed = true;
        Intake intake = new Intake(op, properties);
        intake.Process();
        verify(intakeMotor).setPower(0.5);
        verify(transferMotor).setDirection(DcMotorSimple.Direction.REVERSE);
        verify(intakeMotor).setDirection(DcMotorSimple.Direction.FORWARD);
    }
}
