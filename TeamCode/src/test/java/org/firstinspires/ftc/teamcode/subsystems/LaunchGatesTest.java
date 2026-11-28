package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.firstinspires.ftc.teamcode.objects.HydraOpMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The launch gates with pretend servos, a pretend driver gamepad and a pretend operator
 * gamepad. The operator's triggers launch: left pollen, right nectar. Unless a test says
 * otherwise, the pretend camera is locked on the hive: a tag in view, dead ahead.
 */
@DisplayName("LaunchGates")
class LaunchGatesTest {
    private final BotProperties properties = new BotProperties();
    private Servo pollen;
    private Servo nectar;
    private Gamepad gamepad;
    private Gamepad operator;
    private Telemetry telemetry;
    private Vision vision;
    private LaunchGates gates;

    @BeforeEach
    void setUp() {
        // These tests check the launcher geometry; the nectar-only turn is tested in LaunchChoiceTest.
        properties.nectarAimLeftDeg = 0;
        pollen = mock(Servo.class);
        nectar = mock(Servo.class);
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(Servo.class, properties.cfgPollenServo)).thenReturn(pollen);
        when(hardwareMap.tryGet(Servo.class, properties.cfgNectarServo)).thenReturn(nectar);
        gamepad = mock(Gamepad.class);
        operator = mock(Gamepad.class);
        telemetry = mock(Telemetry.class);
        vision = mock(Vision.class);
        lockedOnTheHive();
        // The lock is off by default; these tests check it working, so switch it on.
        properties.launchNeedsLock = true;
        // Dead-center locks here; the side and launcher offsets have their own tests.
        properties.aimHiveBearingDeg = 0;
        properties.launcherSideOffsetIn = 0;
        properties.launcherOffsetNoRangeDeg = 0;
        HydraOpMode op = new HydraOpMode(telemetry, hardwareMap, gamepad, operator);
        // The Bot builds the gates with the operator's gamepad.
        gates = new LaunchGates(op, properties, operator, vision);
    }

    /** The camera sees one of our tags, with the hive's middle dead ahead. */
    private void lockedOnTheHive() {
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetGroupBearing()).thenReturn(0.0);
    }

    private void noTag() {
        when(vision.SeesTag()).thenReturn(false);
    }

    private void loop() {
        gates.HandleUserInput();
        gates.Process();
    }

    @Test
    @DisplayName("INIT resets both gates to closed, once, however many INIT loops run")
    void closedAtInit() {
        gates.Init();
        gates.Init();
        gates.Init();
        verify(pollen, times(1)).setPosition(properties.pollenServoClosed);
        verify(nectar, times(1)).setPosition(properties.nectarServoClosed);
        verify(pollen, never()).setPosition(properties.pollenServoOpen);
        verify(nectar, never()).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("launchGatesCloseAtInit off: nothing moves during INIT, as before")
    void stillAtInitWhenOff() {
        properties.launchGatesCloseAtInit = false;
        gates.Init();
        verify(pollen, never()).setPosition(anyDouble());
        verify(nectar, never()).setPosition(anyDouble());
    }

    @Test
    @DisplayName("no trigger: both closed")
    void closedByDefault() {
        loop();
        verify(pollen).setPosition(properties.pollenServoClosed);
        verify(nectar).setPosition(properties.nectarServoClosed);
    }

    @Test
    @DisplayName("operator's left trigger: ONLY the pollen gate opens, nectar stays closed")
    void leftTriggerOpensPollen() {
        operator.left_trigger = 1;
        loop();
        assertTrue(gates.IsOpen());
        assertTrue(gates.IsPollenOpen());
        assertFalse(gates.IsNectarOpen());
        verify(pollen).setPosition(properties.pollenServoOpen);
        verify(nectar).setPosition(properties.nectarServoClosed);
        verify(nectar, never()).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("operator's right trigger: ONLY the nectar gate opens, pollen stays closed")
    void rightTriggerOpensNectar() {
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsOpen());
        assertTrue(gates.IsNectarOpen());
        assertFalse(gates.IsPollenOpen());
        verify(nectar).setPosition(properties.nectarServoOpen);
        verify(pollen).setPosition(properties.pollenServoClosed);
        verify(pollen, never()).setPosition(properties.pollenServoOpen);
    }

    @Test
    @DisplayName("both triggers: both gates open together")
    void bothTriggersBoth() {
        operator.left_trigger = 1;
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsPollenOpen());
        assertTrue(gates.IsNectarOpen());
        verify(pollen).setPosition(properties.pollenServoOpen);
        verify(nectar).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("launchBothTogether off: both triggers, pollen wins, only the pollen gate opens")
    void bothTriggersOnlyPollenWhenOff() {
        properties.launchBothTogether = false;
        operator.left_trigger = 1;
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsPollenOpen());
        assertFalse(gates.IsNectarOpen());
        verify(nectar, never()).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("both held, one let go: the gate still wanted stays open and nothing restarts; the other shuts")
    void bothThenOneReleased() {
        operator.left_trigger = 1;
        operator.right_trigger = 1;
        loop();
        sleepSeconds(IsOpenTime());
        loop();
        assertTrue(gates.IsFullyOpen());
        int launches = gates.LaunchCount();
        operator.left_trigger = 0;
        loop();
        assertFalse(gates.IsPollenOpen());
        assertTrue(gates.IsNectarOpen());
        assertTrue(gates.IsFullyOpen(), "nectar was already open: no new wait");
        assertEquals(launches, gates.LaunchCount());
    }

    @Test
    @DisplayName("one held, the other added: the new gate opens with its own wait; the first stays open")
    void oneThenBoth() {
        operator.right_trigger = 1;
        loop();
        sleepSeconds(IsOpenTime());
        loop();
        assertTrue(gates.IsFullyOpen());
        operator.left_trigger = 1;
        loop();
        assertTrue(gates.IsPollenOpen());
        assertTrue(gates.IsNectarOpen());
        assertFalse(gates.IsFullyOpen(), "the pollen gate has only just started to open");
        sleepSeconds(IsOpenTime());
        loop();
        assertTrue(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("the driver's triggers do nothing to the gates now")
    void driverTriggersDoNothing() {
        gamepad.left_trigger = 1;
        gamepad.right_trigger = 1;
        loop();
        assertFalse(gates.IsOpen());
        verify(pollen, never()).setPosition(properties.pollenServoOpen);
        verify(nectar, never()).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("hold to open: letting go closes it right away, no second press")
    void releaseCloses() {
        operator.left_trigger = 1;
        loop();
        operator.left_trigger = 0;
        loop();
        assertFalse(gates.IsOpen());
        verify(pollen).setPosition(properties.pollenServoClosed);
    }

    @Test
    @DisplayName("a trigger barely touched does not launch")
    void deadband() {
        operator.left_trigger = (float) (properties.trgBtnThresh / 2);
        operator.right_trigger = (float) (properties.trgBtnThresh / 2);
        loop();
        assertFalse(gates.IsOpen());
    }

    @Test
    @DisplayName("an auto opens BOTH with SetOpen")
    void autoOpens() {
        gates.SetOpen(true);
        gates.Process();
        assertTrue(gates.IsPollenOpen());
        assertTrue(gates.IsNectarOpen());
        verify(pollen).setPosition(properties.pollenServoOpen);
        verify(nectar).setPosition(properties.nectarServoOpen);
    }

    @Test
    @DisplayName("open time: 80 degrees at 0.25 s per 60, plus 0.05 extra, is 0.38 s")
    void openSecondsMath() {
        properties.pollenServoClosed = 0.267;
        properties.pollenServoOpen = 0;
        properties.nectarServoClosed = 0;
        properties.nectarServoOpen = 0.2;
        properties.launchServoRangeDeg = 300;
        properties.launchServoSecondsPer60Deg = 0.25;
        properties.launchServoExtraSeconds = 0.05;
        // The slower one wins: pollen turns 80.1 degrees, nectar only 60.
        double expected = 0.267 * 300 / 60 * 0.25 + 0.05;
        assertEquals(expected, LaunchGates.OpenSeconds(properties), 1e-9);
    }

    @Test
    @DisplayName("just opened: not fully open yet, so nothing is pushed into it")
    void notFullyOpenAtFirst() {
        operator.left_trigger = 1;
        loop();
        assertTrue(gates.IsOpen());
        assertFalse(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("once the open time has passed: fully open")
    void fullyOpenAfterTheWait() {
        properties.launchServoSecondsPer60Deg = 0;
        properties.launchServoExtraSeconds = 0;
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("trigger pulled: the camera's distance to the tag at that moment is remembered and shown")
    void remembersLaunchRange() {
        when(vision.SeesTag()).thenReturn(true);
        when(vision.GetRange()).thenReturn(62.0);
        when(vision.GetTagId()).thenReturn(31);
        when(vision.GetGroupBearing()).thenReturn(-0.5);   // within aimDoneDegrees, so locked
        operator.left_trigger = 1;
        loop();
        // The camera moves on; the launch keeps the number it had.
        when(vision.GetRange()).thenReturn(99.0);
        loop();
        assertEquals(1, gates.LaunchCount());
        assertEquals(62.0, gates.LastLaunchRangeIn(), 1e-9);
        verify(telemetry, atLeast(1)).addData(eq("Last launch (camera)"), contains("#1: 62 in from tag 31, hive middle 0.5 deg to the right"));
    }

    // ---- The camera lock (0040) ----

    @Test
    @DisplayName("no tag in view: the trigger does nothing, no launch is counted, the operator's gamepad buzzes once")
    void noLockKeepsThemShut() {
        noTag();
        operator.left_trigger = 1;
        loop();
        loop();
        loop();
        assertFalse(gates.IsOpen());
        assertEquals(0, gates.LaunchCount());
        verify(pollen, never()).setPosition(properties.pollenServoOpen);
        verify(operator, times(1)).rumble(properties.launchNoLockRumbleMs);
        verify(gamepad, never()).rumble(anyInt());
        verify(telemetry, atLeast(1)).addData(contains("Launch gates"), contains("NO LOCK"));
    }

    @Test
    @DisplayName("let go and pull again with no tag: it buzzes again")
    void buzzesOncePerPull() {
        noTag();
        operator.left_trigger = 1;
        loop();
        operator.left_trigger = 0;
        loop();
        operator.left_trigger = 1;
        loop();
        verify(operator, times(2)).rumble(properties.launchNoLockRumbleMs);
    }

    @Test
    @DisplayName("the tag shows up while the trigger is held: the gate opens then, no second pull needed")
    void lockArrivingOpens() {
        noTag();
        operator.left_trigger = 1;
        loop();
        assertFalse(gates.IsOpen());
        lockedOnTheHive();
        loop();
        assertTrue(gates.IsOpen());
        assertEquals(1, gates.LaunchCount());
    }

    @Test
    @DisplayName("hive in view but off to one side: shut while the trigger turns the robot, no buzz; open once centered")
    void offCenterWaitsForTheAim() {
        when(vision.GetGroupBearing()).thenReturn(5.0);
        operator.left_trigger = 1;
        loop();
        assertFalse(gates.IsOpen());
        verify(operator, never()).rumble(anyInt());
        verify(telemetry, atLeast(1)).addData(contains("Launch gates"), contains("turning to the hive"));
        when(vision.GetGroupBearing()).thenReturn(0.5);   // within aimDoneDegrees (1)
        loop();
        assertTrue(gates.IsOpen());
    }

    @Test
    @DisplayName("no override any more: lock on and no tag, both triggers held, the gates stay shut")
    void noOverrideStaysShut() {
        noTag();
        operator.left_trigger = 1;
        operator.right_trigger = 1;
        gamepad.right_trigger = 1;   // the old driver + operator override combination
        loop();
        loop();
        assertFalse(gates.IsOpen());
        assertEquals(0, gates.LaunchCount());
        verify(pollen, never()).setPosition(properties.pollenServoOpen);
        verify(nectar, never()).setPosition(properties.nectarServoOpen);
        verify(operator, times(1)).rumble(properties.launchNoLockRumbleMs);
    }

    @Test
    @DisplayName("no override any more: lock on and no tag, the nectar trigger stays shut too")
    void noOverrideNectarStaysShut() {
        noTag();
        operator.right_trigger = 1;
        loop();
        assertFalse(gates.IsNectarOpen());
        verify(nectar, never()).setPosition(properties.nectarServoOpen);
        verify(operator, times(1)).rumble(properties.launchNoLockRumbleMs);
    }

    @Test
    @DisplayName("launchNeedsLock off: the trigger opens the gate whatever the camera sees, as before")
    void lockSwitchedOff() {
        properties.launchNeedsLock = false;
        noTag();
        operator.left_trigger = 1;
        loop();
        assertTrue(gates.IsOpen());
        verify(operator, never()).rumble(anyInt());
        verify(telemetry, atLeast(1)).addData(eq("Last launch (camera)"), contains("#1: no tag in view"));
    }

    @Test
    @DisplayName("no camera at all (null): with the lock on nothing opens; with it off the trigger opens")
    void noCameraNeedsTheLockOff() {
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(Servo.class, properties.cfgPollenServo)).thenReturn(pollen);
        LaunchGates blind = new LaunchGates(new HydraOpMode(telemetry, hardwareMap, gamepad, operator),
                properties, operator, null);
        operator.left_trigger = 1;
        blind.HandleUserInput();
        blind.Process();
        assertFalse(blind.IsOpen());
        properties.launchNeedsLock = false;
        blind.HandleUserInput();
        blind.Process();
        assertTrue(blind.IsOpen());
    }

    @Test
    @DisplayName("an auto's SetOpen is not checked against the camera")
    void autoIgnoresTheLock() {
        noTag();
        gates.SetOpen(true);
        gates.Process();
        assertTrue(gates.IsOpen());
        verify(pollen).setPosition(properties.pollenServoOpen);
        verify(operator, never()).rumble(anyInt());
    }

    @Test
    @DisplayName("holding the trigger is one launch; pull again for the next")
    void oneLaunchPerPull() {
        operator.left_trigger = 1;
        loop();
        loop();
        loop();
        assertEquals(1, gates.LaunchCount());
        operator.left_trigger = 0;
        loop();
        operator.left_trigger = 1;
        loop();
        assertEquals(2, gates.LaunchCount());
    }

    @Test
    @DisplayName("switching from pollen to nectar mid-pull is a new opening: the timer restarts, counted again")
    void switchingLaunchersRestarts() {
        operator.left_trigger = 1;
        loop();
        assertEquals(1, gates.LaunchCount());
        operator.left_trigger = 0;
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsNectarOpen());
        assertFalse(gates.IsPollenOpen());
        assertFalse(gates.IsFullyOpen());
        assertEquals(2, gates.LaunchCount());
    }

    @Test
    @DisplayName("launchBothTogether off: both held, pollen let go: nectar opens and waits its own opening time")
    void bothHeldThenPollenReleasedWhenOff() {
        properties.launchBothTogether = false;
        operator.left_trigger = 1;
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsPollenOpen());
        assertFalse(gates.IsNectarOpen());
        sleepSeconds(IsOpenTime());
        loop();
        assertTrue(gates.IsFullyOpen());
        operator.left_trigger = 0;
        loop();
        assertFalse(gates.IsPollenOpen());
        assertTrue(gates.IsNectarOpen());
        assertFalse(gates.IsFullyOpen(), "the nectar gate has only just started to open");
        sleepSeconds(IsOpenTime());
        loop();
        assertTrue(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("nectar launching, pollen pulled alone over it: the pollen gate waits its opening time")
    void nectarThenPollenAlone() {
        operator.right_trigger = 1;
        loop();
        sleepSeconds(IsOpenTime());
        loop();
        assertTrue(gates.IsFullyOpen());
        operator.right_trigger = 0;
        operator.left_trigger = 1;
        loop();
        assertTrue(gates.IsPollenOpen());
        assertFalse(gates.IsNectarOpen());
        assertFalse(gates.IsFullyOpen());
    }

    /** A little longer than the gate's opening time, in seconds. */
    private double IsOpenTime() {
        return LaunchGates.OpenSeconds(properties) + 0.05;
    }

    private static void sleepSeconds(double seconds) {
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("let go and pull again: the wait starts over")
    void waitStartsOver() {
        operator.left_trigger = 1;
        loop();
        operator.left_trigger = 0;
        loop();
        assertFalse(gates.IsFullyOpen());
        operator.left_trigger = 1;
        loop();
        assertFalse(gates.IsFullyOpen());
    }

    @Test
    @DisplayName("quiet telemetry: the gates still open, but print nothing")
    void quietPrintsNothing() {
        HardwareMap hardwareMap = mock(HardwareMap.class);
        when(hardwareMap.tryGet(Servo.class, properties.cfgPollenServo)).thenReturn(pollen);
        Telemetry quietScreen = mock(Telemetry.class);
        HydraOpMode quiet = new HydraOpMode(quietScreen, hardwareMap, gamepad, operator);
        quiet.mQuiet = true;
        LaunchGates g = new LaunchGates(quiet, properties, operator, vision);
        operator.left_trigger = 1;
        g.HandleUserInput();
        g.Process();
        assertTrue(g.IsOpen());
        verify(quietScreen, never()).addData(anyString(), any());
    }

    @Test
    @DisplayName("by default the lock is off: the trigger launches with no tag in view")
    void lockOffByDefault() {
        assertFalse(new BotProperties().launchNeedsLock);
    }

    @Test
    @DisplayName("left of the hive: locked when the hive sits 1 deg left, not when it is 1 deg right")
    void lockUsesThePollenOffset() {
        properties.aimHiveBearingDeg = 1;
        properties.aimDoneDegrees = 0.5;
        when(vision.GetSideDegrees()).thenReturn(20.0);
        when(vision.GetGroupBearing()).thenReturn(1.2);
        assertTrue(gates.CameraLocked());
        when(vision.GetGroupBearing()).thenReturn(-1.0);
        assertFalse(gates.CameraLocked());
    }

    @Test
    @DisplayName("right of the hive: the offset flips, locked when the hive sits 1 deg right")
    void lockOffsetFlipsOnTheRight() {
        properties.aimHiveBearingDeg = 1;
        properties.aimDoneDegrees = 0.5;
        when(vision.GetSideDegrees()).thenReturn(-20.0);
        when(vision.GetGroupBearing()).thenReturn(-1.2);
        assertTrue(gates.CameraLocked());
        when(vision.GetGroupBearing()).thenReturn(1.0);
        assertFalse(gates.CameraLocked());
    }

    @Test
    @DisplayName("nectar's lock uses the opposite launcher offset: it opens with the hive 1 deg right, not left")
    void nectarLockUsesTheOppositeOffset() {
        properties.launcherOffsetNoRangeDeg = 1;   // no range here: a round 1 degree
        properties.aimDoneDegrees = 0.5;
        when(vision.GetGroupBearing()).thenReturn(1.2);   // pollen's spot: wrong for nectar
        operator.right_trigger = 1;
        loop();
        assertFalse(gates.IsNectarOpen());
        when(vision.GetGroupBearing()).thenReturn(-1.2);  // nectar's spot
        loop();
        assertTrue(gates.IsNectarOpen());
    }

    // ---- Settle before launch (0056): pollen, and nectar too from 2026-11-25 ----

    @Test
    @DisplayName("settle on: the pollen gate waits for the launcher to settle, then opens")
    void pollenWaitsForTheSettle() {
        properties.teleopLaunchSettles = true;
        Launcher launcher = mock(Launcher.class);
        when(launcher.SettleStatus()).thenReturn("settling");
        gates.SetLauncher(launcher);
        operator.left_trigger = 1;
        loop();
        assertFalse(gates.IsPollenOpen());
        verify(launcher, atLeastOnce()).BeginSettle();
        when(launcher.SettleDone()).thenReturn(true);
        loop();
        assertTrue(gates.IsPollenOpen());
    }

    @Test
    @DisplayName("settle on: the nectar gate waits for the NECTAR wheel to settle, then opens (a ball is already at the gate)")
    void nectarWaitsForItsSettle() {
        properties.teleopLaunchSettles = true;
        Launcher launcher = mock(Launcher.class);
        when(launcher.NectarSettleStatus()).thenReturn("settling");
        gates.SetLauncher(launcher);
        operator.right_trigger = 1;
        loop();
        assertFalse(gates.IsNectarOpen());
        verify(launcher, atLeastOnce()).BeginNectarSettle();
        verify(launcher, never()).BeginSettle();
        when(launcher.NectarSettleDone()).thenReturn(true);
        loop();
        assertTrue(gates.IsNectarOpen());
        assertFalse(gates.IsPollenOpen());
    }

    @Test
    @DisplayName("teleopNectarSettles off: the nectar gate opens at once, as before")
    void nectarSettleOff() {
        properties.teleopLaunchSettles = true;
        properties.teleopNectarSettles = false;
        Launcher launcher = mock(Launcher.class);
        gates.SetLauncher(launcher);
        operator.right_trigger = 1;
        loop();
        assertTrue(gates.IsNectarOpen());
        verify(launcher, never()).BeginNectarSettle();
    }

    @Test
    @DisplayName("both triggers: each gate waits for its own wheel; one launch is counted")
    void bothWaitForTheirOwnWheel() {
        properties.teleopLaunchSettles = true;
        Launcher launcher = mock(Launcher.class);
        gates.SetLauncher(launcher);
        operator.left_trigger = 1;
        operator.right_trigger = 1;
        loop();
        assertFalse(gates.IsPollenOpen());
        assertFalse(gates.IsNectarOpen());
        // Nectar settles first: its gate opens, pollen's stays shut.
        when(launcher.NectarSettleDone()).thenReturn(true);
        loop();
        assertTrue(gates.IsNectarOpen());
        assertFalse(gates.IsPollenOpen());
        assertEquals(1, gates.LaunchCount());
        // Then pollen: its gate opens too, and the opening wait starts again for it.
        when(launcher.SettleDone()).thenReturn(true);
        loop();
        assertTrue(gates.IsNectarOpen());
        assertTrue(gates.IsPollenOpen());
        assertFalse(gates.IsFullyOpen());
        assertEquals(1, gates.LaunchCount());
    }

    @Test
    @DisplayName("nectar is let go while it settles: its settle is cancelled, so the speed unlocks")
    void nectarLetGoCancelsItsSettle() {
        properties.teleopLaunchSettles = true;
        Launcher launcher = mock(Launcher.class);
        gates.SetLauncher(launcher);
        operator.right_trigger = 1;
        loop();
        operator.right_trigger = 0;
        loop();
        verify(launcher, atLeastOnce()).CancelNectarSettle();
        assertFalse(gates.IsNectarOpen());
    }
}
