/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

/**
 * Represents a chain of exceptions thrown by the {@link File#umount} and
 * {@link File#update} methods to indicate an error condition which
 * <em>does</em> incur loss of data.
 * 
 * <p>Both methods catch any exceptions occuring throughout their processing
 * and store them in an exception chain until all archive files have been
 * updated.
 * Finally, if the exception chain is not empty, it's reordered and thrown
 * so that if its head is an instance of {@code ArchiveWarningException},
 * only instances of this class or its subclasses are in the chain, but no
 * instances of {@code ArchiveException} or its subclasses (except
 * {@code ArchiveWarningException}, of course).
 *
 * <p>This enables client applications to do a simple case distinction with a
 * try-catch-block like this to react selectively:</p>
 * <pre>{@code 
 * try {
 *     File.umount();
 * } catch (ArchiveWarningException warning) {
 *     // Only warnings have occured and no data has been lost - ignore this.
 * } catch (ArchiveException error) {
 *     // Some data has been lost - panic!
 *     error.printStackTrace();
 * }
 * }</pre>
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveException extends ChainableIOException {
    private static final long serialVersionUID = 4893204620357369739L;

    // TODO: Make this constructor package private!
    /**
     * Constructs a new exception with the specified prior exception.
     * This is used when e.g. updating all ZIP files and more than one ZIP
     * compatible file cannot get updated. The prior exception would then be
     * the exception for the ZIP compatible file which couldn't get updated
     * before.
     *
     * @param  priorException An exception that happened before and that was
     *         caught. This is <b>not</b> a cause! May be {@code null}.
     */
    public ArchiveException(ArchiveException priorException) {
        super(priorException);
    }

    // TODO: Make this constructor package private!
    /**
     * Constructs a new exception with the specified prior exception
     * and a message.
     * This is used when e.g. updating all ZIP files and more than one ZIP
     * compatible file cannot get updated. The prior exception would then be
     * the exception for the ZIP compatible file which couldn't get updated
     * before.
     *
     * @param  priorException An exception that happened before and that was
     *         caught. This is <b>not</b> a cause! May be {@code null}.
     * @param  message The message for this exception.
     */
    public ArchiveException(
            ArchiveException priorException,
            String message) {
        super(priorException, message);
    }
    
    // TODO: Make this constructor package private!
    /**
     * Constructs a new exception with the specified prior exception and the
     * cause.
     * This is used when e.g. updating all ZIP files and more than one ZIP
     * compatible file cannot get updated. The prior exception would then be
     * the exception for the ZIP compatible file which couldn't get updated
     * before.
     *
     * @param  priorException An exception that happened before and that was
     *         caught. This is <b>not</b> a cause! May be {@code null}.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A {@code null} value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.).
     */
    public ArchiveException(
            ArchiveException priorException,
            IOException cause) {
        super(priorException, cause);
    }

    // TODO: Make this constructor package private!
    /**
     * Constructs a new exception with the specified prior exception,
     * a message and a cause.
     * This is used when e.g. updating all ZIP files and more than one ZIP
     * compatible file cannot get updated. The prior exception would then be
     * the exception for the ZIP compatible file which couldn't get updated
     * before.
     *
     * @param  priorException An exception that happened before and that was
     *         caught. This is <b>not</b> a cause! May be {@code null}.
     * @param  message The message for this exception.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A {@code null} value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.).
     */
    public ArchiveException(
            ArchiveException priorException,
            String message,
            IOException cause) {
        super(priorException, message, cause);
    }
}
