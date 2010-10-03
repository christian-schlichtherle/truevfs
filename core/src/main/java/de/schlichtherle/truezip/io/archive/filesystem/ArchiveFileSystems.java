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

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntryContainer;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntryFactory;
import java.io.IOException;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.ROOT;

/**
 * Provides static utility methods for archive file systems.
 * This class cannot get instantiated outside its package.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ArchiveFileSystems {

    ArchiveFileSystems() {
    }

    /**
     * Returns a new archive file system and ensures its integrity.
     * The root directory is created with its last modification time set to
     * the system's current time.
     * The file system is modifiable and marked as touched!
     *
     * @param factory the archive entry factory to use.
     * @param vetoableTouchListener the nullable listener for touch events.
     *        If not {@code null}, its {@link VetoableTouchListener#touch()}
     *        method will be called at the end of this constructor and whenever
     *        a client class changes the state of this archive file system.
     * @throws NullPointerException If {@code factory} is {@code null}.
     */
    public static <AE extends ArchiveEntry>
    ArchiveFileSystem<AE> newArchiveFileSystem(
            CommonEntryFactory<AE> factory,
            VetoableTouchListener vetoableTouchListener)
    throws IOException {
        return new ReadWriteArchiveFileSystem<AE>(factory, vetoableTouchListener);
    }

    /**
     * Returns a new archive file system which populates its entries from
     * the given {@code archive} and ensures its integrity.
     * <p>
     * First, the entries from the archive are loaded into the file system.
     * <p>
     * Second, a root directory with the given last modification time is
     * created and linked into the filesystem (so it's never loaded from the
     * archive).
     * <p>
     * Finally, the file system integrity is checked and fixed: Any missing
     * parent directories are created using the system's current time as their
     * last modification time - existing directories will never be replaced.
     * <p>
     * Note that the entries in the file system are shared with the given
     * archive entry {@code container}.
     *
     * @param  container The archive entry container to read the entries for
     *         the population of the file system.
     * @param  rootTemplate The last modification time of the root of the populated
     *         file system in milliseconds since the epoch.
     * @param  factory the archive entry factory to use.
     * @param  vetoableTouchListener the nullable listener for touch events.
     *         If not {@code null}, its {@link VetoableTouchListener#touch()}
     *         method will be called whenever a client class changes the state
     *         of the archive file system.
     * @param  readOnly If and only if {@code true}, any subsequent
     *         modifying operation on the file system will result in a
     *         {@link ReadOnlyArchiveFileSystemException}.
     * @throws NullPointerException If {@code container}, {@code factory} or
     *         {@code rootTemplate} are {@code null}.
     * @throws IllegalArgumentException If {@code rootTemplate} is an instance
     *         of {@link Entry}.
     */
    public static <AE extends ArchiveEntry>
    ArchiveFileSystem<AE> newArchiveFileSystem(
            CommonEntryContainer<AE> container,
            CommonEntryFactory<AE> factory,
            CommonEntry rootTemplate,
            VetoableTouchListener vetoableTouchListener,
            boolean readOnly) {
        return readOnly
            ? new ReadOnlyArchiveFileSystem<AE>(container, factory, rootTemplate)
            : new ReadWriteArchiveFileSystem<AE>(container, factory, rootTemplate, vetoableTouchListener);
    }

    /**
     * Returns {@code true} iff the given path name refers to the root
     * directory.
     */
    public static boolean isRoot(String path) {
        assert 0 == ROOT.length();
        return 0 == path.length();
    }
}
