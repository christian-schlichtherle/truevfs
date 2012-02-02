/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key.pbe.swing;

import javax.annotation.CheckForNull;
import java.security.GeneralSecurityException;

/**
 * Thrown to indicate that a password or key file is considered weak.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
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
