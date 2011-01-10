/*
 * Copyright 2010 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.io.ChainableIOException;
import de.schlichtherle.truezip.io.fs.FsSyncException;
import de.schlichtherle.truezip.io.fs.FsSyncWarningException;
import de.schlichtherle.truezip.util.AbstractExceptionBuilder;
import java.io.IOException;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ArchiveExceptionBuilder
extends AbstractExceptionBuilder<IOException, ArchiveException> {

    @Override
    protected ArchiveException update(IOException cause, ArchiveException previous) {
        final ArchiveException next;
        if (cause instanceof ArchiveException)
            next = (ArchiveException) cause;
        else if (cause instanceof FsSyncWarningException)
            next = new ArchiveWarningException(cause.getMessage(), cause.getCause()); // remove FsSyncWarningException - it's not thrown yet so it has no stack trace anyway!
        else if (cause instanceof FsSyncException)
            next = new ArchiveException(cause.getMessage(), cause.getCause()); // remove FsSyncWarningException - it's not thrown yet so it has no stack trace anyway!
        else
            next = new ArchiveException(cause.getMessage(), cause);
        try {
            return (ArchiveException) next.initPredecessor(previous);
        } catch (IllegalStateException ex) {
            if (previous != null)
                throw (IllegalStateException) ex.initCause(next);
            return next;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link ArchiveExceptionBuilder}
     * sorts the given exception chain by
     * {@link ChainableIOException#sortPriority() priority} and returns the
     * result.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected final ArchiveException post(ArchiveException assembly) {
        return null == assembly ? null : (ArchiveException) assembly.sortPriority();
    }
}
