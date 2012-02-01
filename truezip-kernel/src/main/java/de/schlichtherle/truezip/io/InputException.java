/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.Immutable;

/**
 * Thrown if an error happened on the input side rather than the output side
 * when copying an {@link InputStream} to an {@link OutputStream}.
 * 
 * @see     Streams#cat(InputStream, OutputStream)
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public class InputException extends IOException {
    private static final long serialVersionUID = 1287654325546872424L;

    /**
     * Constructs a new {@code InputException}.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public InputException(@CheckForNull Throwable cause) {
        super(cause);
    }
}
