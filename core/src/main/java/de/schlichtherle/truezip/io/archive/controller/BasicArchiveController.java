/*
 * Copyright (C) 2004-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.archive.controller;

import de.schlichtherle.truezip.io.filesystem.FileSystemEntry;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.filesystem.concurrent.NotWriteLockedException;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemOperation;
import de.schlichtherle.truezip.io.filesystem.concurrent.ConcurrentFileSystemModel;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.filesystem.FalsePositiveException;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncExceptionBuilder;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.filesystem.InputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.filesystem.OutputOption;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jcip.annotations.NotThreadSafe;

import static de.schlichtherle.truezip.io.entry.Entry.Access.*;
import static de.schlichtherle.truezip.io.entry.Entry.Type.*;
import static de.schlichtherle.truezip.io.filesystem.FileSystemEntryName.*;
import static de.schlichtherle.truezip.io.filesystem.SyncOption.*;
import static de.schlichtherle.truezip.io.Paths.*;
import static de.schlichtherle.truezip.io.filesystem.OutputOption.*;

/**
 * This is the base class for any archive controller, providing all the
 * essential services required for accessing archive files.
 * Each instance of this class manages a globally unique archive file
 * (the <i>target file</i>) in order to allow random access to it as if it
 * were a regular directory in the real file system.
 * <p>
 * In terms of software patterns, an {@code FileSystemController} is
 * similar to a Director in a Builder pattern, with the {@link ArchiveDriver}
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
 * {code FileSystemController}, the path path of the archive file (called
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
abstract class BasicArchiveController<E extends ArchiveEntry>
extends FileSystemController<ConcurrentFileSystemModel> {

    private static final String CLASS_NAME
            = BasicArchiveController.class.getName();

    private static final Logger LOGGER
            = Logger.getLogger(CLASS_NAME, CLASS_NAME);

    private static final BitField<OutputOption> AUTO_MOUNT_OPTIONS
            = BitField.noneOf(OutputOption.class);

    private static final BitField<SyncOption> UNLINK_SYNC_OPTIONS
            = BitField.of(ABORT_CHANGES);

    private final ConcurrentFileSystemModel model;

    /**
     * Constructs a new basic archive controller.
     *
     * @param model the non-{@code null} archive model.
     */
    BasicArchiveController(final ConcurrentFileSystemModel model) {
        assert null != model;
        assert null != model.getParent();
        this.model = model;
    }

    @Override
    public final ConcurrentFileSystemModel getModel() {
        return model;
    }

    final ArchiveFileSystem<E> autoMount() throws IOException {
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
     * @throws FalsePositiveException
     */
    abstract ArchiveFileSystem<E> autoMount(boolean autoCreate,
                                            BitField<OutputOption> options)
    throws IOException;

    @Override
    public final boolean isReadOnly() throws IOException {
        return autoMount().isReadOnly();
    }

    @Override
    public final FileSystemEntry getEntry(FileSystemEntryName name)
    throws IOException {
        return autoMount().getEntry(name);
    }

    @Override
    public final boolean isReadable(FileSystemEntryName name) throws IOException {
        return autoMount().getEntry(name) != null;
    }

    @Override
    public final boolean isWritable(FileSystemEntryName name) throws IOException {
        return autoMount().isWritable(name);
    }

    @Override
    public final void setReadOnly(FileSystemEntryName name) throws IOException {
        autoMount().setReadOnly(name);
    }

    @Override
    public final boolean setTime(   FileSystemEntryName name,
                                    BitField<Access> types,
                                    long value)
    throws IOException {
        autoSync(name, null);
        return autoMount().setTime(name, types, value);
    }

    @Override
    public final InputSocket<?> getInputSocket(
            FileSystemEntryName name,
            BitField<InputOption> options) {
        return new Input(name, options);
    }

    private class Input extends InputSocket<E> {
        final FileSystemEntryName name;
        final BitField<InputOption> options;

        Input(  final FileSystemEntryName name,
                final BitField<InputOption> options) {
            this.name = name;
            this.options = options;
        }

        @Override
        public E getLocalTarget() throws IOException {
            if (!autoSync(name, READ)) {
                autoMount();        // detect false positives
                getPeerTarget();    // triggers autoSync() if in same file system
            }
            final ArchiveFileSystemEntry<E> entry = autoMount().getEntry(name);
            if (null == entry)
                throw new ArchiveEntryNotFoundException(getModel(),
                        name, "no such file or directory");
            return entry.getEntry();
        }

        InputSocket<?> getBoundSocket() throws IOException {
            final E entry = getLocalTarget();
            if (DIRECTORY == entry.getType())
                throw new ArchiveEntryNotFoundException(getModel(),
                        name, "cannot read directories");
            return BasicArchiveController
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
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        return new Output(name, options, template);
    }

    private class Output extends OutputSocket<E> {
        final FileSystemEntryName name;
        final BitField<OutputOption> options;
        final Entry template;

        Output( final FileSystemEntryName name,
                final BitField<OutputOption> options,
                final Entry template) {
            this.name = name;
            this.options = options;
            this.template = template;
        }

        /*@Override
        protected void afterPeering() throws IOException {
            if (template != null)
                template = getPeerTarget(); // update connection
        }*/

        ArchiveFileSystemOperation<E> newLink() throws IOException {
            autoSync(name, WRITE);
            // Start creating or overwriting the archive entry.
            // This will fail if the entry already exists as a directory.
            // TODO: Use getPeerTarget() instead of template!
            return autoMount(   !isRoot(name.getPath())
                                && options.get(CREATE_PARENTS), options)
                    .mknod( name, FILE, options.get(CREATE_PARENTS),
                            template);
        }

        @Override
        public E getLocalTarget() throws IOException {
            if (options.get(APPEND)) {
                throw new UnsupportedOperationException("This feature is not yet implemented!");
                // return null; // FIXME: broken interface contract!
            }
            return newLink().getTarget().getEntry();
        }

        @Override
        public OutputStream newOutputStream() throws IOException {
            final ArchiveFileSystemOperation<E> link = newLink();
            final E entry = link.getTarget().getEntry();
            final OutputSocket<?> output = getOutputSocket(entry);
            final InputStream in = options.get(APPEND)
                    ? getInputSocket(entry.getName()).newInputStream() // FIXME: Crashes on new entry!
                    : null;
            try {
                final OutputStream out = output
                        .bind(null == in ? this : null)
                        .newOutputStream();
                try {
                    link.run();
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

    abstract OutputSocket<?> getOutputSocket(E entry) throws IOException;

    @Override
    public final boolean mknod(
            final FileSystemEntryName name,
            final Type type,
            final BitField<OutputOption> options,
            final Entry template)
    throws IOException {
        if (FILE != type && DIRECTORY != type)
            throw new ArchiveEntryNotFoundException(getModel(),
                    name, "not yet supported: mknod " + type);
        if (isRoot(name.getPath())) {
            try {
                autoMount(); // detect false positives!
            } catch (FalsePositiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true, options);
                return true;
            }
            throw new ArchiveEntryNotFoundException(getModel(),
                    name, "directory exists already");
        } else { // !isRoot(entryName)
            final ArchiveFileSystem<E> fileSystem
                    = autoMount(options.get(CREATE_PARENTS), options);
            final boolean created = null == fileSystem.getEntry(name);
            final ArchiveFileSystemOperation<E> link = fileSystem.mknod(
                    name, type, options.get(CREATE_PARENTS), template);
            assert DIRECTORY != type || created : "mknod() must not overwrite directory entries!";
            if (created)
                link.run();
            return created;
        }
    }

    @Override
    public void unlink(final FileSystemEntryName name) throws IOException {
        final String path = name.getPath();
        autoSync(name, null);
        if (isRoot(path)) {
            final ArchiveFileSystem<E> fileSystem;
            try {
                fileSystem = autoMount();
            } catch (FalsePositiveException ex) {
                try {
                    // The parent archive controller will unlink our target
                    // archive file next, so we need to reset anyway.
                    // The only effect of calling sync for a false positive
                    // archive file is that it will reset the mount state so
                    // that the file system can be successfully mounted again
                    // if the target archive file is subsequently modified to
                    // be a regular archive file.
                    sync(UNLINK_SYNC_OPTIONS, new SyncExceptionBuilder());
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
                LOGGER.log(Level.WARNING, "unlink.absolute",
                        new Object[] {  fileSystem.getSize() - 1,
                                        getModel().getMountPoint() });
            sync(UNLINK_SYNC_OPTIONS, new SyncExceptionBuilder());
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
     * @see    FileSystemController#sync
     * @throws IOException if any exceptional condition occurs
     *         throughout the synchronization of the target archive file.
     * @throws NotWriteLockedException
     * @return Whether or not a synchronization has been performed.
     */
    abstract boolean autoSync(FileSystemEntryName name, Access intention)
    throws SyncException, FileSystemException;
}
