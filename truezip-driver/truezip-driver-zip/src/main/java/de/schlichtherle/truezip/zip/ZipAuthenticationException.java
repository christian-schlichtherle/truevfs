/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import net.jcip.annotations.ThreadSafe;

/**
 * Thrown to indicate that an authenticated ZIP entry has been tampered with.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class ZipAuthenticationException extends ZipCryptoException {
    private static final long serialVersionUID = 2403462923846291232L;

    /**
     * Constructs a ZIP authentication exception with the given detail message.
     */
    public ZipAuthenticationException(String msg) {
        super(msg);
    }
}
