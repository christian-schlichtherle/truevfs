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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.archive.controller.Controllers;
import de.schlichtherle.truezip.io.socket.InputClosedException;
import de.schlichtherle.truezip.io.socket.OutputClosedException;
import java.io.IOException;

/**
 * Defines the available options for archive synchronization operations, i.e.
 * {@link Controllers#sync} and {@link CompositeFileSystemController#sync}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public enum SyncOption {

    /**
     * Suppose there are any open input streams or read only files for any
     * archive entries of an archive controller's target archive file.
     * Then if this option is set, the archive controller waits until all
     * <em>other</em> threads have closed their archive entry input streams
     * and read only files before proceeding with the update of the target
     * archive file.
     * Archive input streams and read only files opened by the
     * <em>current</em> thread are always ignored.
     * If the current thread gets interrupted while waiting, it will
     * stop waiting and proceed normally as if this options wasn't set.
     * <p>
     * Beware: If a stream has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java
     * applications), then the respective archive controller will not
     * return from the update until the current thread gets interrupted!
     */
    WAIT_CLOSE_INPUT,

    /**
     * Suppose there are any open input streams or read only files for any
     * archive entries of an archive controller's target archive file.
     * Then if this option is set, the archive controller will proceed to
     * update the target archive file anyway and finally throw a
     * {@link SyncWarningException} with a {@link InputBusyException} as its
     * cause to indicate that any subsequent operations on these streams will
     * fail with an {@link InputClosedException} because they have been forced
     * to close.
     * <p>
     * If this option is not set, the target archive file is <em>not</em>
     * updated and an {@link InputBusyException} is thrown to indicate
     * that the application must close all entry input streams and read
     * only files first.
     */
    FORCE_CLOSE_INPUT,

    /**
     * Similar to {@link #WAIT_CLOSE_INPUT},
     * but applies to archive entry output streams instead.
     */
    WAIT_CLOSE_OUTPUT,

    /**
     * Similar to {@link #FORCE_CLOSE_INPUT},
     * but applies to archive entry output streams and may throw a
     * {@link OutputClosedException} / {@link OutputBusyException} instead.
     * <p>
     * If this option is set, then
     * {@link #FORCE_CLOSE_INPUT} must be set, too.
     * Otherwise, an {@code IllegalArgumentException} is thrown.
     */
    FORCE_CLOSE_OUTPUT,

    /**
     * If this option is set, all pending changes are aborted.
     * This option will leave a corrupted target archive file and is only
     * meaningful if the target archive file gets deleted immediately.
     * <p>
     * Note that this option is mutually exclusive with {@link #FLUSH_CACHE}.
     * If both are set, an {@code IllegalArgumentException} is thrown.
     */
    ABORT_CHANGES,

    /**
     * Suppose an archive controller has cached output data for archive entries.
     * Then if this option is set, the cached data gets written to the
     * target archive file when it gets synchronized.
     * <p>
     * Note that this option is mutually exclusive with {@link #ABORT_CHANGES}.
     * If both are set, an {@code IllegalArgumentException} is thrown.
     */
    FLUSH_CACHE,
}
