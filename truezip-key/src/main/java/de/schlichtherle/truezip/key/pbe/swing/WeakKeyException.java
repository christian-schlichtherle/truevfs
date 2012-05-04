/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.key.pbe.swing;

import java.security.GeneralSecurityException;
import javax.annotation.CheckForNull;

/**
 * Thrown to indicate that a password or key file is considered weak.
 *
 * @author  Christian Schlichtherle
 */
public class WeakKeyException extends GeneralSecurityException {
    private static final long serialVersionUID = 2946387652018652745L;

    /**
     * Creates a new {@code WeakKeyException} with the given message.
     */
    WeakKeyException(@CheckForNull String message) {
        super(message);
    }
}