/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import java.util.EventObject;
import javax.annotation.concurrent.Immutable;

/**
 * An archive file system event.
 * 
 * @param   <E> The type of the archive entries.
 * @see     FsArchiveFileSystemTouchListener
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
final class FsArchiveFileSystemEvent<E extends FsArchiveEntry>
extends EventObject {
    private static final long serialVersionUID = 7205624082374036401L;

    /**
     * Constructs a new archive file system event.
     *
     * @param source the non-{@code null} archive file system source which
     *        caused this event.
     */
    FsArchiveFileSystemEvent(FsArchiveFileSystem<E> source) {
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
    public FsArchiveFileSystem<E> getSource() {
        return (FsArchiveFileSystem<E>) source;
    }
}
