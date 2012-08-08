/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.zip.raes.crypto;

import net.java.truevfs.driver.zip.raes.crypto.Type0RaesParameters;
import java.util.Random;
import javax.annotation.concurrent.NotThreadSafe;
import net.java.truevfs.key.spec.param.AesKeyStrength;
import static org.junit.Assert.assertTrue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class MockType0RaesParameters implements Type0RaesParameters {

    private static final Logger
            logger = LoggerFactory.getLogger(MockType0RaesParameters.class);
    private static final String PASSWD = "top secret";
    private static final AesKeyStrength[] keyStrengths = AesKeyStrength.values();

    private final Random rnd = new Random();
    private boolean secondTry;
    private AesKeyStrength keyStrength;

    @Override
    public char[] getWritePassword() {
        return PASSWD.toCharArray();
    }

    @Override
    public char[] getReadPassword(boolean invalid) {
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
