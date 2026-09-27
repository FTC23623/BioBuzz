package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DigitalChannel;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.objects.Subsystem;
import org.firstinspires.ftc.teamcode.types.Constants;

/**
 * Reads the Safe Mode toggle switch on the robot and sets {@code HydraOpMode.mSafeMode}.
 *
 * <p>The hub's digital inputs are pulled high inside the hub. So an OPEN switch, an
 * unplugged cable, and a broken wire all read high, and all mean SAFE. Only a switch that
 * is closed (connecting the signal to ground) means FULL speed. If the switch is missing
 * from the robot configuration, that is SAFE too.
 *
 * <p>Read during INIT only. Flipping the switch after START does nothing until the next INIT.
 */
public class SafeModeSwitch implements Subsystem {
    private final HydraOpMode mOp;
    /** null when "safeMode" is not in the robot configuration. */
    private final DigitalChannel mSwitch;

    public SafeModeSwitch(HydraOpMode op) {
        mOp = op;
        mSwitch = op.mHardwareMap.tryGet(DigitalChannel.class, Constants.cfgSafeModeSwitch);
        if (mSwitch != null) {
            mSwitch.setMode(DigitalChannel.Mode.INPUT);
        }
        Read();
    }

    /** Read the switch and show the result on the Driver Station. Call only before START. */
    public void Read() {
        int limitPercent = (int) Math.round(Constants.safeModeMaxPower * 100);
        if (mSwitch == null) {
            mOp.mSafeMode = true;
            mOp.mTelemetry.addLine("SAFE MODE: no '" + Constants.cfgSafeModeSwitch
                    + "' switch in the robot configuration. Drive limited to " + limitPercent + "%.");
            return;
        }

        // An open switch reads true (high). See the note at the top of this class.
        boolean switchIsOpen = mSwitch.getState();
        if (switchIsOpen) {
            mOp.mSafeMode = true;
            mOp.mTelemetry.addLine("SAFE MODE: switch is open. Drive limited to " + limitPercent + "%.");
        } else {
            mOp.mSafeMode = false;
            mOp.mTelemetry.addLine("FULL SPEED: Safe Mode switch is closed.");
        }
    }

    @Override
    public boolean Init() {
        Read();
        return true;
    }

    @Override
    public void HandleUserInput() {
    }

    /** Nothing: the setting is locked at START. */
    @Override
    public void Process() {
    }
}
