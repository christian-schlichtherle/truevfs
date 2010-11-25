/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import java.util.EventObject;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileSystemEvent<AE extends ArchiveEntry>
extends EventObject {

    private static final long serialVersionUID = 7205624082374036401L;

    /**
     * Constructs a new archive file system event.
     *
     * @param source the non-{@code null} archive file system source which
     *        caused this event.
     */
    public ArchiveFileSystemEvent(ArchiveFileSystem<AE> source) {
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
    public ArchiveFileSystem<AE> getSource() {
        return (ArchiveFileSystem<AE>) source;
    }
}
