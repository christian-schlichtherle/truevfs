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
import de.schlichtherle.truezip.io.socket.CommonEntry;
import de.schlichtherle.truezip.io.socket.CommonEntry.Access;
import de.schlichtherle.truezip.io.socket.CommonEntry.Type;
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
 * Each instance of this class maintains a file system and provides input and
 * output sockets for its entries.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions - so
 * client applications may repeatedly call them.
 * <p>
 * Where the methods of this class accept a path name string as a parameter,
 * this must be a relative, hierarchical URI which is resolved against the
 * mount point of the file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public interface FileSystemController {

    FileSystemModel getModel();

    Icon getOpenIcon();

    Icon getClosedIcon();

    boolean isReadOnly();

    FileSystemEntry getEntry(String path);

    boolean isReadable(String path);

    boolean isWritable(String path);

    void setReadOnly(String path) throws IOException;

    boolean setTime(String path, BitField<Access> types, long value)
    throws IOException;

    /**
     * Returns an input socket for reading the given entry from the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code InputSocket}.
     */
    InputSocket<?> getInputSocket(String path, BitField<InputOption> options)
    throws IOException;

    /**
     * Returns an output socket for writing the given entry to the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @throws IOException for some other I/O related reason.
     * @return A non-{@code null} {@code InputSocket}.
     */
    OutputSocket<?> getOutputSocket(String path, BitField<OutputOption> options)
    throws IOException;

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given {@code path} into the file system.
     *
     * @param  path a non-{@code null} relative path name.
     * @param  type a non-{@code null} common entry type.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this common entry as possible - with the exception of its name
     *         and type.
     * @param  options if {@code CREATE_PARENTS} is set, any missing parent
     *         directories will be created and linked into this file
     *         system with its last modification time set to the system's
     *         current time.
     * @throws NullPointerException if {@code path} or {@code type} are
     *         {@code null}.
     * @throws IOException for some other I/O related reason, including but
     *         not exclusively upon one or more of the following conditions:
     *         <ul>
     *         <li>The file system is read only.</li>
     *         <li>{@code path} contains characters which are not
     *             supported by the file system.</li>
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.</li>
     *         <li>The new entry already exists as a directory.</li>
     *         <li>The new entry shall be a directory, but already exists.</li>
     *         <li>A parent entry exists but is not a directory.</li>
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.</li>
     *         </ul>
     */
    boolean mknod(  String path, Type type,
                    CommonEntry template,
                    BitField<OutputOption> options)
    throws IOException;

    void unlink(String path) throws IOException;
}
