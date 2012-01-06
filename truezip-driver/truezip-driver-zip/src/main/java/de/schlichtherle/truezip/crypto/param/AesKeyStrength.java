/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.param;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ResourceBundle;
import net.jcip.annotations.ThreadSafe;

/**
 * Enumerates the AES cipher key strenghts.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
public enum AesKeyStrength implements KeyStrength {
    /** Enum identifier for a 128 bit AES cipher key. */
    BITS_128,

    /** Enum identifier for a 192 bit AES cipher key. */
    BITS_192,

    /** Enum identifier for a 256 bit AES cipher key. */
    BITS_256;

    private static final ResourceBundle resources
            = ResourceBundle.getBundle(AesKeyStrength.class.getName());

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
