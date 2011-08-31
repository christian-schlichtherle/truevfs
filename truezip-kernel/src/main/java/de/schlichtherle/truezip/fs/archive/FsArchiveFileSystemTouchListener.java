/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import java.io.IOException;
import java.util.EventListener;

/**
 * Used to notify implementations of an event in an {@link FsArchiveFileSystem}.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
interface FsArchiveFileSystemTouchListener<E extends FsArchiveEntry>
extends EventListener {

    /**
     * Called immediately before the source archive file system is going to
     * get modified (touched) for the first time.
     * If this method throws an {@code IOException}), then the modification
     * is effectively vetoed.
     *
     * @throws IOException at the discretion of the implementation.
     */
    void beforeTouch(FsArchiveFileSystemEvent<? extends E> event)
    throws IOException;

    /**
     * Called immediately after the source archive file system has been
     * modified (touched) for the first time.
     */
    void afterTouch(FsArchiveFileSystemEvent<? extends E> event);
}
