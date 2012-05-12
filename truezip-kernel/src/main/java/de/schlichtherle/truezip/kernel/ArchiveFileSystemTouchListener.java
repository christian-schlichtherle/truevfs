/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsAccessOption;
import de.truezip.kernel.FsArchiveEntry;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import java.util.EventListener;

/**
 * Used to notify implementations of an event in an {@link FsArchiveFileSystem}.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
interface ArchiveFileSystemTouchListener<E extends FsArchiveEntry>
extends EventListener {

    /**
     * Called immediately before the source archive file system is going to
     * get modified (touched) for the first time.
     * If this method throws an {@code IOException}), then the modification
     * is effectively vetoed.
     *
     * @throws IOException at the discretion of the implementation.
     */
    void preTouch(  BitField<FsAccessOption> options,
                    ArchiveFileSystemEvent<? extends E> event)
    throws IOException;

    /**
     * Called immediately after the source archive file system has been
     * modified (touched) for the first time.
     */
    void postTouch( BitField<FsAccessOption> options,
                    ArchiveFileSystemEvent<? extends E> event);
}
