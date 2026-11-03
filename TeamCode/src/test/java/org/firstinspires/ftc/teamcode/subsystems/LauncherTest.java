package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import java.util.function.Consumer;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two launchers with pretend motors and a pretend operator gamepad. Each motor
 * remembers the last power it was given, and reports whatever speed the test sets.
 */
@DisplayName("Launcher")
class LauncherTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private final double UPDATE_SECONDS = properties.launcherUpdateMs / 1000;

    /** One pretend motor: the last power it was given, and the speed it reports. */
    private static class FakeMotor {
        final DcMotorEx motor = mock(DcMotorEx.class);
        double power = 0;
        double ticksPerSecond = 0;

        FakeMotor() {
            doAnswer(inv -> {
                power = inv.getArgument(0);
                return null;
            }).when(motor).setPower(anyDouble());
            when(motor.getVelocity()).thenAnswer(inv -> ticksPerSecond);
        }
    }

    private final FakeMotor pollen = new FakeMotor();
    private final FakeMotor nectar = new FakeMotor();
    private Gamepad gamepad;
    private Gamepad driver;
    private Vision vision;
    private HydraOpMode op;
    private Launcher launcher;

    @BeforeEach
    void setUp() {
        // The tests set every number they check, so tuning BotProperties on the robot
        // can't break them.
        properties.pollenLauncherRpm = 1000;
        properties.nectarLauncherRpm = 1000;
        properties.pollenLauncherReversed = false;
        properties.nectarLauncherReversed = true;
        properties.launcherKF = 0.000242;
        properties.launcherKP = 0.003;
        properties.launcherKI = 0;
        // The D-pad tests below set the speed directly. The distance table has its own tests.
        properties.launcherRangeTuning = false;
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgLaunchPollen)).thenReturn(pollen.motor);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgLaunchNectar)).thenReturn(nectar.motor);
        gamepad = mock(Gamepad.class);
        driver = mock(Gamepad.class);
        vision = mock(Vision.class);
        op = new HydraOpMode(mock(Telemetry.class), hardwareMap, driver, gamepad);
        launcher = new Launcher(op, properties, gamepad, vision);
    }

    /** The camera sees one of our tags this many inches away. */
    private void tagAt(double rangeIn) {
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetRange()).thenReturn(rangeIn);
    }

    /** Press a button and hold it long enough for the debouncer, then let go. */
    private void tap(Consumer<Boolean> button) {
        for (int loop = 0; loop < properties.debounce + 2; loop++) {
            button.accept(true);
            launcher.HandleUserInput();
        }
        button.accept(false);
        launcher.HandleUserInput();
    }

    private void spinsAt(FakeMotor wheel, double rpm) {
        wheel.ticksPerSecond = rpm / 60 * properties.launcherTicksPerRev;
    }

    @Test
    @DisplayName("motors are set up like Decode: our code picks power, wheel coasts when off")
    void motorSetUp() {
        verify(nectar.motor).setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        verify(nectar.motor).setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        verify(pollen.motor).setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    @Test
    @DisplayName("each launcher's direction follows its reversed setting: false FORWARD, true REVERSE")
    void directions() {
        verify(pollen.motor).setDirection(DcMotor.Direction.FORWARD);
        verify(nectar.motor).setDirection(DcMotor.Direction.REVERSE);
    }

    @Test
    @DisplayName("flipping nectarLauncherReversed flips the nectar motor")
    void nectarFlip() {
        properties.nectarLauncherReversed = false;
        FakeMotor other = new FakeMotor();
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(DcMotorEx.class, properties.cfgLaunchNectar)).thenReturn(other.motor);
        new Launcher(new HydraOpMode(mock(Telemetry.class), hardwareMap, mock(Gamepad.class), gamepad),
                properties, gamepad, vision);
        verify(other.motor).setDirection(DcMotor.Direction.FORWARD);
    }

    @Test
    @DisplayName("both targets start at the BotProperties speeds")
    void startsAt1000() {
        assertEquals(1000, launcher.GetPollenTargetRpm(), EPS);
        assertEquals(1000, launcher.GetNectarTargetRpm(), EPS);
    }

    @Test
    @DisplayName("D-pad up/down changes pollen only; right/left changes nectar only")
    void eachLauncherHasItsOwnSpeed() {
        tap(down -> gamepad.dpad_up = down);
        tap(down -> gamepad.dpad_up = down);
        assertEquals(1100, launcher.GetPollenTargetRpm(), EPS);
        assertEquals(1000, launcher.GetNectarTargetRpm(), EPS);

        tap(down -> gamepad.dpad_left = down);
        assertEquals(950, launcher.GetNectarTargetRpm(), EPS);
        assertEquals(1100, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("the D-pad writes the speed into BotProperties, where the dashboard sees it")
    void dpadChangesTheSetting() {
        tap(down -> gamepad.dpad_up = down);
        assertEquals(1050, properties.pollenLauncherRpm, EPS);
    }

    @Test
    @DisplayName("a speed changed in BotProperties (the dashboard) reaches the launcher")
    void settingChangeReachesLauncher() {
        properties.nectarLauncherRpm = 2500;
        assertEquals(2500, launcher.GetNectarTargetRpm(), EPS);
    }

    @Test
    @DisplayName("distance table: the band rule, 10 inches a row, clamped at both ends")
    void bandRule() {
        double[][] table = {{110, 2900}, {120, 3030}, {130, 3170}, {140, 3300}};
        assertEquals(2900, Launcher.RpmForRange(100, table), EPS);   // nearer than the first row
        assertEquals(2900, Launcher.RpmForRange(110, table), EPS);
        assertEquals(2900, Launcher.RpmForRange(119.9, table), EPS);
        assertEquals(3030, Launcher.RpmForRange(120, table), EPS);
        assertEquals(3170, Launcher.RpmForRange(135, table), EPS);
        assertEquals(3300, Launcher.RpmForRange(140, table), EPS);
        assertEquals(3300, Launcher.RpmForRange(200, table), EPS);   // farther than the last row
    }

    @Test
    @DisplayName("tag in view: the pollen target follows the table every loop; nectar is left alone")
    void followsTheTable() {
        properties.launcherRangeTuning = true;
        tagAt(135);
        launcher.Process();
        assertEquals(3170, launcher.GetPollenTargetRpm(), EPS);
        assertEquals(1000, launcher.GetNectarTargetRpm(), EPS);
        tagAt(150);
        launcher.Process();
        assertEquals(3300, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("no tag in view: the target holds where it was, so a blind shot still has a speed")
    void noTagHolds() {
        properties.launcherRangeTuning = true;
        tagAt(112);
        launcher.Process();
        assertEquals(2900, launcher.GetPollenTargetRpm(), EPS);
        when(vision.SeesTag()).thenReturn(false);
        launcher.Process();
        assertEquals(2900, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("range tuning on: D-pad up / down trims the whole table, 50 a press")
    void dpadTrimsTheTable() {
        properties.launcherRangeTuning = true;
        driver.right_bumper = true;   // TeleOp: the table only follows while aiming
        tagAt(125);
        tap(down -> gamepad.dpad_up = down);
        tap(down -> gamepad.dpad_up = down);
        launcher.Process();
        assertEquals(100, properties.launcherRangeTrimRpm, EPS);
        assertEquals(3030 + 100, launcher.GetPollenTargetRpm(), EPS);
        tap(down -> gamepad.dpad_down = down);
        launcher.Process();
        assertEquals(3030 + 50, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("TeleOp, no aim button: the camera is not tracking, the speed holds")
    void teleOpTracksOnlyWhileAiming() {
        properties.launcherRangeTuning = true;
        tagAt(150);
        launcher.HandleUserInput();
        launcher.Process();
        assertEquals(1000, launcher.GetPollenTargetRpm(), EPS);

        driver.right_bumper = true;
        launcher.HandleUserInput();
        launcher.Process();
        assertEquals(3300, launcher.GetPollenTargetRpm(), EPS);

        // Let go: holds where it got to, even as the robot moves.
        driver.right_bumper = false;
        tagAt(112);
        launcher.HandleUserInput();
        launcher.Process();
        assertEquals(3300, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("TeleOp, launch trigger held: tracks too")
    void teleOpTracksWhileLaunching() {
        properties.launcherRangeTuning = true;
        tagAt(125);
        driver.right_trigger = 1;
        launcher.HandleUserInput();
        launcher.Process();
        assertEquals(3030, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("range tuning off: a tag in view changes nothing, and the D-pad sets the speed itself")
    void tuningOffIgnoresTheTag() {
        properties.launcherRangeTuning = false;
        tagAt(150);
        launcher.Process();
        assertEquals(1000, launcher.GetPollenTargetRpm(), EPS);
        tap(down -> gamepad.dpad_up = down);
        assertEquals(1050, launcher.GetPollenTargetRpm(), EPS);
        assertEquals(0, properties.launcherRangeTrimRpm, EPS);
    }

    @Test
    @DisplayName("target never goes below 0 or above the maximum")
    void targetLimits() {
        launcher.ChangePollenRpm(-5000);
        assertEquals(0, launcher.GetPollenTargetRpm(), EPS);
        launcher.ChangeNectarRpm(properties.launcherMaxRpm + 1000);
        assertEquals(properties.launcherMaxRpm, launcher.GetNectarTargetRpm(), EPS);
    }

    @Test
    @DisplayName("not spinning: no power, even below the target")
    void offMeansNoPower() {
        spinsAt(pollen, 0);
        launcher.Update(UPDATE_SECONDS);
        assertEquals(0, pollen.power, EPS);
        assertEquals(0, nectar.power, EPS);
    }

    @Test
    @DisplayName("SetSpinning(true), as START does: both get power from the speed control; false: both off")
    void spinningFromStart() {
        spinsAt(pollen, 900);
        launcher.SetSpinning(true);
        assertTrue(launcher.IsSpinning());
        launcher.Update(UPDATE_SECONDS);
        double expected = properties.launcherKF * 1000 + properties.launcherKP * 100
                + properties.launcherKI * 100 * UPDATE_SECONDS;
        assertEquals(Math.min(expected, 1), pollen.power, EPS);
        assertTrue(nectar.power > 0);

        launcher.SetSpinning(false);
        launcher.Update(UPDATE_SECONDS);
        assertEquals(0, pollen.power, EPS);
        assertEquals(0, nectar.power, EPS);
    }

    @Test
    @DisplayName("no button turns the launchers off: the right bumper does nothing now")
    void noOffButton() {
        launcher.SetSpinning(true);
        tap(down -> gamepad.right_bumper = down);
        assertTrue(launcher.IsSpinning());
        spinsAt(pollen, 900);
        launcher.Update(UPDATE_SECONDS);
        assertTrue(pollen.power > 0);
    }

    @Test
    @DisplayName("a gain changed on the dashboard counts at the next update")
    void liveGains() {
        launcher.SetSpinning(true);
        spinsAt(pollen, 1000);
        properties.launcherKF = 0.0005;
        launcher.Update(UPDATE_SECONDS);
        assertEquals(0.5, pollen.power, EPS);
    }

    @Test
    @DisplayName("RPM: 28 ticks per second on a 28-tick motor is 60 RPM")
    void rpmMath() {
        pollen.ticksPerSecond = properties.launcherTicksPerRev;
        launcher.Update(UPDATE_SECONDS);
        assertEquals(60, launcher.GetPollenMeasuredRpm(), EPS);
    }

    @Test
    @DisplayName("at speed: both spinning within 50 RPM of their targets")
    void atSpeed() {
        launcher.SetSpinning(true);
        spinsAt(pollen, 1040);
        spinsAt(nectar, 960);
        launcher.Update(UPDATE_SECONDS);
        assertTrue(launcher.AtSpeed());
        spinsAt(nectar, 900);
        launcher.Update(UPDATE_SECONDS);
        assertFalse(launcher.AtSpeed());
    }

    @Test
    @DisplayName("a robot with no launcher motors still runs")
    void missingMotors() {
        HydraOpMode empty = new HydraOpMode(mock(Telemetry.class), mock(HardwareMap.class),
                mock(Gamepad.class), gamepad);
        Launcher nothing = new Launcher(empty, properties, gamepad, null);
        nothing.SetSpinning(true);
        nothing.Init();
        nothing.Process();
        nothing.Update(UPDATE_SECONDS);
        assertFalse(nothing.AtSpeed());
    }

    @Test
    @DisplayName("one launcher missing from the config: the other alone decides at speed")
    void missingWheelDoesNotHoldUpALaunch() {
        HardwareMap onlyPollen = mock(HardwareMap.class);
        when(onlyPollen.tryGet(DcMotorEx.class, properties.cfgLaunchPollen)).thenReturn(pollen.motor);
        Launcher half = new Launcher(new HydraOpMode(mock(Telemetry.class), onlyPollen,
                mock(Gamepad.class), gamepad), properties, gamepad, vision);
        half.SetSpinning(true);
        spinsAt(pollen, 1000);
        half.Update(UPDATE_SECONDS);
        assertTrue(half.AtSpeed());
    }
}
