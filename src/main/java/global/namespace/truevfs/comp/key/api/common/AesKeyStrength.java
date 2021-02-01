/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api.common;

import global.namespace.truevfs.comp.key.api.KeyStrength;

import java.util.ResourceBundle;

/**
 * Enumerates the AES cipher key strenghts.
 *
 * @author Christian Schlichtherle
 */
public enum AesKeyStrength implements KeyStrength {

    /**
     * 128 bit AES cipher key.
     */
    BITS_128,

    /**
     * 192 bit AES cipher key.
     */
    BITS_192,

    /**
     * 256 bit AES cipher key.
     */
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
