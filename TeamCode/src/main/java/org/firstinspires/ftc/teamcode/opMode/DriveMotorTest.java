package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * Bench check for the drive wiring. WHEELS OFF THE GROUND. HAND ON STOP.
 *
 * <p>Hold a D-pad direction to spin one motor forward at low power:
 * up = front left, right = front right, down = back right, left = back left.
 * Every wheel should roll the way the robot drives forward. If one spins backward or
 * the wrong wheel moves, fix the wiring or the configuration before running HyDrive.
 *
 * <p>Always runs at the Safe Mode speed, key or no key.
 */
@TeleOp(name = "Test: Drive Motors", group = "Test")
public class DriveMotorTest extends OpMode_Base {
    private static final double TEST_POWER = Constants.safeModeMaxPower;

    @Override
    public void runOpMode() {
        DcMotorEx frontLeft = hardwareMap.get(DcMotorEx.class, Constants.cfgDriveFrontLeft);
        DcMotorEx frontRight = hardwareMap.get(DcMotorEx.class, Constants.cfgDriveFrontRight);
        DcMotorEx backLeft = hardwareMap.get(DcMotorEx.class, Constants.cfgDriveBackLeft);
        DcMotorEx backRight = hardwareMap.get(DcMotorEx.class, Constants.cfgDriveBackRight);
        // Same directions as Drive, so "forward" here matches "forward" in HyDrive.
        frontLeft.setDirection(DcMotor.Direction.FORWARD);
        backLeft.setDirection(DcMotor.Direction.FORWARD);
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        telemetry.addLine("WHEELS OFF THE GROUND. HAND ON STOP.");
        telemetry.addLine("D-pad: up=FL  right=FR  down=BR  left=BL");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            frontLeft.setPower(gamepad1.dpad_up ? TEST_POWER : 0);
            frontRight.setPower(gamepad1.dpad_right ? TEST_POWER : 0);
            backRight.setPower(gamepad1.dpad_down ? TEST_POWER : 0);
            backLeft.setPower(gamepad1.dpad_left ? TEST_POWER : 0);
            telemetry.addData("FL ticks", frontLeft.getCurrentPosition());
            telemetry.addData("FR ticks", frontRight.getCurrentPosition());
            telemetry.addData("BL ticks", backLeft.getCurrentPosition());
            telemetry.addData("BR ticks", backRight.getCurrentPosition());
            telemetry.update();
        }
    }
}
