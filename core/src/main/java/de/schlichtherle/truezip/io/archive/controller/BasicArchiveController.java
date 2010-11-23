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

import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.DefaultSyncExceptionBuilder;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.util.Links;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Operation;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.filesystem.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.READ;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;

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
 * {code FileSystemController}, the path name of the archive file (called
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
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class BasicArchiveController   <AE extends ArchiveEntry>
extends        AbstractArchiveController<AE> {

    private final ArchiveModel model;

    /**
     * Constructs a new basic archive controller.
     *
     * @param model the non-{@code null} archive model.
     */
    BasicArchiveController(final ArchiveModel model) {
        assert null != model;
        this.model = model;
    }

    @Override
    public final ArchiveModel getModel() {
        return model;
    }

    final ArchiveFileSystem<AE> autoMount()
    throws IOException {
        return autoMount(false, BitField.noneOf(OutputOption.class));
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
    abstract ArchiveFileSystem<AE> autoMount(   boolean autoCreate,
                                                BitField<OutputOption> options)
    throws IOException;

    @Override
    public final boolean isReadOnly()
    throws FileSystemException {
        try {
            return autoMount().isReadOnly();
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws FileSystemException {
        try {
            return autoMount().getEntry(path) != null;
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws FileSystemException {
        try {
            return autoMount().isWritable(path);
        } catch (FileSystemException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public final void setReadOnly(final String path)
    throws IOException {
        autoMount().setReadOnly(path);
    }

    @Override
    public final boolean setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        autoSync(path, null);
        return autoMount().setTime(path, types, value);
    }

    @Override
    public final InputSocket<AE> getInputSocket(
            final String path,
            final BitField<InputOption> options) {
        class Input extends InputSocket<AE> {
            boolean recursion;

            @Override
            public AE getLocalTarget() throws IOException {
                if (!autoSync(path, READ) && !recursion) {
                    autoMount(); // detect false positives!
                    recursion = true;
                    try {
                        getPeerTarget(); // force autoSync for peer target!
                    } finally {
                        recursion = false;
                    }
                }
                final AE entry = Links.getTarget(autoMount().getEntry(path));
                if (null == entry)
                    throw new ArchiveEntryNotFoundException(getModel(),
                            path, "no such file or directory");
                return entry;
            }

            final InputSocket<? extends AE> getBoundSocket() throws IOException {
                final AE entry = getLocalTarget();
                if (DIRECTORY == entry.getType())
                    throw new ArchiveEntryNotFoundException(getModel(),
                            path, "cannot read directories");
                return BasicArchiveController.this.getInputSocket(entry.getName()).bind(this);
            }

            @Override
            public InputStream newInputStream() throws IOException {
                return getBoundSocket().newInputStream();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return getBoundSocket().newReadOnlyFile();
            }
        } // class Input

        return new Input();
    }

    abstract InputSocket<? extends AE> getInputSocket(String name) throws IOException;

    @Override
    public final OutputSocket<AE> getOutputSocket(
            final String path,
            final BitField<OutputOption> options,
            final CommonEntry template) {
        class Output extends OutputSocket<AE> {
            Operation<AE> link;

            AE getEntry() throws IOException {
                if (autoSync(path, WRITE))
                    link = null;
                if (null == link) {
                    // Start creating or overwriting the archive entry.
                    // This will fail if the entry already exists as a directory.
                    link = autoMount(!isRoot(path) && options.get(CREATE_PARENTS),
                                     options)
                            .mknod( path, FILE,
                                    options.get(CREATE_PARENTS), template);
                }
                return link.getTarget().getTarget();
            }

            @Override
            protected void afterPeering() {
                link = null; // reset local target reference
            }

            @Override
            public AE getLocalTarget() throws IOException {
                if (options.get(APPEND))
                    return null; // FIXME: broken interface contract!
                return getEntry();
            }

            @Override
            public OutputStream newOutputStream() throws IOException {
                final AE entry = getEntry();
                final OutputSocket<? extends AE> output = getOutputSocket(entry);
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

        return new Output();
    }

    abstract OutputSocket<? extends AE> getOutputSocket(AE entry) throws IOException;

    @Override
    public final boolean mknod(
            final String path,
            final Type type,
            final BitField<OutputOption> options,
            final CommonEntry template)
    throws IOException {
        if (FILE != type && DIRECTORY != type)
            throw new ArchiveEntryNotFoundException(getModel(),
                    path, "not yet supported: mknod " + type);
        if (isRoot(path)) {
            try {
                autoMount(); // detect false positives!
            } catch (FalsePositiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true, options);
                return true;
            }
            throw new ArchiveEntryNotFoundException(getModel(),
                    path, "directory exists already");
        } else { // !isRoot(entryName)
            final ArchiveFileSystem<AE> fileSystem
                    = autoMount(options.get(CREATE_PARENTS), options);
            final boolean created = null == fileSystem.getEntry(path);
            final Operation<AE> link = fileSystem.mknod(
                    path, type, options.get(CREATE_PARENTS), template);
            assert DIRECTORY != type || created : "mknod() must not overwrite directory entries!";
            if (created)
                link.run();
            return created;
        }
    }

    @Override
    public void unlink(final String path) throws IOException {
        autoSync(path, null);
        if (isRoot(path)) {
            final ArchiveFileSystem<AE> fileSystem;
            try {
                fileSystem = autoMount();
            } catch (FalsePositiveException ex) {
                try {
                    // The parent archive controller will unlink our target
                    // archive file next, so we need to reset anyway.
                    sync(   new DefaultSyncExceptionBuilder(),
                            BitField.of(ABORT_CHANGES));
                } catch (IOException cannotHappen) {
                    throw new AssertionError(cannotHappen);
                }
                throw ex; // continue with unlinking our target archive file.
            }
            if (!fileSystem.getEntry(path).getMembers().isEmpty())
                throw new IOException("root directory not empty");
            sync(   new DefaultSyncExceptionBuilder(),
                    BitField.of(ABORT_CHANGES));
        } else { // !isRoot(path)
            autoMount().unlink(path);
        }
    }

    /**
     * Synchronizes the archive file only if the archive file has new data for
     * the file system entry with the given path name.
     * <p>
     * <b>Warning:</b> As a side effect,
     * all data structures may get reset (filesystem, entries, streams, etc.)!
     * This method may require synchronization on the write lock!
     *
     * @param  path the path name of the entry
     * @param  intention the intended operation on the entry. If {@code null},
     *         a pure file system operation with no I/O is intended.
     * @see    ArchiveController#sync(ExceptionBuilder, BitField)
     * @throws SyncException If any exceptional condition occurs
     *         throughout the synchronization of the target archive file.
     * @throws NotWriteLockedException
     * @return Whether or not a synchronization has been performed.
     */
    abstract boolean autoSync(String path, Access intention)
    throws SyncException, FileSystemException;
}
