package org.firstinspires.ftc.teamcode.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.qualcomm.robotcore.hardware.Gamepad;
import org.firstinspires.ftc.teamcode.bot.BotProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Which launcher the operator (gamepad 2) is aiming or launching with: left side pollen,
 * right side nectar, pollen wins when both are asked for.
 */
@DisplayName("LaunchChoice")
class LaunchChoiceTest {
    private static final double EPS = 1e-9;
    private final BotProperties properties = new BotProperties();
    private Gamepad operator;

    @BeforeEach
    void setUp() {
        operator = mock(Gamepad.class);
        properties.launchTriggerAims = true;
    }

    /** A trigger pulled all the way. */
    private static final float PULLED = 1;

    @Test
    @DisplayName("nothing pressed: launching and aiming NONE")
    void nothingPressed() {
        assertEquals(LaunchChoice.NONE, LaunchChoice.Launching(operator, properties));
        assertEquals(LaunchChoice.NONE, LaunchChoice.Aiming(operator, properties));
    }

    @Test
    @DisplayName("left trigger launches POLLEN; right trigger launches NECTAR")
    void triggersLaunch() {
        operator.left_trigger = PULLED;
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Launching(operator, properties));
        operator.left_trigger = 0;
        operator.right_trigger = PULLED;
        assertEquals(LaunchChoice.NECTAR, LaunchChoice.Launching(operator, properties));
    }

    @Test
    @DisplayName("a trigger barely touched (under trgBtnThresh) does not launch")
    void triggerDeadband() {
        operator.left_trigger = (float) (properties.trgBtnThresh / 2);
        operator.right_trigger = (float) (properties.trgBtnThresh / 2);
        assertEquals(LaunchChoice.NONE, LaunchChoice.Launching(operator, properties));
    }

    @Test
    @DisplayName("bumpers aim but never launch: left POLLEN, right NECTAR")
    void bumpersAim() {
        operator.left_bumper = true;
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Aiming(operator, properties));
        assertEquals(LaunchChoice.NONE, LaunchChoice.Launching(operator, properties));
        operator.left_bumper = false;
        operator.right_bumper = true;
        assertEquals(LaunchChoice.NECTAR, LaunchChoice.Aiming(operator, properties));
        assertEquals(LaunchChoice.NONE, LaunchChoice.Launching(operator, properties));
    }

    @Test
    @DisplayName("launchTriggerAims on: a launch trigger aims its own launcher too")
    void triggersAim() {
        operator.left_trigger = PULLED;
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Aiming(operator, properties));
        operator.left_trigger = 0;
        operator.right_trigger = PULLED;
        assertEquals(LaunchChoice.NECTAR, LaunchChoice.Aiming(operator, properties));
    }

    @Test
    @DisplayName("launchTriggerAims off: the triggers only launch, they do not aim")
    void triggersDoNotAimWhenOff() {
        properties.launchTriggerAims = false;
        operator.left_trigger = PULLED;
        operator.right_trigger = PULLED;
        assertEquals(LaunchChoice.NONE, LaunchChoice.Aiming(operator, properties));
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Launching(operator, properties));
    }

    @Test
    @DisplayName("both triggers: POLLEN wins")
    void bothTriggersPollenWins() {
        operator.left_trigger = PULLED;
        operator.right_trigger = PULLED;
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Launching(operator, properties));
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Aiming(operator, properties));
    }

    @Test
    @DisplayName("both bumpers, or nectar's bumper with pollen's trigger: POLLEN wins")
    void bothAimsPollenWins() {
        operator.left_bumper = true;
        operator.right_bumper = true;
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Aiming(operator, properties));
        operator.left_bumper = false;
        operator.left_trigger = PULLED;
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Aiming(operator, properties));
    }

    @Test
    @DisplayName("Pick: pollen wins, then nectar, else none")
    void pick() {
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Pick(true, true));
        assertEquals(LaunchChoice.POLLEN, LaunchChoice.Pick(true, false));
        assertEquals(LaunchChoice.NECTAR, LaunchChoice.Pick(false, true));
        assertEquals(LaunchChoice.NONE, LaunchChoice.Pick(false, false));
    }

    @Test
    @DisplayName("launcher offset: atan(2 in / range), pollen left (+), nectar right (-), none counts as pollen")
    void launcherOffsets() {
        properties.launcherSideOffsetIn = 2;
        properties.pollenLeftOfCamera = true;
        assertEquals(1.04, LaunchChoice.POLLEN.LauncherOffsetDeg(110, properties), 0.005);
        assertEquals(0.79, LaunchChoice.POLLEN.LauncherOffsetDeg(145, properties), 0.005);
        assertEquals(-1.04, LaunchChoice.NECTAR.LauncherOffsetDeg(110, properties), 0.005);
        assertEquals(LaunchChoice.POLLEN.LauncherOffsetDeg(120, properties),
                LaunchChoice.NONE.LauncherOffsetDeg(120, properties), EPS);
    }

    @Test
    @DisplayName("launcher offset: no distance uses launcherOffsetNoRangeDeg; pollen on the right flips both")
    void launcherOffsetFallbackAndFlip() {
        properties.launcherOffsetNoRangeDeg = 0.9;
        assertEquals(0.9, LaunchChoice.POLLEN.LauncherOffsetDeg(0, properties), EPS);
        assertEquals(-0.9, LaunchChoice.NECTAR.LauncherOffsetDeg(0, properties), EPS);
        properties.pollenLeftOfCamera = false;
        assertEquals(-0.9, LaunchChoice.POLLEN.LauncherOffsetDeg(0, properties), EPS);
        assertEquals(0.9, LaunchChoice.NECTAR.LauncherOffsetDeg(0, properties), EPS);
    }

    @Test
    @DisplayName("aim spot = side offset (same for both, flips with the side) + launcher offset (never flips)")
    void aimAtAddsBothParts() {
        properties.aimHiveBearingDeg = 1;
        properties.aimSideCenterDeg = 5;
        properties.launcherSideOffsetIn = 2;
        double launcher = Math.toDegrees(Math.atan(2.0 / 110));
        // Robot left of the hive (side +20): side part +1.
        assertEquals(1 + launcher, LaunchChoice.POLLEN.AimAtDeg(20, 110, properties), EPS);
        assertEquals(1 - launcher, LaunchChoice.NECTAR.AimAtDeg(20, 110, properties), EPS);
        // Robot right of the hive: side part -1, launcher part unchanged.
        assertEquals(-1 + launcher, LaunchChoice.POLLEN.AimAtDeg(-20, 110, properties), EPS);
        assertEquals(-1 - launcher, LaunchChoice.NECTAR.AimAtDeg(-20, 110, properties), EPS);
        // Straight in front: the launcher part only.
        assertEquals(launcher, LaunchChoice.POLLEN.AimAtDeg(0, 110, properties), EPS);
    }

    @Test
    @DisplayName("today's numbers: side 1.0, launchers 2 in apart from the camera, pollen on the left, aim done within 0.5")
    void todaysAimNumbers() {
        BotProperties today = new BotProperties();
        assertEquals(1.0, today.aimHiveBearingDeg, EPS);
        assertEquals(2.0, today.launcherSideOffsetIn, EPS);
        assertEquals(0.9, today.launcherOffsetNoRangeDeg, EPS);
        assertEquals(true, today.pollenLeftOfCamera);
        assertEquals(0.5, today.aimDoneDegrees, EPS);
    }
}
