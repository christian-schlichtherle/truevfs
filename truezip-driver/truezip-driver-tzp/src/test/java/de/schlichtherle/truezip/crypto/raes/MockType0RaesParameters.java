/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.crypto.raes;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.NotThreadSafe;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class MockType0RaesParameters implements Type0RaesParameters {

    private static final Logger
            logger = Logger.getLogger(MockType0RaesParameters.class.getName());
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
            logger.finest("First returned password was wrong, providing the right one now!");
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
        logger.log(Level.FINEST, "Using {0} bits cipher key.", keyStrength.getBits());
        return keyStrength;
    }

    @Override
    public void setKeyStrength(final KeyStrength keyStrength) {
        assertTrue(
                "null != this.keyStrength(" + this.keyStrength + ") && this.keyStrength != keyStrength(" + keyStrength + ")",
                null == this.keyStrength || this.keyStrength == keyStrength);
    }
}
