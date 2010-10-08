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

package de.schlichtherle.truezip.crypto.generators;

import de.schlichtherle.truezip.crypto.generator.DigestRandom;
import de.schlichtherle.truezip.util.Arrays;
import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.bouncycastle.crypto.digests.SHA256Digest;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DigestRandomTest extends TestCase {

    private Random rnd1;
    private Random rnd2;

    public static Test suite() {
        TestSuite suite = new TestSuite(DigestRandomTest.class);

        return suite;
    }

    public DigestRandomTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        rnd1 = new DigestRandom(new SHA256Digest());
        rnd2 = new DigestRandom(new SHA256Digest());
        //rnd1 = new SecureRandom();
        //rnd2 = new SecureRandom();
    }

    @Override
    protected void tearDown() throws Exception {
        rnd1 = null;
        rnd2 = null;
    }

    public void testNextBytes() {
        final int n = 1024 * 1024;
        final byte[] buf1 = new byte[n];
        final byte[] buf2 = new byte[n];
        for (int i = 32; --i >= 0; ) {
            rnd1.nextBytes(buf1);
            rnd2.nextBytes(buf2);
            assertFalse(Arrays.equals(buf1, 0, buf2, 0, n));
        }
    }

    public void testNext() {
        for (int i = 1024; --i >= 0; )
            assertTrue(rnd1.nextInt() != rnd2.nextInt());
    }
}
