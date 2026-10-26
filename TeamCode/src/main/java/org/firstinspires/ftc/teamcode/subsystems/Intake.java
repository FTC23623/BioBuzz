package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;

/**
 * The intake and the transfer: two motors that pull balls in and move them up.
 *
 * <p>For now both just run at full power from START until STOP. No buttons, no sensors.
 * The powers are settings in BotProperties. Before START both are at 0.
 */
public class Intake implements Subsystem {
    private final HydraOpMode mOp;
    private final BotProperties mProperties;
    private final DcMotor mIntakeMotor;
    private final DcMotor mTransferMotor;

    private double mIntakePower = 0;
    private double mTransferPower = 0;

    public Intake(HydraOpMode op, BotProperties properties) {
        mOp = op;
        mProperties = properties;
        mIntakeMotor = op.mHardwareMap.get(DcMotor.class, properties.cfgIntake);
        mTransferMotor = op.mHardwareMap.get(DcMotor.class, properties.cfgTransfer);
        // Our code picks the power, so no encoder is needed.
        mIntakeMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        mTransferMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        mIntakeMotor.setDirection(properties.intakeReversed
                ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
        mTransferMotor.setDirection(properties.transferReversed
                ? DcMotorSimple.Direction.REVERSE : DcMotorSimple.Direction.FORWARD);
    }

    @Override
    public boolean Init() {
        mIntakeMotor.setPower(0);
        mTransferMotor.setPower(0);
        ShowStatus();
        return true;
    }

    /** Nothing to read yet: the motors do not use the gamepad. */
    @Override
    public void HandleUserInput() {
    }

    @Override
    public void Process() {
        mIntakePower = mProperties.intakePower;
        mTransferPower = mProperties.transferPower;
        mIntakeMotor.setPower(mIntakePower);
        mTransferMotor.setPower(mTransferPower);
        ShowStatus();
    }

    private void ShowStatus() {
        mOp.mTelemetry.addData("Intake power", "%.2f", mIntakePower);
        mOp.mTelemetry.addData("Transfer power", "%.2f", mTransferPower);
    }
}
