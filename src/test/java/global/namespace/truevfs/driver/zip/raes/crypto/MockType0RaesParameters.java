/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.zip.raes.crypto;

import global.namespace.truevfs.comp.key.api.common.AesKeyStrength;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Christian Schlichtherle
 */
public final class MockType0RaesParameters implements Type0RaesParameters {

    private static final Logger
            logger = LoggerFactory.getLogger(MockType0RaesParameters.class);
    private static final String PASSWD = "top secret";
    private static final AesKeyStrength[] keyStrengths = AesKeyStrength.values();

    private final Random rnd = new Random();
    private boolean secondTry;
    private AesKeyStrength keyStrength;

    @Override
    public char[] getPasswordForWriting() {
        return PASSWD.toCharArray();
    }

    @Override
    public char[] getPasswordForReading(boolean invalid) {
        assertTrue(secondTry || !invalid);
        if (secondTry) {
            logger.trace("First returned password was wrong, providing the right one now!");
            return PASSWD.toCharArray();
        } else {
            secondTry = true;
            byte[] buf = new byte[1];
            rnd.nextBytes(buf);
            return buf[0] >= 0 ? PASSWD.toCharArray() : "wrong".toCharArray();
        }
    }

    @Override
    public AesKeyStrength getKeyStrength() {
        keyStrength = keyStrengths[rnd.nextInt(keyStrengths.length)];
        logger.trace("Using {} bits cipher key.", keyStrength.getBits());
        return keyStrength;
    }

    @Override
    public void setKeyStrength(final AesKeyStrength keyStrength) {
        assertTrue(
                "null != this.keyStrength(" + this.keyStrength + ") && this.keyStrength != keyStrength(" + keyStrength + ")",
                null == this.keyStrength || this.keyStrength == keyStrength);
    }
}
