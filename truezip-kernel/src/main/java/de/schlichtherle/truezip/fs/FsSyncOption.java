/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.io.InputBusyException;
import de.schlichtherle.truezip.io.OutputBusyException;
import de.schlichtherle.truezip.io.InputClosedException;
import de.schlichtherle.truezip.io.OutputClosedException;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * Defines the available options for the synchronization of federated file
 * systems via the methods {@link FsController#sync} and
 * {@link FsManager#sync}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public enum FsSyncOption {

    /**
     * Suppose there are any open input resources (input streams etc.) for any
     * file system entries.
     * Then if this option is set, the respective file system controller waits
     * until all <em>other</em> threads have closed their input resources
     * before proceeding with the update of the federated file system.
     * Input resources allocated by the <em>current</em> thread are always
     * ignored.
     * If the current thread gets interrupted while waiting, it will stop
     * waiting and proceed normally as if this option wasn't set.
     * <p>
     * Beware: If an input resource has not been closed because the client
     * application does not always properly close its streams, even on an
     * {@link IOException} (which is a typical bug in many Java applications),
     * then the respective file system controller will not return from the
     * update until the current thread gets interrupted!
     */
    WAIT_CLOSE_INPUT,

    /**
     * Suppose there are any open input resources (input streams etc.) for any
     * file system entries.
     * Then if this option is set, the respective file system controller
     * proceeds to update the federated file system anyway and finally throws
     * an {@link FsSyncWarningException} with an {@link InputBusyException} as
     * its cause to indicate that any subsequent operations on these resources
     * will fail with an {@link InputClosedException} because they have been
     * forced to close.
     * <p>
     * If this option is not set however, the federated file system is
     * <em>not</em> updated, but instead
     * an {@link FsSyncException} with an {@link InputBusyException} as
     * its cause is thrown to indicate
     * that the application must close all input resources first.
     */
    FORCE_CLOSE_INPUT,

    /**
     * Similar to {@link #WAIT_CLOSE_INPUT},
     * but applies to file system entry output resources (output streams etc.)
     * instead.
     */
    WAIT_CLOSE_OUTPUT,

    /**
     * Similar to {@link #FORCE_CLOSE_INPUT},
     * but applies to file system entry output resources (output streams etc.)
     * and may throw an {@link OutputClosedException} /
     * {@link OutputBusyException} instead.
     * <p>
     * If this option is set, then
     * {@link #FORCE_CLOSE_INPUT} must be set, too.
     * Otherwise, an {@code IllegalArgumentException} is thrown.
     */
    FORCE_CLOSE_OUTPUT,

    /**
     * If this option is set, all pending changes are aborted.
     * This option is only meaningful immediately before the federated file
     * system itself gets deleted.
     */
    ABORT_CHANGES,

    /**
     * Suppose a controller for a federated file system has cached entry data.
     * Then if this option is set when the file system gets synchronized,
     * the cached entry data get cleared after flushing it to the file system.
     */
    CLEAR_CACHE,
}
