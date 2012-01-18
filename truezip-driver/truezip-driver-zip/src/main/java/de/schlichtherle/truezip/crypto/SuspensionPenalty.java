/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto;

/**
 * A utility class for enforcing a suspension penalty of at least
 * {@link #MIN_KEY_RETRY_DELAY} milliseconds between two subsequent key
 * verifications.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class SuspensionPenalty {

    /* can't touch this - hammer time! */
    private SuspensionPenalty() {
    }

    /**
     * The minimum delay between subsequent attempts to verify a key
     * in milliseconds.
     */
    public static final int MIN_KEY_RETRY_DELAY = 3 * 1000;

    /**
     * Call this method in a key verification loop in order to enforce a
     * suspension penalty for providing a wrong key of at least
     * {@link #MIN_KEY_RETRY_DELAY} milliseconds.
     * 
     * @param  last the last try time.
     *         This should be zero upon the first call.
     *         Subsequent calls should provide the return value of the last
     *         call.
     * @return The new try time.
     */
    @SuppressWarnings("SleepWhileInLoop")
    public static long enforce(final long last) {
        long delay;
        boolean interrupted = false;
        while ((delay = System.currentTimeMillis() - last) < MIN_KEY_RETRY_DELAY) {
            try {
                Thread.sleep(MIN_KEY_RETRY_DELAY - delay);
            } catch (InterruptedException ex) {
                interrupted = true;
            }
        }
        if (interrupted)
            Thread.currentThread().interrupt();
        return last + delay; // approximately System.currentTimeMillis()
    }
}
