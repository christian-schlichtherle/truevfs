/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    @SuppressWarnings("SleepWhileHoldingLock")
    public static long enforce(final long last) {
        long delay;
        InterruptedException interrupted = null;
        while ((delay = System.currentTimeMillis() - last) < MIN_KEY_RETRY_DELAY) {
            try {
                Thread.sleep(MIN_KEY_RETRY_DELAY - delay);
            } catch (InterruptedException ex) {
                interrupted = ex;
            }
        }
        if (null != interrupted)
            Thread.currentThread().interrupt();
        return last + delay;
    }
}
