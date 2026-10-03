package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.Launcher;

/**
 * Launcher speed test. Wheel guard on, hands clear, hand on STOP.
 *
 * <p>Gamepad 1:
 * <ul>
 *   <li><b>Hold right trigger</b>: spin the launcher at the target speed.
 *   <li><b>Right bumper / left bumper</b>: target speed up / down 50 RPM.
 * </ul>
 *
 * <p>The target starts at 1000 RPM. The Driver Station shows target and actual RPM, so
 * you can see how close the speed control gets. See docs/decisions/0009 for tuning.
 */
@TeleOp(name = "Test: Launcher", group = "Test")
public class LauncherMotorTest extends OpMode_Base {

    @Override
    public void runOpMode() {
        CreateHydraOpMode();
        Launcher launcher = new Launcher(mOpMode, gamepad1);
        mSystems.add(launcher);

        SetLynxHubsManual();
        InitializeAllSystems();

        while (!isStarted() && !isStopRequested()) {
            telemetry.addLine("Hold right trigger to spin. Bumpers change the speed.");
            telemetry.addData("Launcher target RPM", "%.0f", launcher.GetTargetRpm());
            telemetry.update();
            idle();
        }

        while (opModeIsActive()) {
            ClearLynxHubCaches();
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
