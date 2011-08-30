/*
 * Copyright (C) 2004-2011 Schlichtherle IT Services
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

import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import de.schlichtherle.truezip.entry.Entry.Type;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.FsConcurrentModelController;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.fs.FsEntryName;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import de.schlichtherle.truezip.fs.FsEntryNotFoundException;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsNotSyncedException;
import de.schlichtherle.truezip.fs.FsNotWriteLockedException;
import de.schlichtherle.truezip.fs.FsOutputOption;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.FsSyncOption;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.NotThreadSafe;

/**
 * An abstract base class for any archive file system controller which
 * provide all the essential services required for accessing a prospective
 * archive file.
 * This base class encapsulates all the code which is not depending on a
 * particular archive update strategy and the corresponding state of this
 * file system controller.
 * <p>
 * Each instance of this class manages an archive file - the <i>target file</i>
 * - in order to allow random access to it as if it were a regular directory in
 * its parent file system.
 * <p>
 * Note that in general all of the methods in this class are reentrant on
 * exceptions.
 * This is important because client applications may repeatedly call them.
 * Of course, depending on the calling context, some or all of the archive
 * file's data may be lost in this case.
 * 
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
abstract class FsArchiveController<E extends FsArchiveEntry>
extends FsConcurrentModelController {

    private static final Logger logger = Logger.getLogger(
            FsArchiveController.class.getName(),
            FsArchiveController.class.getName());

    private static final BitField<FsSyncOption>
            UNLINK_SYNC_OPTIONS = BitField.of(ABORT_CHANGES);

    private final ThreadLocal<FsOperationContext>
            context = new ThreadLocal<FsOperationContext>();

    /**
     * Constructs a new basic archive controller.
     *
     * @param model the non-{@code null} archive model.
     */
    FsArchiveController(final FsConcurrentModel model) {
        super(model);
        if (null == model.getParent())
            throw new IllegalArgumentException();
    }

    /**
     * Returns a JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in progress.
     * <p>
     * Note that this is a thread-local property!
     * 
     * @return A JavaBean which represents the original values of selected
     *         parameters for the {@link FsContextController} operation in
     *         progress.
     */
    final FsOperationContext getContext() {
        return context.get();
    }

    /**
     * Sets the JavaBean which represents the original values of selected
     * parameters for the {@link FsContextController} operation in progress.
     * This method should only get called by the class
     * {@link FsContextController}.
     * <p>
     * Note that this is a thread-local property!
     * 
     * @param context the JavaBean which represents the original values of
     *        selected parameters for the {@link FsContextController}
     *        operation in progress.
     * @see   #getContext()
     */
    final void setContext(final @CheckForNull FsOperationContext context) {
        if (null != context)
            this.context.set(context);
        else
            this.context.remove();
    }

    /** Equivalent to {@link #autoMount(boolean) autoMount(false)}. */
    final FsArchiveFileSystem<E> autoMount() throws IOException {
        return autoMount(false);
    }

    /**
     * Returns the (virtual) archive file system mounted from the target
     * archive file. This method is reentrant with respect to any exceptions
     * it may throw.
     * <p>
     * <b>Warning:</b> Either the read or the write lock of this controller
     * must be acquired while this method is called!
     * If only a read lock is acquired, but a write lock is required, this
     * method will temporarily release all locks, so any preconditions must be
     * checked again upon return to protect against concurrent modifications!
     *
     * @param  autoCreate If the archive file does not exist and this is
     *         {@code true}, a new archvie file system with only a (virtual)
     *         root directory is created with its last modification time set
     *         to the system's current time.
     * @return An archive file system.
     */
    abstract FsArchiveFileSystem<E> autoMount(boolean autoCreate)
    throws IOException;

    @Override
    public final boolean isReadOnly() throws IOException {
        return autoMount().isReadOnly();
    }

    @Override
    public final FsEntry getEntry(FsEntryName name)
    throws IOException {
        return autoMount().getEntry(name);
    }

    @Override
    public final boolean isReadable(FsEntryName name) throws IOException {
        return autoMount().getEntry(name) != null;
    }

    @Override
    public final boolean isWritable(FsEntryName name) throws IOException {
        return autoMount().isWritable(name);
    }

    @Override
    public final void setReadOnly(FsEntryName name) throws IOException {
        autoMount().setReadOnly(name);
    }

    @Override
    public final boolean setTime(   FsEntryName name,
                                    Map<Access, Long> times,
                                    BitField<FsOutputOption> options)
    throws IOException {
        checkAccess(name, null);
        return autoMount().setTime(name, times);
    }

    @Override
    public final boolean setTime(   FsEntryName name,
                                    BitField<Access> types,
                                    long value,
                                    BitField<FsOutputOption> options)
    throws IOException {
        checkAccess(name, null);
        return autoMount().setTime(name, types, value);
    }

    @Override
    public final InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsInputOption> options) {
        return new Input(name);
    }

    private final class Input extends InputSocket<FsArchiveEntry> {
        final FsEntryName name;

        Input(final FsEntryName name) {
            this.name = name;
        }

        @Override
        public FsArchiveEntry getLocalTarget() throws IOException {
            getPeerTarget();    // may trigger sync() if in same file system
            checkAccess(name, READ);
            final FsCovariantEntry<E> entry = autoMount().getEntry(name);
            if (null == entry)
                throw new FsEntryNotFoundException(getModel(),
                        name, "no such file or directory");
            return entry.getEntry();
        }

        InputSocket<?> getBoundSocket() throws IOException {
            final FsArchiveEntry entry = getLocalTarget();
            if (FILE != entry.getType())
                throw new FsEntryNotFoundException(getModel(),
                        name, "cannot read directories");
            return FsArchiveController
                    .this
                    .getInputSocket(entry.getName())
                    .bind(this);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public SeekableByteChannel newSeekableByteChannel() throws IOException {
            return getBoundSocket().newSeekableByteChannel();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return getBoundSocket().newInputStream();
        }
    } // Input

    abstract InputSocket<?> getInputSocket(String name);

    @Override
    public final OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private final class Output extends OutputSocket<FsArchiveEntry> {
        final FsEntryName name;
        final boolean append;
        final @CheckForNull Entry template;

        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            this.name = name;
            this.append = options.get(APPEND);
            this.template = template;
        }

        FsArchiveFileSystemOperation<E> mknod() throws IOException {
            checkAccess(name, WRITE);
            final BitField<FsOutputOption> options = getContext()
                    .getOutputOptions();
            // Start creating or overwriting the archive entry.
            // This will fail if the entry already exists as a directory.
            return autoMount(!name.isRoot() && options.get(CREATE_PARENTS))
                    .mknod(name, FILE, options, template);
        }

        @Override
        public FsArchiveEntry getLocalTarget() throws IOException {
            final E entry = mknod().getTarget().getEntry();
            if (append) {
                // A proxy entry must get returned here in order to inhibit
                // a peer target to recognize the type of this entry and
                // change the contents of the transferred data accordingly.
                // This would not work when APPENDing.
                return new ProxyEntry(entry);
            }
            return entry;
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            InputStream in = null;
            if (append) {
                try {
                    in = new Input(name).newInputStream();
                } catch (IOException ex) {
                    // When appending, there is no need for the entry to exist,
                    // so we can safely ignore this - fall through!
                }
            }
            try {
                final FsArchiveFileSystemOperation<E> mknod = mknod();
                final E entry = mknod.getTarget().getEntry();
                final OutputStream out = getOutputSocket(entry)
                        .bind(null == in ? this : null)
                        .newOutputStream();
                try {
                    mknod.run();
                    if (in != null)
                        Streams.cat(in, out);
                } catch (IOException ex) {
                    out.close(); // may throw another exception!
                    throw ex;
                }
                return out;
            } finally {
                if (null != in) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        throw new InputException(ex);
                    }
                }
            }
        }
    } // Output

    private static final class ProxyEntry
    extends DecoratingEntry<FsArchiveEntry>
    implements FsArchiveEntry {
        ProxyEntry(FsArchiveEntry entry) {
            super(entry);
        }

        @Override
        public Type getType() {
            return delegate.getType();
        }

        @Override
        public boolean setSize(Size type, long value) {
            return delegate.setSize(type, value);
        }

        @Override
        public boolean setTime(Access type, long value) {
            return delegate.setTime(type, value);
        }
    } // ProxyEntry

    abstract OutputSocket<?> getOutputSocket(E entry);

    @Override
    public final void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final Entry template)
    throws IOException {
        assert options.equals(getContext().getOutputOptions());
        checkAccess(name, null); // TODO: Explain why this is redundant!
        if (name.isRoot()) {
            try {
                autoMount(); // detect false positives!
            } catch (FsFalsePositiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true);
                return;
            }
            throw new FsEntryNotFoundException(getModel(),
                    name, "directory exists already");
        } else {
            autoMount(options.get(CREATE_PARENTS))
                    .mknod(name, type, options, template)
                    .run();
        }
    }

    @Override
    public void unlink(final FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        checkAccess(name, null);
        if (name.isRoot()) {
            final FsArchiveFileSystem<E> fileSystem;
            try {
                fileSystem = autoMount();
            } catch (FsFalsePositiveException ex) {
                try {
                    // The parent archive controller will unlink our target
                    // archive file next, so we need to reset anyway.
                    // The only effect of calling sync for a false positive
                    // archive file is that it will reset the mount state so
                    // that the file system can be successfully mounted again
                    // if the target archive file is subsequently modified to
                    // be a regular archive file.
                    sync(UNLINK_SYNC_OPTIONS);
                } catch (IOException cannotHappen) {
                    throw new AssertionError(cannotHappen);
                }
                throw ex; // continue with unlinking our target archive file.
            }
            if (!fileSystem.getEntry(ROOT).getMembers().isEmpty())
                throw new IOException("root directory not empty");
            // Check for any archive entries with absolute entry names.
            // Subtract one for the ROOT entry.
            if (1 < fileSystem.getSize())
                logger.log(Level.WARNING, "unlink.absolute",
                        new Object[] {  fileSystem.getSize() - 1,
                                        getMountPoint() });
            sync(UNLINK_SYNC_OPTIONS);
        } else { // !isRoot(path)
            autoMount().unlink(name);
        }
    }

    /**
     * Checks if the intended access to the named archive entry in the virtual
     * file system is possible without performing a
     * {@link FsController#sync(BitField, ExceptionHandler) sync} operation in
     * advance.
     *
     * @param  name the file system entry name.
     * @param  intention the intended I/O operation on the archive entry.
     *         If {@code null}, then only an update to the archive entry meta
     *         data (i.e. a pure virtual file system operation with no I/O)
     *         is intended.
     * @throws IOException if any I/O error occurs when synchronizing the
     *         archive file to its parent file system.
     * @throws FsNotWriteLockedException
     * @throws FsNotSyncedException If a sync operation is required.
     */
    abstract void checkAccess(FsEntryName name, @CheckForNull Access intention)
    throws FsNotWriteLockedException, FsNotSyncedException;
}
