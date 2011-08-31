/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.jcip.annotations.ThreadSafe;

/**
 * Thrown if an error happened on the input side rather than the output side
 * when copying an {@link InputStream} to an {@link OutputStream}.
 * 
 * @see     Streams#cat(InputStream, OutputStream)
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
public class InputException extends IOException {
    private static final long serialVersionUID = 1287654325546872424L;

    /**
     * Constructs a new {@code InputException}.
     *
     * @param cause the cause for this exception to get thrown.
     */
    public InputException(final Throwable cause) {
        super(cause);
    }
}
