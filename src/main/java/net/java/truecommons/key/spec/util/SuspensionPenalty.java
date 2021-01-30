/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec.util;

import javax.annotation.concurrent.Immutable;

/**
 * A utility class for enforcing a suspension penalty of at least
 * {@link #MIN_KEY_RETRY_DELAY} milliseconds between two subsequent key
 * verifications.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Immutable
public class SuspensionPenalty {

    /**
     * The minimum delay between subsequent attempts to verify a key,
     * which is {@value} milliseconds.
     */
    public static final int MIN_KEY_RETRY_DELAY = 3 * 1000;

    private SuspensionPenalty() { }

    /**
     * Call this method in a key verification loop in order to enforce a
     * suspension penalty for providing a wrong key of at least
     * {@value #MIN_KEY_RETRY_DELAY} milliseconds.
     * Interrupting the current thread does not show any effect on this method.
     *
     * @param  last the last try time.
     *         This should be zero upon the first call.
     *         Subsequent calls should provide the return value of the last
     *         call.
     * @return The new try time.
     */
    public static long enforce(final long last) {
        final long start = System.currentTimeMillis();
        final long elapsed = start - last;
        final long delay = MIN_KEY_RETRY_DELAY - elapsed;
        if (0 < delay) {
            pause(delay);
            return start + delay; // ~ System.currentTimeMillis()
        } else {
            return start; // ~ System.currentTimeMillis()
        }
    }

    /**
     * Uninterruptibly pauses the current thread for the given time interval.
     *
     * @param  millis the milliseconds to pause the current thread.
     * @throws IllegalArgumentException if {@code millis} is negative.
     */
    @SuppressWarnings("SleepWhileInLoop")
    private static void pause(long millis) {
        final long start = System.currentTimeMillis();
        boolean interrupted = false;
        do {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupt) {
                interrupted = true;
            }
        } while (0 < (millis -= System.currentTimeMillis() - start));
        if (interrupted) Thread.currentThread().interrupt(); // restore
    }
}
