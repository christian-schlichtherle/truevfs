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

package de.schlichtherle.truezip.io.archive.driver;

import de.schlichtherle.truezip.io.File;
import java.io.IOException;

/**
 * May be thrown by archive drivers to indicate that an {@link IOException}
 * occured as a transient event when accessing an archive file and another
 * try to access the same archive file may finally succeed.
 * On the other hand, if the archive controller catches an IOException from
 * an an archive driver when trying to access an archive file which is
 * <em>not</em> a {@code TransientIOException}, then the archive
 * controller may consider the archive file to be a false positive and cache
 * the exception until {@link File#umount} or {@link File#update} is called.
 * <p>
 * This feature is primarily used by the RAES encrypted ZIP file driver
 * family when prompting for passwords has been cancelled by the user.
 * In this case, the driver will wrap the {@code IOException} in a
 * {@code TransientIOException} and throw this instead to signal that
 * another attempt to prompt the user should be allowed.
 * <p>
 * This class is marked final since the archive controller will throw
 * away this exception anyway and deal with the transient cause only.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TransientIOException
extends IOException {

    /**
     * @param cause The transient cause of this exception.
     * @throws NullPointerException If {@code cause} is {@code null}.
     */
    public TransientIOException(IOException cause) {
        if (cause == null)
            throw new NullPointerException();
        super.initCause(cause);
    }

    /**
     * Returns the transient cause of this exception as an
     * {@code IOException} - {@code null} is never returned.
     */
    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
