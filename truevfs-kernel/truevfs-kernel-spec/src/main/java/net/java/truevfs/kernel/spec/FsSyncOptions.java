/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.shed.BitField;
import static net.java.truevfs.kernel.spec.FsSyncOption.*;

/**
 * Provides common bit fields of synchronization options.
 *
 * @see    FsController#sync(BitField)
 * @see    FsSyncOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsSyncOptions {

    /** A bit field with no synchronization options set. */
    public static final BitField<FsSyncOption>
            NONE = BitField.noneOf(FsSyncOption.class);

    /**
     * Forcibly closes all I/O resources (i.e. streams, channels etc) for any
     * entries of the file system, flushes and clears its selective entry cache,
     * commits all changes to its parent file system (if any) and makes its
     * controller eligible for garbage collection unless any strong references
     * are held by the client application.
     * This is equivalent to
     * {@code BitField.of(FsSyncOption.FORCE_CLOSE_IO, FsSyncOption.CLEAR_CACHE)}.
     * <p>
     * These options should be used if an application wants to
     * synchronize all mounted archive files and make sure to clean up
     * <em>all</em> resources, including the selective entry cache.
     * Care should be taken not to use these options while any other thread
     * is still doing I/O to the archive files because otherwise the threads
     * may not be able to succeed and receive an exception.
     */
    public static final BitField<FsSyncOption>
            UMOUNT = BitField.of(FORCE_CLOSE_IO, CLEAR_CACHE);

    /**
     * Waits for all other threads to close their I/O resources (i.e. streams,
     * channels etc) for any entries of the file system, flushes its selective
     * entry cache without clearing it and commits all changes to its parent
     * file system (if any).
     * This is equivalent to {@code BitField.of(FsSyncOption.WAIT_CLOSE_IO)}.
     * <p>
     * These options should be used if a multithreaded application wants to
     * synchronize all mounted archive files without affecting any I/O to
     * these archive files by any other thread.
     * <p>
     * Note that this bit field deliberately doesn't include CLEAR_CACHE!
     * This is because CLEAR_CACHE may induce dead locks or even busy loops
     * when accessing nested archive files in different threads.
     *
     * @see <a href="http://java.net/jira/browse/TRUEZIP-268">#TRUEZIP-268</a>
     * @see <a href="http://java.net/jira/browse/TRUEZIP-269">#TRUEZIP-269</a>
     */
    public static final BitField<FsSyncOption>
            SYNC = BitField.of(WAIT_CLOSE_IO);

    /**
     * Aborts all pending changes for the federated file system, clears the
     * selective cache without flushing it and makes the file system controller
     * eligible for garbage collection unless any strong references are held by
     * the client application.
     * This is equivalent to
     * {@code BitField.of(FsSyncOption.ABORT_CHANGES)}.
     * <p>
     * These options are only meaningful immediately before the federated file
     * system itself gets deleted and should not of used by client
     * applications.
     */
    public static final BitField<FsSyncOption>
            RESET = BitField.of(ABORT_CHANGES);

    /**
     * Converts the given array to a bit field of synchronization options.
     *
     * @param  options an array of synchronization options.
     * @return A bit field of synchronization options.
     */
    public static BitField<FsSyncOption> of(FsSyncOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private FsSyncOptions() { }
}
