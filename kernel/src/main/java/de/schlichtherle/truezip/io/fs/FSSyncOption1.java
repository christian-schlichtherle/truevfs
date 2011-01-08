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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.socket.InputClosedException;
import de.schlichtherle.truezip.io.socket.OutputClosedException;
import java.io.IOException;

/**
 * Defines the available options for the synchronization of federated file
 * systems via the methods {@link FSController#sync} and
 * {@link FSManager#sync}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public enum FSSyncOption1 {

    /**
     * Suppose there are any open input streams or read only files for any
     * file system entries.
     * Then if this option is set, the file system controller waits until all
     * <em>other</em> threads have closed their entry input streams
     * and read only files before proceeding with the update of the federated
     * file system.
     * Input streams and read only files opened by the
     * <em>current</em> thread are always ignored.
     * If the current thread gets interrupted while waiting, it will
     * stop waiting and proceed normally as if this options wasn't set.
     * <p>
     * Beware: If a stream has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java
     * applications), then the respective file system controller will not
     * return from the update until the current thread gets interrupted!
     */
    WAIT_CLOSE_INPUT,

    /**
     * Suppose there are any open input streams or read only files for any
     * file system entries.
     * Then if this option is set, the file system controller will proceed to
     * update the federated file system anyway and finally throw a
     * {@link FSSyncWarningException} with a {@link InputBusyException} as its
     * cause to indicate that any subsequent operations on these streams will
     * fail with an {@link InputClosedException} because they have been forced
     * to close.
     * <p>
     * If this option is not set, the federated file system is <em>not</em>
     * updated and an {@link InputBusyException} is thrown to indicate
     * that the application must close all entry input streams and read
     * only files first.
     */
    FORCE_CLOSE_INPUT,

    /**
     * Similar to {@link #WAIT_CLOSE_INPUT},
     * but applies to file system entry output streams instead.
     */
    WAIT_CLOSE_OUTPUT,

    /**
     * Similar to {@link #FORCE_CLOSE_INPUT},
     * but applies to file system entry output streams and may throw a
     * {@link OutputClosedException} / {@link OutputBusyException} instead.
     * <p>
     * If this option is set, then
     * {@link #FORCE_CLOSE_INPUT} must be set, too.
     * Otherwise, an {@code IllegalArgumentException} is thrown.
     */
    FORCE_CLOSE_OUTPUT,

    /**
     * If this option is set, all pending changes are aborted.
     * This option may leave the federated file system corrupted and is only
     * meaningful immediately before the federated file system gets deleted.
     */
    ABORT_CHANGES,

    /**
     * Suppose a controller of a federated file system has cached entry data.
     * Then if this option is set, the cached entry data get cleared after
     * flushing it to the file system when it gets synchronized.
     */
    CLEAR_CACHE,
}
