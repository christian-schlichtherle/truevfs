/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.crypto.generators;

import java.security.SecureRandom;
import java.util.Random;
import org.bouncycastle.crypto.Digest;

/**
 * A Pseudo Random Number Generator (PRNG) using an arbitrary digest function.
 * Similar to {@link SecureRandom}, this class is self-seeding.
 * However, unlike {@code SecureRandom}, it's not seedable:
 * Calling one of the {@link Random#setSeed} methods does not have any effect!
 * <p>
 * Unlike its super class, this class does not support serialization.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DigestRandom extends Random {

    private static final SecureRandom seeder = new SecureRandom();
    private static final long serialVersionUID = 1236745263589856228L;

    private final byte[] in;
    private long counter;

    private final Digest digest;

    private final byte[] out;
    private int outOff;

    public DigestRandom(Digest digest) {
        this.digest = digest;
        final int size = digest.getDigestSize();
        in = new byte[size];
        out = new byte[size];
        outOff = size;

        // Seed the PRNG from the seeder PRNG and the current time.
        // Note: DON'T use seeder.generateSeed(size)! While this makes a very
        // good seed indeed (on Linux), it seriously KILLS PERFORMANCE!
        // The resulting performance is so bad that the unit test in
        // truevfs.io.RandomDataZip32RaesTest may compute for hours
        // instead of a few minutes (on my computer).
        // Note that the seeder is self-seeded whith a true random number on
        // the first call anyway, so calling generateSeed() is not required.
        seeder.nextBytes(in);

        // This is probably a bit paranoid...
        long time = System.currentTimeMillis();
        for (int i = Math.min(size, 8); --i >= 0; ) {
            in[i] ^= time; // XOR with PRNG
            time >>= 8;
        }
    }

    /**
     * Generates a user-specified number of pseudo random bytes.
     * This method is used as the basis of all random entities returned by
     * this class (except seed bytes).
     *
     * @param bytes The array to be filled in with random bytes.
     */
    @Override
    synchronized public void nextBytes(byte[] bytes) {
        for (int i = bytes.length; --i >= 0; ) {
            update();
            bytes[i] = out[outOff++];
        }
    }

    /**
     * Generates an integer containing the user-specified number of
     * pseudo-random bits (right justified, with leading zeros).
     * This method overrides a {@code java.util.Random} method, and serves
     * to provide a source of random bits to all of the methods inherited
     * from that class (for example, {@code nextInt}, {@code nextLong},
     * and {@code nextFloat}).
     *
     * @param numBits Number of pseudo-random bits to be generated, where
     *        0 <= {@code numBits} <= 32.
     *
     * @return An {@code int} containing the user-specified number
     *         of pseudo-random bits (right justified, with leading zeros).
     */
    @Override
    final protected int next(final int numBits) {
	final int numBytes = (numBits + 7) >>> 3; // round up

        int next = 0;
	for (int i = numBytes; --i >= 0; ) {
            update();
	    next = (next << 8) | (out[outOff++] & 0xFF);
        }

	return next >>> ((numBytes << 3) - numBits); // shift away rounded bits
    }

    private void update() {
        if (outOff >= out.length) {
            long counter = ++this.counter;
            for (int i = in.length; --i >= 0; ) {
                counter += in[i] & 0xff;
                in[i] = (byte) counter;
                counter >>>= 8;
            }
            digest.update(in, 0, in.length);
            digest.doFinal(out, 0);
            outOff = 0;
        }
    }
}
