/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import java.security.GeneralSecurityException;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed.
 * The subclass provides more information.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class UnknownKeyException extends GeneralSecurityException {
    private static final long serialVersionUID = 6092786348232837265L;

    UnknownKeyException() {
    }

    protected UnknownKeyException(String msg) {
        super(msg);
    }

    public UnknownKeyException(Throwable cause) {
        super(cause);
    }
}
