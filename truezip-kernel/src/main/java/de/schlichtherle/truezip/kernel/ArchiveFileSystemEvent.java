/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsArchiveEntry;
import java.util.EventObject;
import javax.annotation.concurrent.Immutable;

/**
 * An archive file system event.
 * 
 * @param  <E> The type of the archive entries.
 * @see    FsArchiveFileSystemTouchListener
 * @author Christian Schlichtherle
 */
@Immutable
final class ArchiveFileSystemEvent<E extends FsArchiveEntry>
extends EventObject {
    private static final long serialVersionUID = 7205624082374036401L;

    /**
     * Constructs a new archive file system event.
     *
     * @param source the non-{@code null} archive file system source which
     *        caused this event.
     */
    ArchiveFileSystemEvent(ArchiveFileSystem<E> source) {
        super(source);
    }

    /**
     * Returns the non-{@code null} archive file system source which caused
     * this event.
     *
     * @return The non-{@code null} archive file system source which caused
     *         this event.
     */
    @Override
    @SuppressWarnings("unchecked")
    public ArchiveFileSystem<E> getSource() {
        return (ArchiveFileSystem<E>) source;
    }
}
