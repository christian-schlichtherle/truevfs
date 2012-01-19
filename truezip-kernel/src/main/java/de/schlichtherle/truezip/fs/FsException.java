/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * An abstract exception which indicates an exceptional condition discovered
 * in the TrueZIP Kernel.
 * <p>
 * ONLY THE TRUEZIP KERNEL SHOULD THROW AN EXCEPTION OF THIS TYPE!
 * DO NOT CREATE OR THROW AN EXCEPTION OF THIS TYPE (INCLUDING SUB-CLASSES)
 * ANYWHERE ELSE!
 *
 * @see     FsController
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@SuppressWarnings("serial") // serializing an exception for a temporary event is nonsense!
public abstract class FsException extends IOException {

    FsException() {
        this(null);
    }

    FsException(Throwable cause) {
        super(cause);
    }
}
