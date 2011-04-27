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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import javax.swing.Icon;

/**
 * An abstract class which provides read/write access to a file system.
 * This class may be organized in a chain of responsibility for file system
 * federation.
 * The {@link FsModel#getMountPoint() mount point} of the
 * {@link #getModel() file system model} addresses the file system at the head
 * of this chain of federated file systems.
 * <p>
 * Where the methods of this abstract class accept a
 * {@link FsEntryName file system entry name} as a parameter, this will get
 * resolved against the {@link FsModel#getMountPoint() mount point} URI of this
 * controller's {@link #getModel() file system model}.
 * <p>
 * Sub-classes must be reentrant on exceptions - so users may repeatedly
 * call their methods.
 *
 * @param   <M> The type of the file system model.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class FsController<M extends FsModel> {

    /** Returns the file system model. */
    public abstract M getModel();

    /**
     * Returns the controller for the parent file system or {@code null} if
     * and only if this file system is not federated, i.e. not a member of
     * another file system.
     */
    public abstract @Nullable FsController<?> getParent();

    public abstract @Nullable Icon getOpenIcon()
    throws IOException;

    public abstract @Nullable Icon getClosedIcon()
    throws IOException;

    public abstract boolean isReadOnly()
    throws IOException;

    public abstract @CheckForNull FsEntry getEntry(FsEntryName name)
    throws IOException;

    public abstract boolean isReadable(FsEntryName name)
    throws IOException;

    public abstract boolean isWritable(FsEntryName name)
    throws IOException;

    public abstract void setReadOnly(FsEntryName name)
    throws IOException;

    public abstract boolean setTime(FsEntryName name,
                                    BitField<Access> types,
                                    long value)
    throws IOException;

    /**
     * Returns an input socket for reading the contents of the entry addressed
     * by the given name from the file system.
     *
     * @param  name a file system entry name.
     * @return An {@code InputSocket}.
     */
    public abstract InputSocket<?>
    getInputSocket( FsEntryName name,
                    BitField<FsInputOption> options);

    /**
     * Returns an output socket for writing the contents of the entry addressed
     * by the given name to the file system.
     * If {@code template} is not {@code null}, then the output entry shall
     * have as many of its properties copied as reasonable, e.g. the last
     * modification time.
     *
     * @param  name a file system entry name.
     * @param  template a nullable template for the properties of the output
     *         entry.
     * @return An {@code OutputSocket}.
     */
    public abstract OutputSocket<?>
    getOutputSocket(FsEntryName name,
                    BitField<FsOutputOption> options,
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
     *         <li>The entry already exists and either the option
     *             {@link FsOutputOption#EXCLUSIVE} is set or the entry is a
     *             directory.
     *         <li>The entry exists as a different type.
     *         <li>A parent entry exists but is not a directory.
     *         <li>A parent entry is missing and {@code createParents} is
     *             {@code false}.
     *         </ul>
     */
    public abstract void
    mknod(  FsEntryName name,
            Type type,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template)
    throws IOException;

    public abstract void
    unlink(FsEntryName name)
    throws IOException;

    /**
     * Commits all unsynchronized changes to the contents of this file system
     * to its parent file system, releases the associated resources for access
     * by third parties (e.g. other processes) and cleans up any temporary
     * resources.
     * Note that temporary resources may get used even if this file systems
     * was accessed read-only.
     * If this is not a federated file system, i.e. if its not a member of a
     * parent file system, then nothing happens.
     * Otherwise, the state of this file system controller is reset.
     * <p>
     * This method calls {@link #sync sync(options, builder)}, where builder is
     * an instance of {@link FsSyncExceptionBuilder}.
     * If the call succeeds, the builder's {@link FsSyncExceptionBuilder#check}
     * method is called to check out any {@link FsSyncWarningException}, too.
     *
     * @param  options the synchronization options.
     * @throws FsSyncException if committing the changes fails for any reason.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if {@code FORCE_CLOSE_INPUT} is cleared
     *         and {@code FORCE_CLOSE_OUTPUT} is set.
     */
    public final void
    sync(BitField<FsSyncOption> options)
    throws FsSyncException {
        FsSyncExceptionBuilder builder = new FsSyncExceptionBuilder();
        sync(options, builder);
        builder.check();
    }

    /**
     * Commits all unsynchronized changes to the contents of this file system
     * to its parent file system, releases the associated resources for access
     * by third parties (e.g. other processes) and cleans up any temporary
     * resources.
     * Note that temporary resources may get used even if this file systems
     * was accessed read-only.
     * If this is not a federated file system, i.e. if its not a member of a
     * parent file system, then nothing happens.
     * Otherwise, the state of this file system controller is reset.
     *
     * @param  options the synchronization options.
     * @param  handler the exception handling strategy for dealing with one or
     *         more input {@code FsSyncException}s which may trigger an {@code X}.
     * @param  <X> the type of the {@code IOException} to throw at the
     *         discretion of the exception {@code handler}.
     * @throws IllegalArgumentException if the combination of synchronization
     *         options is illegal, e.g. if
     *         {@code FsSyncOption.FORCE_CLOSE_INPUT} is cleared and
     *         {@code FsSyncOption.FORCE_CLOSE_OUTPUT} is set.
     */
    public abstract <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws X;

    /**
     * Two file system controllers are considered equal if and only if they
     * are identical.
     * This can't get overriden.
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public final boolean equals(@CheckForNull Object that) {
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

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[model=")
                .append(getModel())
                .append(']')
                .toString();
    }
}
