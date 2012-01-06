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
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has been disabled.
 * This is normally caused by the client application, but will also happen
 * if the JVM is running in headless mode.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class KeyPromptingDisabledException extends CacheableUnknownKeyException  {
    private static final long serialVersionUID = 7656348649239172586L;

    public KeyPromptingDisabledException() {
        super("Key prompting has been disabled!");
    }

    public KeyPromptingDisabledException(Throwable cause) {
        super("Key prompting has been disabled!");
        super.initCause(cause);
    }
}
