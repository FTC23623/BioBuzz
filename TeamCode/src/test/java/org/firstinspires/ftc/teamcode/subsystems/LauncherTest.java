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
        // Steady range (0048) is off unless a test turns it on.
        properties.steadyRange = false;
        properties.steadyRangeAverage = true;
        properties.launchBandHoldIn = 1;
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
    @DisplayName("distance curve: 2-inch steps, each a little bigger than the last, clamped at both ends")
    void curve() {
        BotProperties p = new BotProperties();
        p.launchNearIn = 110;
        p.launchNearRpm = 2900;
        p.launchFarIn = 140;
        p.launchFarRpm = 3325;
        p.launchCurve = 1.1;
        p.launchBandIn = 2;
        assertEquals(2900, Launcher.RpmForRange(100, p), EPS);   // nearer than the near shot
        assertEquals(2900, Launcher.RpmForRange(111.9, p), EPS); // still the first step
        assertEquals(2922, Launcher.RpmForRange(112, p), EPS);
        assertEquals(3027, Launcher.RpmForRange(121, p), EPS);
        assertEquals(3172, Launcher.RpmForRange(130, p), EPS);
        assertEquals(3325, Launcher.RpmForRange(140, p), EPS);
        assertEquals(3325, Launcher.RpmForRange(200, p), EPS);   // farther than the far shot
        // Farther steps are bigger: the curve bends upward, for the ball's arc.
        double nearStep = Launcher.RpmForRange(112, p) - Launcher.RpmForRange(110, p);
        double farStep = Launcher.RpmForRange(138, p) - Launcher.RpmForRange(136, p);
        assertTrue(farStep > nearStep, "near step " + nearStep + ", far step " + farStep);
    }

    @Test
    @DisplayName("curve 1: a straight line between the two shots")
    void curveOneIsStraight() {
        BotProperties p = new BotProperties();
        p.launchCurve = 1;
        p.launchBandIn = 0;   // no steps, so the midpoint is exact
        assertEquals(Math.round((p.launchNearRpm + p.launchFarRpm) / 2),
                Launcher.RpmForRange((p.launchNearIn + p.launchFarIn) / 2, p), EPS);
    }

    @Test
    @DisplayName("tag in view: the pollen target follows the table every loop; nectar is left alone")
    void followsTheTable() {
        properties.launcherRangeTuning = true;
        tagAt(135);
        launcher.Process();
        assertEquals(3172, launcher.GetPollenTargetRpm(), EPS);
        assertEquals(1000, launcher.GetNectarTargetRpm(), EPS);
        tagAt(150);
        launcher.Process();
        assertEquals(3275, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("steady hold: the step only changes once the distance is 1 in past its edge")
    void stepForHolds() {
        // Held on the 120 step (120 to 122), steps of 2, hold 1.
        assertEquals(120, Launcher.StepFor(119.2, 120, properties), EPS);
        assertEquals(120, Launcher.StepFor(122.8, 120, properties), EPS);
        assertEquals(118, Launcher.StepFor(118.9, 120, properties), EPS);
        assertEquals(122, Launcher.StepFor(123.1, 120, properties), EPS);
        // Nothing held yet: the plain step.
        assertEquals(124, Launcher.StepFor(125, Double.NaN, properties), EPS);
        // The ends: the far speed holds just inside 140, the near speed below 110.
        assertEquals(140, Launcher.StepFor(139.5, 140, properties), EPS);
        assertEquals(110, Launcher.StepFor(100, Double.NaN, properties), EPS);
    }

    @Test
    @DisplayName("steady range off (today): the biggest tag's distance and plain steps, as before")
    void steadyOffIsUnchanged() {
        properties.launcherRangeTuning = true;
        tagAt(121);
        when(vision.GetGroupRange()).thenReturn(131.0);
        launcher.Process();
        assertEquals(2942, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("steady range on: the speed comes from the average of our tags")
    void steadyUsesTheGroupRange() {
        properties.launcherRangeTuning = true;
        properties.steadyRange = true;
        properties.launchBandHoldIn = 0;
        tagAt(121);
        when(vision.GetGroupRange()).thenReturn(131.0);
        launcher.Process();
        assertEquals(Launcher.RpmForRange(131, properties), launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("steady range on: a distance wobbling on a step edge keeps one speed")
    void steadyHoldsOnAnEdge() {
        properties.launcherRangeTuning = true;
        properties.steadyRange = true;
        properties.steadyRangeAverage = false;
        tagAt(121.9);
        launcher.Process();
        double onTheEdge = launcher.GetPollenTargetRpm();
        assertEquals(2942, onTheEdge, EPS);
        tagAt(122.3);
        launcher.Process();
        assertEquals(onTheEdge, launcher.GetPollenTargetRpm(), EPS, "122.3 is inside the 1-in hold");
        tagAt(121.8);
        launcher.Process();
        assertEquals(onTheEdge, launcher.GetPollenTargetRpm(), EPS);
        tagAt(123.2);
        launcher.Process();
        assertEquals(Launcher.RpmForRange(122, properties), launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("gamepad 2 cross: steady range on (one rumble), again off (two)")
    void crossTogglesSteady() {
        tap(down -> gamepad.cross = down);
        assertTrue(properties.steadyRange);
        verify(gamepad).rumbleBlips(1);
        tap(down -> gamepad.cross = down);
        assertFalse(properties.steadyRange);
        verify(gamepad).rumbleBlips(2);
    }

    @Test
    @DisplayName("no tag in view: the target holds where it was, so a blind shot still has a speed")
    void noTagHolds() {
        properties.launcherRangeTuning = true;
        tagAt(111);
        launcher.Process();
        assertEquals(2800, launcher.GetPollenTargetRpm(), EPS);
        when(vision.SeesTag()).thenReturn(false);
        launcher.Process();
        assertEquals(2800, launcher.GetPollenTargetRpm(), EPS);
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
        assertEquals(3005 + 100, launcher.GetPollenTargetRpm(), EPS);
        tap(down -> gamepad.dpad_down = down);
        launcher.Process();
        assertEquals(3005 + 50, launcher.GetPollenTargetRpm(), EPS);
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
        assertEquals(3275, launcher.GetPollenTargetRpm(), EPS);

        // Let go: holds where it got to, even as the robot moves.
        driver.right_bumper = false;
        tagAt(112);
        launcher.HandleUserInput();
        launcher.Process();
        assertEquals(3275, launcher.GetPollenTargetRpm(), EPS);
    }

    @Test
    @DisplayName("TeleOp, launch trigger held: tracks too")
    void teleOpTracksWhileLaunching() {
        properties.launcherRangeTuning = true;
        tagAt(125);
        driver.right_trigger = 1;
        launcher.HandleUserInput();
        launcher.Process();
        assertEquals(3005, launcher.GetPollenTargetRpm(), EPS);
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

    @Test
    @DisplayName("launcher-only telemetry: the screen is quiet, but the launchers still show their speeds")
    void launcherOnlyTelemetryShowsTheLaunchers() {
        Telemetry screen = mock(Telemetry.class);
        HydraOpMode quiet = new HydraOpMode(screen, mock(HardwareMap.class), driver, gamepad);
        quiet.mQuiet = true;
        properties.launcherOnlyTelemetry = true;
        Launcher l = new Launcher(quiet, properties, gamepad, vision);
        l.Process();
        verify(screen).addData(org.mockito.ArgumentMatchers.eq("Launcher speed from distance"),
                org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    @DisplayName("quiet telemetry alone: the launchers print nothing")
    void quietHidesTheLaunchers() {
        Telemetry screen = mock(Telemetry.class);
        HydraOpMode quiet = new HydraOpMode(screen, mock(HardwareMap.class), driver, gamepad);
        quiet.mQuiet = true;
        properties.launcherOnlyTelemetry = false;
        Launcher l = new Launcher(quiet, properties, gamepad, vision);
        l.Process();
        org.mockito.Mockito.verifyNoInteractions(screen);
    }
}
