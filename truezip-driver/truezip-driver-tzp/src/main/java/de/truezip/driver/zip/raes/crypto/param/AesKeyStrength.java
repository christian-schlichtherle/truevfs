/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.crypto.param;

import java.util.ResourceBundle;
import javax.annotation.concurrent.Immutable;

/**
 * Enumerates the AES cipher key strenghts.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public enum AesKeyStrength implements de.schlichtherle.truezip.crypto.param.KeyStrength {
    /** Enum identifier for a 128 bit AES cipher key. */
    BITS_128,

    /** Enum identifier for a 192 bit AES cipher key. */
    BITS_192,

    /** Enum identifier for a 256 bit AES cipher key. */
    BITS_256;

    private static final ResourceBundle
            resources = ResourceBundle.getBundle(AesKeyStrength.class.getName());

    @Override
    public int getBytes() {
        return 16 + 8 * ordinal();
    }

    @Override
    public int getBits() {
        return 8 * getBytes();
    }

    @Override
    public String toString() {
        return resources.getString(name());
    }
}