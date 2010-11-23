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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import javax.swing.Icon;

/**
 * Provides multi-threaded read/write access to the (virtual) file system
 * addressed by the {@link FileSystemModel#getMountPoint() mount point} of
 * its associated {@link #getModel() file system model}.
 * <p>
 * Each instance of this class maintains a (virtual) file system and provides
 * input and output sockets for its entries.
 * <p>
 * Note that the methods of this interface are reentrant on exceptions - so
 * client applications may repeatedly call them.
 * <p>
 * Where the methods of this interface accept a path name string as a
 * parameter, this must be a relative, hierarchical URI which is resolved
 * against the {@link FileSystemModel#getMountPoint() mount point} of the
 * (virtual) file system.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemController<CE extends CommonEntry>
extends CompositeFileSystemController<CE> {

    @Override
    Icon getOpenIcon();

    @Override
    Icon getClosedIcon();

    @Override
    boolean isReadOnly();

    @Override
    FileSystemEntry<? extends CE> getEntry(String path);

    @Override
    boolean isReadable(String path);

    @Override
    boolean isWritable(String path);
}
