/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
