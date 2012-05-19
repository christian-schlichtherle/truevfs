/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.key.param;

import java.util.ResourceBundle;
import javax.annotation.concurrent.Immutable;

/**
 * Enumerates the AES cipher key strenghts.
 * 
 * @author Christian Schlichtherle
 */
@Immutable
public enum AesKeyStrength implements KeyStrength {
    /** 128 bit AES cipher key. */
    BITS_128,

    /** 192 bit AES cipher key. */
    BITS_192,

    /** 256 bit AES cipher key. */
    BITS_256;

    private static final ResourceBundle
            resources = ResourceBundle.getBundle(AesKeyStrength.class.getName());

    /** Returns the key strength in bytes. */
    @Override
    public int getBytes() {
        return 16 + 8 * ordinal();
    }

    /** Returns the key strength in bits. */
    @Override
    public int getBits() {
        return 8 * getBytes();
    }

    @Override
    public String toString() {
        return resources.getString(name());
    }
}