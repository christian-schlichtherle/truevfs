/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.fs;

import de.truezip.kernel.io.SequentialIOException;
import de.truezip.kernel.io.SequentialIOExceptionBuilder;
import java.io.IOException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Assembles a {@link FsSyncException} from one or more {@link IOException}s by
 * {@link SequentialIOException#initPredecessor(SequentialIOException) chaining}
 * them.
 * When the assembly is thrown or returned later, it is sorted by
 * {@link SequentialIOException#sortPriority() priority}.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class FsSyncExceptionBuilder
extends SequentialIOExceptionBuilder<IOException, FsSyncException> {
    public FsSyncExceptionBuilder() {
        super(IOException.class, FsSyncException.class);
    }
}
