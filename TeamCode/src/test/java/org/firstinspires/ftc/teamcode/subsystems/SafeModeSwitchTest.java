package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.firstinspires.ftc.teamcode.types.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SafeModeSwitch")
class SafeModeSwitchTest {
    private HardwareMap hardwareMap;
    private Telemetry telemetry;
    private HydraOpMode op;

    @BeforeEach
    void setUp() {
        hardwareMap = mock(HardwareMap.class);
        telemetry = mock(Telemetry.class);
        op = new HydraOpMode(telemetry, hardwareMap, mock(Gamepad.class), mock(Gamepad.class));
    }

    private DigitalChannel wiredSwitch(boolean state) {
        DigitalChannel sw = mock(DigitalChannel.class);
        when(sw.getState()).thenReturn(state);
        when(hardwareMap.tryGet(DigitalChannel.class, Constants.cfgSafeModeSwitch)).thenReturn(sw);
        return sw;
    }

    @Test
    @DisplayName("every OpMode starts in Safe Mode, before the switch is read")
    void defaultIsSafe() {
        assertTrue(op.mSafeMode);
    }

    @Test
    @DisplayName("switch missing from the robot configuration: SAFE, with a warning")
    void missingIsSafe() {
        op.mSafeMode = false;
        new SafeModeSwitch(op);
        assertTrue(op.mSafeMode);
        verify(telemetry).addLine(contains("no 'safeMode' switch"));
    }

    @Test
    @DisplayName("switch open (reads high; also an unplugged or broken wire): SAFE")
    void openIsSafe() {
        op.mSafeMode = false;
        wiredSwitch(true);
        new SafeModeSwitch(op);
        assertTrue(op.mSafeMode);
        verify(telemetry).addLine(contains("switch is open"));
    }

    @Test
    @DisplayName("switch closed (reads low): FULL speed")
    void closedIsFull() {
        wiredSwitch(false);
        new SafeModeSwitch(op);
        assertFalse(op.mSafeMode);
        verify(telemetry).addLine(contains("FULL SPEED"));
    }

    @Test
    @DisplayName("the switch is set up as an input")
    void setsInputMode() {
        DigitalChannel sw = wiredSwitch(true);
        new SafeModeSwitch(op);
        verify(sw).setMode(eq(DigitalChannel.Mode.INPUT));
    }

    @Test
    @DisplayName("flipping the switch during INIT is picked up by the next Read()")
    void rereadDuringInit() {
        DigitalChannel sw = wiredSwitch(true);
        SafeModeSwitch safeMode = new SafeModeSwitch(op);
        assertTrue(op.mSafeMode);
        when(sw.getState()).thenReturn(false);
        safeMode.Read();
        assertFalse(op.mSafeMode);
    }

    @Test
    @DisplayName("after START, Process() never changes the setting")
    void lockedAfterStart() {
        DigitalChannel sw = wiredSwitch(true);
        SafeModeSwitch safeMode = new SafeModeSwitch(op);
        when(sw.getState()).thenReturn(false);
        safeMode.HandleUserInput();
        safeMode.Process();
        assertTrue(op.mSafeMode);
    }
}
