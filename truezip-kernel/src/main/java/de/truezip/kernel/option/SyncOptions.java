/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.option;

import static de.truezip.kernel.option.SyncOption.*;
import de.truezip.kernel.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common bit fields of synchronization options.
 * 
 * @see    SyncOption
 * @author Christian Schlichtherle
 */
@Immutable
public final class SyncOptions {

    /** A bit field with no synchronization options set. */
    public static final BitField<SyncOption>
            NONE = BitField.noneOf(SyncOption.class);

    /**
     * Forcibly closes all I/O resources (i.e. streams, channels etc) for any
     * entries of the file system, flushes and clears its selective entry cache,
     * commits all changes to its parent file system (if any) and makes its
     * controller eligible for garbage collection unless any strong references
     * are held by the client application.
     * This is equivalent to
     * {@code BitField.of(SyncOption.FORCE_CLOSE_IO, SyncOption.CLEAR_CACHE)}.
     * <p>
     * These options should be used if an application wants to
     * synchronize all mounted archive files and make sure to clean up
     * <em>all</em> resources, including the selective entry cache.
     * Care should be taken not to use these options while any other thread
     * is still doing I/O to the archive files because otherwise the threads
     * may not be able to succeed and receive an exception.
     */
    public static final BitField<SyncOption>
            UMOUNT = BitField.of(FORCE_CLOSE_IO, CLEAR_CACHE);

    /**
     * Waits for all other threads to close their I/O resources (i.e. streams,
     * channels etc) for any entries of the file system, flushes its selective
     * entry cache without clearing it and commits all changes to its parent
     * file system (if any).
     * This is equivalent to {@code BitField.of(SyncOption.WAIT_CLOSE_IO)}.
     * <p>
     * These options should be used if a multithreaded application wants to
     * synchronize all mounted archive files without affecting any I/O to
     * these archive files by any other thread.
     * However, a call with the {@link #UMOUNT} options is still required in
     * order to really clean up <em>all</em> resources, including the
     * selective entry cache.
     */
    // Note that setting CLEAR_CACHE may cause endless loops when working with
    // nested archive files and there are copy operations where an input stream
    // has been successfully acquired and then acquiring the output stream
    // would require an automatic sync() of the same target archive file from
    // which the input stream is reading.
    public static final BitField<SyncOption>
            SYNC = BitField.of(WAIT_CLOSE_IO);

    /**
     * Aborts all pending changes for the federated file system, clears the
     * selective cache without flushing it and makes the file system controller
     * eligible for garbage collection unless any strong references are held by
     * the client application.
     * This is equivalent to
     * {@code BitField.of(SyncOption.ABORT_CHANGES)}.
     * <p>
     * These options are only meaningful immediately before the federated file
     * system itself gets deleted and should not of used by client
     * applications.
     */
    public static final BitField<SyncOption>
            RESET = BitField.of(ABORT_CHANGES);

    /**
     * Converts the given array to a bit field of synchronization options.
     * 
     * @param  options an array of synchronization options.
     * @return A bit field of synchronization options.
     */
    public static BitField<SyncOption> of(SyncOption... options) {
        return 0 == options.length ? NONE : BitField.of(options[0], options);
    }

    /* Can't touch this - hammer time! */
    private SyncOptions() { }
}
