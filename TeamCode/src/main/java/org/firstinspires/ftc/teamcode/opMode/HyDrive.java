package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.Drive_Manual;
import org.firstinspires.ftc.teamcode.subsystems.Imu;
import org.firstinspires.ftc.teamcode.subsystems.Imu_Pinpoint;

/**
 * The one TeleOp. There is no red or blue version.
 *
 * <p>Drive only for now. Other mechanisms join {@code mSystems} as they are ported.
 * The loop is Decode's HyDrive loop: clear the hub caches, every subsystem reads the
 * gamepad, every subsystem does its work, send telemetry once.
 *
 * <p>At INIT, point the robot away from the driver. That direction becomes "forward"
 * for field-centric drive. Press circle during the match to re-set it.
 */
@TeleOp(name = "HyDrive")
public class HyDrive extends OpMode_Base {

    @Override
    public void runOpMode() {
        ElapsedTime loopTimer = new ElapsedTime(ElapsedTime.Resolution.MILLISECONDS);
        mOpMode = new HydraOpMode(telemetry, hardwareMap, gamepad1, gamepad2);

        Imu imu = new Imu_Pinpoint(mOpMode);
        mSystems.add(imu);
        mSystems.add(new Drive_Manual(mOpMode, imu));

        SetLynxHubsManual();
        InitializeAllSystems();

        while (!isStarted() && !isStopRequested()) {
            ClearLynxHubCaches();
            imu.Process();
            telemetry.addData("Heading ready", imu.Ready());
            telemetry.addData("Yaw", imu.GetYaw());
            telemetry.update();
            idle();
        }

        loopTimer.reset();
        while (opModeIsActive()) {
            ClearLynxHubCaches();
            mOpMode.mLoopTime = loopTimer.milliseconds();
            loopTimer.reset();
            telemetry.addData("LoopTime", mOpMode.mLoopTime);
            for (Subsystem system : mSystems) {
                system.HandleUserInput();
            }
            for (Subsystem system : mSystems) {
                system.Process();
            }
            telemetry.update();
            idle();
        }
    }
}
