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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.fs.concurrency.FSConcurrencyController;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.fs.FSSyncOption.*;

/**
 * Provides read/write access to one or more file systems which are organized
 * in a chain of responsibility for file system federation.
 * The {@link FSModel#getMountPoint() mount point} of the
 * {@link #getModel() file system model} addresses the file system at the head
 * of this chain of federated file systems.
 * <p>
 * Where the methods of this abstract class accept a
 * {@link FSEntryName file system entry name} as a parameter, this will get
 * resolved against the {@link FSModel#getMountPoint() mount point} URI of this
 * controller's {@link #getModel() file system model}.
 * <p>
 * All method implementations of this abstract class must be reentrant on
 * exceptions - so client applications may repeatedly call them.
 * <p>
 * Though not strictly required, it is recommended that a subclass
 * implementation is thread safe.
 * Otherwise, it's instances must be decorated by a synchronization guard such
 * as {@link FSConcurrencyController}.
 *
 * @param   <M> The type of the file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FsController<M extends FSModel> {

    /** Returns the file system model. */
    public abstract @NonNull M
    getModel();

    /**
     * Returns the controller for the parent file system or {@code null} if
     * and only if this file system is not federated, i.e. not a member of
     * another file system.
     */
    public abstract @Nullable FsController<?>
    getParent();

    public abstract @Nullable Icon
    getOpenIcon()
    throws IOException;

    public abstract @Nullable Icon
    getClosedIcon()
    throws IOException;

    public abstract boolean
    isReadOnly()
    throws IOException;

    public abstract @CheckForNull FSEntry
    getEntry(@NonNull FSEntryName name)
    throws IOException;

    public abstract boolean
    isReadable(@NonNull FSEntryName name)
    throws IOException;

    public abstract boolean
    isWritable(@NonNull FSEntryName name)
    throws IOException;

    public abstract void
    setReadOnly(@NonNull FSEntryName name)
    throws IOException;

    public abstract boolean
    setTime(@NonNull FSEntryName name,
            @NonNull BitField<Access> types,
            long value)
    throws IOException;

    /**
     * Returns an input socket for reading the contents of the entry addressed
     * by the given name from the file system.
     *
     * @param  name a file system entry name.
     * @return An {@code InputSocket}.
     */
    public abstract @NonNull InputSocket<?>
    getInputSocket( @NonNull FSEntryName name,
                    @NonNull BitField<FSInputOption> options);

    /**
     * Returns an output socket for writing the contents of the entry addressed
     * by the given name to the file system.
     *
     * @param  name a file system entry name.
     * @return An {@code OutputSocket}.
     */
    public abstract @NonNull OutputSocket<?>
    getOutputSocket(@NonNull FSEntryName name,
                    @NonNull BitField<FSOutputOption> options,
                    @CheckForNull Entry template);

    /**
     * Creates or replaces and finally links a chain of one or more entries
     * for the given entry {@code name} into the file system.
     *
     * @param  name the file system entry name.
     * @param  type the file system entry type.
     * @param  options the file system output options.
     *         If {@code CREATE_PARENTS} is set, any missing parent directories
     *         will be created and linked into this file system with its last
     *         modification time set to the system's current time.
     * @param  template if not {@code null}, then the file system entry
     *         at the end of the chain shall inherit as much properties from
     *         this entry as possible - with the exception of its name and type.
     * @throws IOException for some other I/O related reason, including but
     *         not exclusively upon one or more of the following conditions:
     *         <ul>
     *         <li>The file system is read only.
     *         <li>{@code name} contains characters which are not
     *             supported by the file system.
     *         <li>TODO: type is not {@code FILE} or {@code DIRECTORY}.
     *         <li>The entry already exists and either the option
     *             {@link FSOutputOption#EXCLUSIVE} is set or the entry is a
     *             directory.
     *         <li>The entry exists as a different type.
     *         <li>A parent entry exists but is not a directory.
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.
     *         </ul>
     */
    public abstract void
    mknod(  @NonNull FSEntryName name,
            @NonNull Type type,
            @NonNull BitField<FSOutputOption> options,
            @CheckForNull Entry template)
    throws IOException;

    public abstract void
    unlink(@NonNull FSEntryName name)
    throws IOException;

    /**
     * Commits all changes to the contents of this file system to its parent
     * file system.
     *
     * @param  options the synchronization options.
     * @param  handler the exception handling strategy for dealing with one or
     *         more input {@code FSSyncException}s which may trigger an {@code X}.
     * @param  <X> the type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IOException at the discretion of the exception {@code handler}.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FORCE_CLOSE_INPUT} is cleared
     *         and {@code FORCE_CLOSE_OUTPUT} is set.
     * @see    #UPDATE
     * @see    #UMOUNT
     */
    public abstract <X extends IOException> void
    sync(   @NonNull BitField<FSSyncOption> options,
            @NonNull ExceptionHandler<? super FSSyncException, X> handler)
    throws X, FSException;

    /**
     * Equivalent to
     * {@code BitField.of(FSSyncOption.FORCE_CLOSE_INPUT, FSSyncOption.FORCE_CLOSE_OUTPUT)}.
     */
    public static final BitField<FSSyncOption> UPDATE
            = BitField.of(FORCE_CLOSE_INPUT, FORCE_CLOSE_OUTPUT);

    /**
     * Equivalent to {@code UPDATE.set(FSSyncOption.CLEAR_CACHE)}.
     */
    public static final BitField<FSSyncOption> UMOUNT = UPDATE.set(CLEAR_CACHE);

    /**
     * Two file system controllers are considered equal if and only if they
     * are identical.
     * This can't get overriden.
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override public final boolean
    equals(Object that) {
        return this == that;
    }

    /**
     * Returns a hash code which is consistent with {@link #equals}.
     * This can't get overriden.
     */
    @Override public final int
    hashCode() {
        return super.hashCode();
    }

    @Override public final String
    toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[model=")
                .append(getModel())
                .append(']')
                .toString();
    }
}
