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

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.entry.DecoratingEntry;
import de.schlichtherle.truezip.fs.FsEntryNotFoundException;
import de.schlichtherle.truezip.fs.FsEntry;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.fs.FsNotWriteLockedException;
import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsException;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncExceptionBuilder;
import de.schlichtherle.truezip.rof.ReadOnlyFile;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.util.BitField;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.entry.Entry.Type.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsSyncOption.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;

/**
 * This is the base class for any archive controller, providing all the
 * essential services required for accessing archive files.
 * Each instance of this class manages a globally unique archive file
 * (the <i>target file</i>) in order to allow random access to it as if it
 * were a regular directory in the real file system.
 * <p>
 * In terms of software patterns, an {@code FsController} is
 * similar to a Director in a Builder pattern, with the {@link FsArchiveDriver}
 * interface as its Builder or Abstract Factory.
 * However, an archive controller does not necessarily build a new archive.
 * It may also simply be used to access an existing archive for read-only
 * operations, such as listing its top level directory, or reading entry data.
 * Whatever type of operation it's used for, an archive controller provides
 * and controls <em>all</em> access to any particular archive file by the
 * client application and deals with the rather complex details of its
 * states and transitions.
 * <p>
 * Each instance of this class maintains a (virtual) archive file system,
 * provides input and output streams for the entries of the archive file and
 * methods to synchronize the contents of the archive file system to the target
 * archive file in the parent file system.
 * In cooperation with the calling methods, it also knows how to deal with
 * nested archive files (such as {@code "outer.zip/inner.tar.gz"}
 * and <i>false positives</i>, i.e. plain files or directories or file or
 * directory entries in a parent archive file which have been incorrectly
 * recognized to be <i>prospective archive files</i>.
 * <p>
 * To ensure that for each archive file there is at most one
 * {code FsController}, the path path of the archive file (called
 * <i>target</i>) must be canonicalized, so it doesn't matter whether a target
 * archive file is addressed as {@code "archive.zip"} or
 * {@code "/dir/archive.zip"} if {@code "/dir"} is the client application's
 * current directory.
 * <p>
 * Note that in general all of its methods are reentrant on exceptions.
 * This is important because client applications may repeatedly call them,
 * triggered by the client application. Of course, depending on the context,
 * some or all of the archive file's data may be lost in this case.
 * <p>
 * This class is actually the abstract base class for any archive controller.
 * It encapsulates all the code which is not depending on a particular entry
 * updating strategy and the corresponding state of the controller.
 * Though currently unused, this is intended to be helpful for future
 * extensions of TrueZIP, where different synchronization strategies may be
 * implemented.
 * 
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
abstract class FsBasicArchiveController<E extends FsArchiveEntry>
extends FsController<FsConcurrentModel> {

    private static final Logger
            logger = Logger.getLogger(  FsBasicArchiveController.class.getName(),
                                        FsBasicArchiveController.class.getName());

    private static final BitField<FsOutputOption> AUTO_MOUNT_OPTIONS
            = BitField.noneOf(FsOutputOption.class);

    private static final BitField<FsSyncOption> UNLINK_SYNC_OPTIONS
            = BitField.of(ABORT_CHANGES);

    private final FsConcurrentModel model;

    /**
     * Constructs a new basic archive controller.
     *
     * @param model the non-{@code null} archive model.
     */
    FsBasicArchiveController(final FsConcurrentModel model) {
        if (null == model)
            throw new NullPointerException();
        if (null == model.getParent())
            throw new IllegalArgumentException();
        this.model = model;
    }

    @Override
    public final FsConcurrentModel getModel() {
        return model;
    }

    final FsArchiveFileSystem<E> autoMount() throws IOException {
        return autoMount(false, AUTO_MOUNT_OPTIONS);
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
     * @param autoCreate If the archive file does not exist and this is
     *        {@code true}, a new file system with only a (virtual) root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @return A valid archive file system - {@code null} is never returned.
     * @throws FsFalsePositiveException
     */
    abstract FsArchiveFileSystem<E> autoMount(boolean autoCreate,
                                            BitField<FsOutputOption> options)
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
                                    BitField<Access> types,
                                    long value)
    throws IOException {
        autoSync(name, null);
        return autoMount().setTime(name, types, value);
    }

    @Override
    public final InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsInputOption> options) {
        return new Input(name, options);
    }

    private class Input extends InputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsInputOption> options;

        Input(  final FsEntryName name,
                final BitField<FsInputOption> options) {
            this.name = name;
            this.options = options;
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            if (!autoSync(name, READ)) {
                autoMount();        // detect false positives
                getPeerTarget();    // triggers autoSync() if in same file system
            }
            final FsArchiveFileSystemEntry<E> entry = autoMount().getEntry(name);
            if (null == entry)
                throw new FsEntryNotFoundException(getModel(),
                        name, "no such file or directory");
            return entry.getEntry();
        }

        InputSocket<?> getBoundSocket() throws IOException {
            final Entry entry = getLocalTarget();
            if (DIRECTORY == entry.getType())
                throw new FsEntryNotFoundException(getModel(),
                        name, "cannot read directories");
            return FsBasicArchiveController
                    .this
                    .getInputSocket(entry.getName())
                    .bind(this);
        }

        @Override
        public ReadOnlyFile newReadOnlyFile() throws IOException {
            return getBoundSocket().newReadOnlyFile();
        }

        @Override
        public InputStream newInputStream() throws IOException {
            return getBoundSocket().newInputStream();
        }
    } // class Input

    abstract InputSocket<?> getInputSocket(String name) throws IOException;

    @Override
    public final OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends OutputSocket<Entry> {
        final FsEntryName name;
        final BitField<FsOutputOption> options;
        final @CheckForNull Entry template;

        Output( final FsEntryName name,
                final BitField<FsOutputOption> options,
                final @CheckForNull Entry template) {
            this.name = name;
            this.options = options;
            this.template = template;
        }

        FsArchiveFileSystemOperation<E> mknod() throws IOException {
            autoSync(name, WRITE);
            // Start creating or overwriting the archive entry.
            // This will fail if the entry already exists as a directory.
            return autoMount(   !name.isRoot()
                                && options.get(CREATE_PARENTS), options)
                    .mknod(name, FILE, options, template);
        }

        @Override
        public Entry getLocalTarget() throws IOException {
            final Entry entry = mknod().getTarget().getEntry();
            if (options.get(APPEND)) {
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
            final FsArchiveFileSystemOperation<E> mknod = mknod();
            final E entry = mknod.getTarget().getEntry();
            final OutputSocket<?> output = getOutputSocket(entry);
            InputStream in = null;
            if (options.get(APPEND)) {
                try {
                    in = getInputSocket(entry.getName()).newInputStream();
                } catch (FileNotFoundException ignore) {
                }
            }
            try {
                final OutputStream out = output
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
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        throw new InputException(ex);
                    }
                }
            }
        }
    } // class Output

    private static class ProxyEntry extends DecoratingEntry<Entry> {
        ProxyEntry(Entry entry) {
            super(entry);
        }
    } // class ProxyEntry

    abstract OutputSocket<?> getOutputSocket(E entry) throws IOException;

    @Override
    public final void mknod(
            final FsEntryName name,
            final Type type,
            final BitField<FsOutputOption> options,
            final @CheckForNull Entry template)
    throws IOException {
        if (name.isRoot()) {
            try {
                autoMount(); // detect false positives!
            } catch (FsFalsePositiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true, options);
                return;
            }
            throw new FsEntryNotFoundException(getModel(),
                    name, "directory exists already");
        } else {
            autoMount(options.get(CREATE_PARENTS), options)
                    .mknod(name, type, options, template)
                    .run();
        }
    }

    @Override
    public void unlink(final FsEntryName name) throws IOException {
        autoSync(name, null);
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
                    sync(UNLINK_SYNC_OPTIONS, new FsSyncExceptionBuilder());
                } catch (IOException cannotHappen) {
                    throw new AssertionError(cannotHappen);
                }
                throw ex; // continue with unlinking our target archive file.
            }
            if (!fileSystem.getEntry(ROOT).getMembers().isEmpty())
                throw new IOException("root directory not empty");
            // Check for any archive entries with absolute entry names.
            // Subtract one for the ROOT entry.
            if (1 != fileSystem.getSize())
                logger.log(Level.WARNING, "unlink.absolute",
                        new Object[] {  fileSystem.getSize() - 1,
                                        getModel().getMountPoint() });
            sync(UNLINK_SYNC_OPTIONS, new FsSyncExceptionBuilder());
        } else { // !isRoot(path)
            autoMount().unlink(name);
        }
    }

    /**
     * Synchronizes the archive file only if the archive file has new data for
     * the file system entry with the given path path.
     * <p>
     * <b>Warning:</b> As a side effect,
     * all data structures may get reset (filesystem, entries, streams, etc.)!
     * This method may require synchronization on the write lock!
     *
     * @param  name the non-{@code null} entry name.
     * @param  intention the intended operation on the entry. If {@code null},
     *         a pure file system operation with no I/O is intended.
     * @see    FsController#sync
     * @throws IOException if any exceptional condition occurs
     *         throughout the synchronization of the target archive file.
     * @throws FsNotWriteLockedException
     * @return Whether or not a synchronization has been performed.
     */
    abstract boolean autoSync(  FsEntryName name,
                                @CheckForNull Access intention)
    throws FsSyncException, FsException;
}
