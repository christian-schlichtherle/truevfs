/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.SequentialIOExceptionBuilder;
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
final class FsSyncExceptionBuilder
extends SequentialIOExceptionBuilder<IOException, FsSyncException> {
    FsSyncExceptionBuilder() {
        super(IOException.class, FsSyncException.class);
    }
}
