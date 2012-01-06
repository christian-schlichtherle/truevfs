/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.util.Link;

/**
 * Represents an operation on a chain of one or more archive entries.
 * The operation is run by its {@link #run} method and the head of the
 * chain can be obtained by its {@link #getTarget} method.
 * <p>
 * Note that the state of the archive file system will not change until
 * the {@link #run} method is called!
 * <p>
 * Implementations do <em>not</em> need to be thread-safe.
 *
 * @param   <E> The type of the archive entries.
 * @see     FsArchiveFileSystem#mknod
 * @author  Christian Schlichtherle
 * @version $Id$
 */
interface FsArchiveFileSystemOperation<E extends FsArchiveEntry>
extends Link<FsCovariantEntry<E>> {

    /** Executes this archive file system operation. */
    void run() throws FsArchiveFileSystemException;
}
