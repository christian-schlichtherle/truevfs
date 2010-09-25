/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Thrown if an {@link IOException} happened on the input side rather than
 * the output side when copying an InputStream to an OutputStream.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class InputException extends IOException {
    private static final long serialVersionUID = 1287654325546872424L;

    /**
     * Constructs a new {@code InputException}.
     *
     * @param cause A valid {@code IOException}.
     *        This must not be {@code null} and must not be an instance
     *        of {@link FileNotFoundException} in order to prevent masking it.
     * @throws IllegalArgumentException If {@code cause} is an instance of
     *         {@code FileNotFoundException}.
     */
    public InputException(final IOException cause) {
        super(cause != null ? cause.toString() : null);
        if (cause instanceof FileNotFoundException)
            throw new IllegalArgumentException(cause);
        super.initCause(cause);
    }

    /**
     * Returns the {@link IOException} provided as the cause when this
     * exception was created.
     *
     * @return The {@link IOException} provided as the cause when this
     * exception was created.
     */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
