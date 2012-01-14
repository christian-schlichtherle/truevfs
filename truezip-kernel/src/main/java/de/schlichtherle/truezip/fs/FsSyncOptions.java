/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.Immutable;

/**
 * A static utility class which holds common options for use with
 * {@link FsController#sync(BitField)} and its many 
 * variants and incarnations in the TrueZIP Kernel and client API modules.
 * 
 * @see     FsSyncOption
 * @since   TrueZIP 7.1.1
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public final class FsSyncOptions {

    /**
     * Forcibly closes all input and output resources
     * (input and output streams etc.) for any file system entries
     * and clears the selective entry cache
     * before synchronizing the federated file system with its parent file
     * system.
     * Equivalent to
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
     * Waits for other threads to close their input and output resources
     * (input and output streams etc.) for any file system entries
     * before synchronizing the federated file system with its parent file
     * system.
     * Equivalent to
     * {@code BitField.of(FsSyncOption.WAIT_CLOSE_INPUT, FsSyncOption.WAIT_CLOSE_OUTPUT)}.
     * <p>
     * These options should be used if a multithreaded application wants to
     * synchronize all mounted archive files without affecting any I/O to
     * these archive files by any other thread.
     * However, a call with the {@link #UMOUNT} options is still required in
     * order to really clean up <em>all</em> resources, including the
     * selective entry cache.
     * 
     * @since TrueZIP 7.4.4
     */
    public static final BitField<FsSyncOption>
            SYNC = BitField.of(WAIT_CLOSE_INPUT, WAIT_CLOSE_OUTPUT);

    /**
     * Cancels all pending changes.
     * This option is only meaningful immediately before the federated file
     * system itself gets deleted.
     * Equivalent to
     * {@code BitField.of(FsSyncOption.ABORT_CHANGES)}.
     * <p>
     * These options should not normally be used by applications.
     * 
     * @since TrueZIP 7.4.4
     */
    public static final BitField<FsSyncOption>
            CANCEL = BitField.of(ABORT_CHANGES);

    /** You cannot instantiate this class. */
    private FsSyncOptions() {
    }
}
