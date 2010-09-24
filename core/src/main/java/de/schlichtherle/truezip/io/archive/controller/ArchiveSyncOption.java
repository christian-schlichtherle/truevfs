/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.socket.common.entry.CommonEntryStreamClosedException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Defines the available options for archive synchronization operations, i.e.
 * {@link ArchiveControllers#sync(URI, BitField, ArchiveSyncExceptionBuilder)}
 * and {@link ArchiveController#sync(BitField, ArchiveSyncExceptionBuilder)}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public enum ArchiveSyncOption {

    /**
     * Suppose any other thread has still one or more archive entry input
     * streams open to an archive controller's target file.
     * Then if and only if this property is {@code true}, the respective
     * archive controller will wait until all other threads have closed
     * their archive entry input streams before proceeding with the update
     * of the target archive file.
     * Archive entry input streams opened (and not yet closed) by the
     * current thread are always ignored.
     * If the current thread gets interrupted while waiting, it will
     * stop waiting and proceed normally as if this property is
     * {@code false}.
     * <p>
     * Beware: If a stream has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java
     * applications), then the respective archive controller will not
     * return from the update until the current thread gets interrupted!
     */
    WAIT_FOR_INPUT_STREAMS,
    /**
     * Suppose there are any open input streams for any archive entries of
     * an archive controller's target file because the client application has
     * forgot to {@link InputStream#close()} all {@code InputStream} objects
     * or another thread is still busy doing I/O on the target archive file.
     * Then if this property is {@code true}, the respective archive
     * controller will proceed to update the target archive file anyway and
     * finally throw an {@link ArchiveBusyWarningException} to indicate
     * that any subsequent operations on these streams will fail with an
     * {@link CommonEntryStreamClosedException} because they have been
     * forced to close.
     * <p>
     * If this property is {@code false}, the target archive file is
     * <em>not</em> updated and an {@link ArchiveBusyException} is thrown to
     * indicate that the application must close all entry input streams
     * first.
     */
    CLOSE_INPUT_STREAMS,
    /**
     * Similar to {@code waitInputStreams},
     * but applies to archive entry output streams instead.
     */
    WAIT_FOR_OUTPUT_STREAMS,
    /**
     * Similar to {@code closeInputStreams},
     * but applies to archive entry output streams instead.
     * <p>
     * If this parameter is {@code true}, then
     * {@code closeInputStreams} must be {@code true}, too.
     * Otherwise, an {@code IllegalArgumentException} is thrown.
     */
    CLOSE_OUTPUT_STREAMS,
    /**
     * If this property is {@code true}, the archive controller's target file
     * is completely released in order to enable subsequent read/write access
     * to it for third parties such as other processes <em>before</em> TrueZIP
     * can be used again to read from or write to the target archive file.
     * <p>
     * If this property is {@code true}, some temporary files might be retained
     * for caching in order to enable faster subsequent access to the archive
     * file again.
     * <p>
     * Note that temporary files are always deleted by TrueZIP unless the JVM
     * is terminated unexpectedly. This property solely exists to control
     * cooperation with third parties or enabling faster access.
     */
    UMOUNT,
    /**
     * Let's assume an archive controller's target file is enclosed in another
     * archive file.
     * Then if this property is {@code true}, the updated target archive file
     * is also written to its enclosing archive file.
     * Note that this property <em>must</em> be set to {@code true} if the
     * property {@code umount} is set to {@code true} as well.
     * Failing to comply to this requirement may throw an
     * {@link AssertionError} and will incur loss of data!
     */
    REASSEMBLE,
}
