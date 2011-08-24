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

import java.util.EventObject;
import net.jcip.annotations.Immutable;

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
