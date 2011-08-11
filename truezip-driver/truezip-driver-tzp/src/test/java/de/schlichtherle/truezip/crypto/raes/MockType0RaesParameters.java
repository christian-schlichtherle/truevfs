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
package de.schlichtherle.truezip.crypto.raes;

import net.jcip.annotations.NotThreadSafe;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
public final class MockType0RaesParameters implements Type0RaesParameters {

    private static final Logger logger = Logger.getLogger(
            MockType0RaesParameters.class.getName());
    private static final String PASSWD = "secret";
    private static final KeyStrength[] keyStrengths = KeyStrength.values();

    private final Random rnd = new Random();
    private boolean secondTry;
    private KeyStrength keyStrength;

    @Override
    public char[] getWritePassword() {
        return PASSWD.toCharArray();
    }

    @Override
    public char[] getReadPassword(boolean invalid) {
        assertTrue(secondTry || !invalid);
        if (secondTry) {
            logger.finer("First returned password was wrong, providing the right one now!");
            return PASSWD.toCharArray();
        } else {
            secondTry = true;
            byte[] buf = new byte[1];
            rnd.nextBytes(buf);
            return buf[0] >= 0 ? PASSWD.toCharArray() : "wrong".toCharArray();
        }
    }

    @Override
    public KeyStrength getKeyStrength() {
        keyStrength = keyStrengths[rnd.nextInt(keyStrengths.length)];
        logger.log(Level.FINE, "Using {0} bits cipher key.", keyStrength.getBits());
        return keyStrength;
    }

    @Override
    public void setKeyStrength(KeyStrength keyStrength) {
        assertTrue(null == this.keyStrength || this.keyStrength == keyStrength);
    }
}
