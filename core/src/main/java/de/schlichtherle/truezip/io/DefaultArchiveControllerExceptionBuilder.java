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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.controller.ArchiveControllerException;
import de.schlichtherle.truezip.io.archive.controller.ArchiveControllerWarningException;
import de.schlichtherle.truezip.util.AbstractExceptionBuilder;

public final class DefaultArchiveControllerExceptionBuilder
extends AbstractExceptionBuilder<ArchiveControllerException, ArchiveException>
implements ArchiveControllerExceptionBuilder {

    /**
     * If the given {@link ArchiveControllerException} is an instance of an
     * {@link ArchiveControllerWarningException}, it is wrapped in a new
     * {@link ArchiveWarningException}.
     * Otherwise, it's wrapped in an {@link ArchiveException}.
     * In either case, the {@code previous} assembled exception is used as the
     * tail of the new exception chain.
     * <p>
     * {@inheritDoc}
     */
    protected ArchiveException assemble(
            final ArchiveException previous,
            final ArchiveControllerException cause) {
        return cause instanceof ArchiveControllerWarningException
                ? new ArchiveWarningException(previous, cause)
                : new ArchiveException(previous, cause);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class
     * {@link DefaultArchiveControllerExceptionBuilder}
     * sorts the assembled exception by
     * {@link ArchiveException#sortPriority() priority}
     * before returning the result.
     */
    @Override
    public ArchiveException reset(final ArchiveException exception) {
        final ArchiveException e = super.reset(exception);
        return e != null ? e.sortPriority() : null;
    }
}
