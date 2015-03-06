/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;
import net.java.truecommons.io.ClosedInputException;
import net.java.truecommons.io.ClosedOutputException;
import net.java.truecommons.shed.BitField;

/**
 * Defines options for (virtual) file system synchronization.
 *
 * @see    FsController#sync(BitField)
 * @see    FsSyncOptions
 * @author Christian Schlichtherle
 */
@Immutable
public enum FsSyncOption {

    /**
     * Suppose there are any open I/O streams or channels for any file system
     * entries.
     * Then if this option is set, the respective file system controller waits
     * until all <em>other</em> threads have closed their I/O resources before
     * proceeding with the update of the file system.
     * I/O resources allocated by the <em>current</em> thread are always
     * ignored.
     * If the current thread gets interrupted while waiting, it will stop
     * waiting and proceed normally as if this option wasn't set.
     * <p>
     * <strong>WARNING:</strong> If an I/O resource has not been closed because
     * the client application does not always properly close its streams, even
     * on an {@link IOException} (which is a common bug in many Java
     * applications), then the respective file system controller will not
     * return from the update until the current thread gets interrupted!
     */
    WAIT_CLOSE_IO,

    /**
     * Suppose there are any open I/O streams or channels for any file system
     * entries.
     * Then if this option is set, the respective file system controller
     * proceeds to update the file system anyway and finally throws an
     * {@link FsSyncWarningException} with a
     * {@link FsOpenResourceException} as its cause to indicate that any
     * subsequent operations on these resources will fail with an
     * {@link ClosedInputException} or {@link ClosedOutputException}
     * respectively because they have been forced to close.
     * <p>
     * If this option is not set however, the file system is <em>not</em>
     * updated, but instead an {@link FsSyncException} with a
     * {@link FsOpenResourceException} as its cause is thrown to indicate that
     * the application must close all I/O resources first.
     */
    FORCE_CLOSE_IO,

    /**
     * If this option is set, all pending changes are aborted.
     * This option is only meaningful immediately before the file system itself
     * gets deleted and should not of used by client applications.
     */
    ABORT_CHANGES,

    /**
     * Suppose a file system controller has selectively cached entry contents.
     * Then if this option is set when the file system gets synchronized,
     * the entry contents get cleared.
     * <p>
     * Note that this option may induce dead locks or even busy loops
     * when accessing nested archive files in different threads.
     *
     * @see <a href="http://java.net/jira/browse/TRUEZIP-268">#TRUEZIP-268</a>
     * @see <a href="http://java.net/jira/browse/TRUEZIP-269">#TRUEZIP-269</a>
     */
    CLEAR_CACHE,
}
