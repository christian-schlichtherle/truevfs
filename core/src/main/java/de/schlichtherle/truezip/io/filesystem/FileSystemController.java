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

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.filesystem.concurrent.ConcurrentFileSystemController;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;

/**
 * Provides read/write access to one or more file systems which are organized
 * in a chain of responsibility for file system federation.
 * The {@link FileSystemModel#getMountPoint() mount point} of the
 * {@link #getModel() file system model} addresses the file system at the head
 * of this chain of federated file systems.
 * <p>
 * Where the methods of this abstract class accept a
 * {@link FileSystemEntry#getName path name} string as a parameter, this will
 * be resolved against the
 * {@link FileSystemModel#getMountPoint() mount point} URI of this
 * controller's file system.
 * <p>
 * All method implementations of this abstract class must be reentrant on
 * exceptions - so client applications may repeatedly call them.
 * <p>
 * Though not strictly required, it is recommended that a subclass
 * implementation is thread safe.
 * Otherwise, it's instances must be decorated by a synchronization guard such
 * as {@link ConcurrentFileSystemController}.
 *
 * @author  Christian Schlichtherle
 * @version $Id: FileSystemController.java,v 100e4ef190c1 2010/12/24 00:02:30 christian $
 */
public abstract class FileSystemController<M extends FileSystemModel> {

    /** Returns the non-{@code null} file system model. */
    @NonNull
    public abstract M getModel();

    /**
     * Returns the controller for the parent file system or {@code null} if
     * and only if this file system is not federated, i.e. not a member of
     * another file system.
     */
    @Nullable
    public abstract FileSystemController<?> getParent();

    @Nullable
    public abstract Icon getOpenIcon() throws IOException;

    @Nullable
    public abstract Icon getClosedIcon() throws IOException;

    public abstract boolean isReadOnly() throws IOException;

    @Nullable
    public abstract FileSystemEntry getEntry(@NonNull FileSystemEntryName name)
    throws IOException;

    public abstract boolean isReadable(@NonNull FileSystemEntryName name)
    throws IOException;

    public abstract boolean isWritable(@NonNull FileSystemEntryName name)
    throws IOException;

    public abstract void setReadOnly(@NonNull FileSystemEntryName name)
    throws IOException;

    // TODO: Consider putting this into FileSystemEntry and let getEntry()
    // return a proxy instead - mind the IOException however!
    public abstract boolean setTime(@NonNull FileSystemEntryName name,
                                    @NonNull BitField<Access> types,
                                    long value)
    throws IOException;

    /**
     * Returns an input socket for reading the given entry from the file
     * system.
     *
     * @param  name a file system entry name.
     * @return An {@code InputSocket}.
     */
    @NonNull
    public abstract InputSocket<?> getInputSocket(
            @NonNull FileSystemEntryName name,
            @NonNull BitField<InputOption> options);

    /**
     * Returns an output socket for writing the given entry to the file
     * system.
     *
     * @param  name a file system entry name.
     * @return An {@code OutputSocket}.
     */
    @NonNull
    public abstract OutputSocket<?> getOutputSocket(
            @NonNull FileSystemEntryName name,
            @NonNull BitField<OutputOption> options,
            @CheckForNull Entry template);

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given {@code path} into the file system.
     *
     * @param  name a non-{@code null} relative path name.
     * @param  type a non-{@code null} entry type.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
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
    public abstract boolean mknod(
            @NonNull FileSystemEntryName name,
            @NonNull Type type,
            @NonNull BitField<OutputOption> options,
            @Nullable Entry template)
    throws IOException;

    public abstract void unlink(@NonNull FileSystemEntryName name)
    throws IOException;

    /**
     * Writes all changes to the contents of this file system to its
     * parent file system.
     *
     * @param  options the synchronization options.
     * @param  handler the exception handling strategy for dealing with one or
     *         more input {@code SyncException}s which may trigger an {@code X}.
     * @param  <X> the type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FORCE_CLOSE_INPUT} is cleared
     *         and {@code FORCE_CLOSE_OUTPUT} is set.
     * @see    #UPDATE
     * @see    #UMOUNT
     */
    public abstract <X extends IOException>
    void sync(  @NonNull BitField<SyncOption> options,
                @NonNull ExceptionHandler<? super SyncException, X> builder)
    throws X, FileSystemException;

    /**
     * Equivalent to
     * {@code BitField.of(SyncOption.FORCE_CLOSE_INPUT, SyncOption.FORCE_CLOSE_OUTPUT)}.
     */
    public static final BitField<SyncOption> UPDATE
            = BitField.of(FORCE_CLOSE_INPUT, FORCE_CLOSE_OUTPUT);

    /**
     * Equivalent to {@code UPDATE.set(SyncOption.CLEAR_CACHE)}.
     */
    public static final BitField<SyncOption> UMOUNT = UPDATE.set(CLEAR_CACHE);

    /**
     * Two file system controllers are considered equal if and only if they
     * are identical.
     * This can't get overriden.
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public final boolean equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    @Override
    public final String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[model=")
                .append(getModel())
                .append(']')
                .toString();
    }
}
