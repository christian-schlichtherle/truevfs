/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.key.pbe.swing;

import javax.annotation.CheckForNull;
import java.security.GeneralSecurityException;

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
    public WeakKeyException(@CheckForNull String message) {
        super(message);
    }
}