/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * Thrown if an error happened on the input side rather than the output side
 * when copying an {@link InputStream} to an {@link OutputStream}.
 * 
 * @see     Streams#cat(InputStream, OutputStream)
 * @author  Christian Schlichtherle
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