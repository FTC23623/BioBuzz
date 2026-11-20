package org.firstinspires.ftc.teamcode.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Snake on a strip, played by itself. */
@DisplayName("StripSnake")
class StripSnakeTest {

    /** true when the LED is on the snake's body, tail to head, wrapping. */
    private static boolean OnSnake(StripSnake s, int led) {
        for (int i = 0; i < s.Length(); i++) {
            if ((s.Tail() + i) % s.Leds() == led) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("starts at the beginning of the strip, the start length, food somewhere else")
    void startsShort() {
        StripSnake s = new StripSnake(36, 3, new Random(1));
        assertEquals(0, s.Tail());
        assertEquals(2, s.Head());
        assertEquals(3, s.Length());
        assertFalse(OnSnake(s, s.Food()));
    }

    @Test
    @DisplayName("a step without food: one LED along, same length")
    void stepsAlong() {
        StripSnake s = new StripSnake(36, 3, new Random(1));
        // Walk until the next step won't land on the food.
        while ((s.Head() + 1) % s.Leds() == s.Food()) {
            s.Step();
        }
        int head = s.Head();
        int length = s.Length();
        assertFalse(s.Step());
        assertEquals((head + 1) % 36, s.Head());
        assertEquals(length, s.Length());
    }

    @Test
    @DisplayName("eating: one longer, the tail stays put, and new food not on the snake")
    void eatingGrows() {
        StripSnake s = new StripSnake(36, 3, new Random(7));
        while ((s.Head() + 1) % s.Leds() != s.Food()) {
            s.Step();
        }
        int tail = s.Tail();
        assertTrue(s.Step());
        assertEquals(4, s.Length());
        assertEquals(tail, s.Tail());
        assertFalse(OnSnake(s, s.Food()));
    }

    @Test
    @DisplayName("past the last LED it wraps to the first, drawn as two runs")
    void wraps() {
        StripSnake s = new StripSnake(10, 3, new Random(3));
        // Play until the body crosses the end of the strip.
        int steps = 0;
        while (!s.Wraps() && steps < 1000) {
            s.Step();
            steps++;
        }
        assertTrue(s.Wraps());
        assertTrue(s.Tail() > s.Head());
    }

    @Test
    @DisplayName("played a long time: the food is never on the snake, and it starts over once the strip is full")
    void playsForever() {
        StripSnake s = new StripSnake(12, 3, new Random(42));
        int longest = 0;
        boolean startedOver = false;
        for (int step = 0; step < 20000; step++) {
            int before = s.Length();
            s.Step();
            assertFalse(OnSnake(s, s.Food()), "food on the snake at step " + step);
            assertTrue(s.Length() >= 3 && s.Length() < 12, "length " + s.Length());
            longest = Math.max(longest, s.Length());
            if (s.Length() < before) {
                startedOver = true;
            }
        }
        assertEquals(11, longest);
        assertTrue(startedOver);
    }
}
