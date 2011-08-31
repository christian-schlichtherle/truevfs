/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.crypto.raes;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that a RAES file has been tampered with.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class RaesAuthenticationException extends RaesException {
    private static final long serialVersionUID = 2362389234686232732L;

    /**
     * Constructs a RAES exception with
     * a detail message indicating that a RAES file has been tampered with.
     */
    public RaesAuthenticationException() {
        super("Authenticated file content has been tampered with!");
    }
}
