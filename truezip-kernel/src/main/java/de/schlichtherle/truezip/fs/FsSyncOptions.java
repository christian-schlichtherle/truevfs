/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.util.BitField;
import javax.annotation.concurrent.Immutable;

/**
 * Provides common options for use with {@link FsController#sync(BitField)} and
 * its many variants and incarnations in the TrueZIP Kernel and client API
 * modules.
 * 
 * @see    FsSyncOption
 * @since  TrueZIP 7.1.1
 * @author Christian Schlichtherle
 */
@Immutable
public class FsSyncOptions {

    /**
     * Forcibly closes all I/O resources (i.e. streams, channels etc) for any
     * entries of the file system, flushes and clears its selective entry cache,
     * commits all changes to its parent file system (if any) and makes its
     * controller eligible for garbage collection unless any strong references
     * are held by the client application.
     * This is equivalent to
     * {@code BitField.of(FsSyncOption.FORCE_CLOSE_INPUT, FsSyncOption.FORCE_CLOSE_OUTPUT, FsSyncOption.CLEAR_CACHE)}.
     * <p>
     * These options should be used if an application wants to
     * synchronize all mounted archive files and make sure to clean up
     * <em>all</em> resources, including the selective entry cache.
     * Care should be taken not to use these options while any other thread
     * is still doing I/O to the archive files because otherwise the threads
     * may not be able to succeed and receive an exception.
     * 
     * @since TrueZIP 7.1.1
     */
    public static final BitField<FsSyncOption>
            UMOUNT = BitField.of(   FORCE_CLOSE_INPUT,
                                    FORCE_CLOSE_OUTPUT,
                                    CLEAR_CACHE);

    /**
     * Waits for all other threads to close their I/O resources (i.e. streams,
     * channels etc) for any entries of the file system, flushes its selective
     * entry cache without clearing it, commits all changes to its parent file
     * system (if any) and makes its controller eligible for garbage collection
     * unless any strong references are held by the client application.
     * This is equivalent to
     * {@code BitField.of(FsSyncOption.WAIT_CLOSE_INPUT, FsSyncOption.WAIT_CLOSE_OUTPUT)}.
     * <p>
     * These options should be used if a multithreaded application wants to
     * synchronize all mounted archive files without affecting any I/O to
     * these archive files by any other thread.
     * However, a call with the {@link #UMOUNT} options is still required in
     * order to really clean up <em>all</em> resources, including the
     * selective entry cache.
     * 
     * @since TrueZIP 7.5
     */
    // Note that setting CLEAR_CACHE may cause endless loops when working with
    // nested archive files and there are copy operations where an input stream
    // has been successfully acquired and then acquiring the output stream
    // would require an automatic sync() of the same target archive file from
    // which the input stream is reading.
    public static final BitField<FsSyncOption>
            SYNC = BitField.of( WAIT_CLOSE_INPUT,
                                WAIT_CLOSE_OUTPUT);

    /**
     * Aborts all pending changes for the federated file system, clears the
     * selective cache without flushing it and makes the file system controller
     * eligible for garbage collection unless any strong references are held by
     * the client application.
     * This is equivalent to
     * {@code BitField.of(FsSyncOption.ABORT_CHANGES)}.
     * <p>
     * These options are only meaningful immediately before the federated file
     * system itself gets deleted and should not get used by client
     * applications.
     * 
     * @since TrueZIP 7.5
     */
    public static final BitField<FsSyncOption>
            RESET = BitField.of(ABORT_CHANGES);

    /* Can't touch this - hammer time! */
    private FsSyncOptions() { }
}
