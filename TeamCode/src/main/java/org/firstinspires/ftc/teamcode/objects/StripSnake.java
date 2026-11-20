package org.firstinspires.ftc.teamcode.objects;

import java.util.Random;

/**
 * Snake on a strip of LEDs, played by itself. Nobody steers: the snake slides one LED along
 * every step, wrapping from the last LED back to the first, and when its head lands on the
 * food it grows one LED longer and new food appears somewhere it isn't. When the snake
 * fills the whole strip, it starts over short.
 *
 * <pre>
 *     10 LEDs, length 3, food on 6:
 *     step 0:   . S S S . . F . . .     tail 1, head 3
 *     step 3:   . . . . S S S . . .     head on 6: ate it, length 4, new food
 *     step 4:   . . . . S S S S . .     the tail stayed put, so it grew
 * </pre>
 *
 * <p>The body is always one unbroken run of LEDs ending at the head, so it can be drawn as
 * one run, or two when it wraps past the end ({@link #Wraps()}). No hardware here, so it
 * can be tested on a laptop.
 */
public class StripSnake {
    private final int mLeds;
    private final int mStartLength;
    private final Random mRandom;
    private int mHead;
    private int mLength;
    private int mFood;

    /**
     * @param leds        how many LEDs the snake lives on, numbered 0 to leds - 1. At least 2.
     * @param startLength how long the snake is at the start and after it fills the strip
     * @param random      where the food goes. Give it a seed in a test to know where.
     */
    public StripSnake(int leds, int startLength, Random random) {
        mLeds = Math.max(2, leds);
        mStartLength = Math.max(1, Math.min(startLength, mLeds - 1));
        mRandom = random;
        Reset();
    }

    /** Back to the start: short, at the beginning of the strip, with new food. */
    public void Reset() {
        mLength = mStartLength;
        mHead = mLength - 1;
        mFood = NewFood();
    }

    /**
     * One step along.
     *
     * @return true when the head landed on the food this step
     */
    public boolean Step() {
        mHead = (mHead + 1) % mLeds;
        if (mHead != mFood) {
            return false;
        }
        // Ate: one longer. The tail doesn't move this step, which is what makes it grow.
        mLength = mLength + 1;
        if (mLength >= mLeds) {
            // The whole strip is snake: nowhere left for food. Start over.
            Reset();
        } else {
            mFood = NewFood();
        }
        return true;
    }

    /**
     * Food on a random LED the snake isn't on. The body runs from the tail up to the head,
     * so the free LEDs are the ones just past the head, before the tail: pick one of those.
     */
    private int NewFood() {
        int free = mLeds - mLength;
        return (mHead + 1 + mRandom.nextInt(free)) % mLeds;
    }

    /** @return the LED the head is on */
    public int Head() {
        return mHead;
    }

    /** @return the LED the tail is on: the far end of the body from the head */
    public int Tail() {
        return ((mHead - mLength + 1) % mLeds + mLeds) % mLeds;
    }

    /** @return how many LEDs long the snake is */
    public int Length() {
        return mLength;
    }

    /** @return the LED the food is on */
    public int Food() {
        return mFood;
    }

    /**
     * @return true when the body runs off the last LED and on from the first, so it takes
     *     two runs to draw: tail to the last LED, then the first LED to the head
     */
    public boolean Wraps() {
        return Tail() > mHead;
    }

    /** @return how many LEDs the snake lives on */
    public int Leds() {
        return mLeds;
    }
}
