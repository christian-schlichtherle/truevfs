/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that prompting for a key to open or create a
 * protected resource has timed out.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class KeyPromptingTimeoutException extends UnknownKeyException {
    private static final long serialVersionUID = 7656348612765052586L;

    public KeyPromptingTimeoutException() {
        super("Key prompting has timed out!");
    }

    public KeyPromptingTimeoutException(Throwable cause) {
        super("Key prompting has timed out!");
        super.initCause(cause);
    }
}
