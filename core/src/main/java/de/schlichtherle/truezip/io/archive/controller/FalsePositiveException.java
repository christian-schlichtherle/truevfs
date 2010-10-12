/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.archive.driver.TransientIOException;
import java.io.IOException;

/**
 * Indicates a false positive archive entry which may exist as an entry in an
 * enclosing file system.
 */
final class FalsePositiveException extends RuntimeException {
    private static final long serialVersionUID = 947139561381472363L;

    private final boolean trans;

    FalsePositiveException(final IOException cause) {
        super.initCause(cause instanceof TransientIOException ? cause.getCause() : cause);
        assert null != cause;
        // A transient I/O exception is just a wrapper exception to mark
        // the real transient cause, therefore we can safely throw it away.
        // We must do this in order to allow an archive controller to inspect
        // the real transient cause and act accordingly.
        trans = cause instanceof TransientIOException;
    }

    /** Returns the nullable cause of this exception. */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }

    /**
     * Returns {@code true} if and only if this exception was created with a
     * {@link TransientIOException} as its cause.
     */
    final boolean isTransient() {
        return trans;
    }
}
