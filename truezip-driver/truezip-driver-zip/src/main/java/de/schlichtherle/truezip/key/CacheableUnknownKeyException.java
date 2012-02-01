/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.key;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that the retrieval of the key to open or create a
 * protected resource has failed and that this exception is cacheable.
 * The subclass provides more information.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class CacheableUnknownKeyException extends UnknownKeyException {
    private static final long serialVersionUID = 2463586348235337265L;

    CacheableUnknownKeyException() {
    }

    CacheableUnknownKeyException(@CheckForNull String message) {
        super(message);
    }
}
