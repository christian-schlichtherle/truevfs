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

import de.schlichtherle.truezip.io.TemporarilyNotFoundException;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.entry.CommonEntry.Access;
import de.schlichtherle.truezip.util.Links;
import de.schlichtherle.truezip.io.socket.OutputSocketProvider;
import de.schlichtherle.truezip.io.socket.InputSocketProvider;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Operation;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static de.schlichtherle.truezip.io.archive.controller.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.READ;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Access.WRITE;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.OutputOption.APPEND;
import static de.schlichtherle.truezip.io.socket.OutputOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.socket.OutputOption.COPY_PROPERTIES;

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
 * Each instance of this class maintains a virtual file system, provides input
 * and output streams for the entries of the archive file and methods
 * to update the contents of the virtual file system to the target file
 * in the real file system.
 * In cooperation with the calling methods, it also knows how to deal with
 * nested archive files (such as {@code "outer.zip/inner.tar.gz"}
 * and <i>false positives</i>, i.e. plain files or directories or file or
 * directory entries in an enclosing archive file which have been incorrectly
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
abstract class BasicArchiveController<AE extends ArchiveEntry>
implements     ArchiveController     <AE>,
               InputSocketProvider   <AE>,
               OutputSocketProvider  <AE> {

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
    throws TemporarilyNotFoundException, FalsePositiveException, IOException {
        return autoMount(false, false);
    }

    final ArchiveFileSystem<AE> autoMount(boolean autoCreate)
    throws TemporarilyNotFoundException, FalsePositiveException, IOException {
        return autoMount(autoCreate, autoCreate);
    }

    /**
     * Returns the virtual archive file system mounted from the target file.
     * This method is reentrant with respect to any exceptions it may throw.
     * <p>
     * <b>Warning:</b> Either the read or the write lock of this controller
     * must be acquired while this method is called!
     * If only a read lock is acquired, but a write lock is required, this
     * method will temporarily release all locks, so any preconditions must be
     * checked again upon return to protect against concurrent modifications!
     *
     * @param autoCreate If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @return A valid archive file system - {@code null} is never returned.
     * @throws FalsePositiveException
     */
    abstract ArchiveFileSystem<AE> autoMount(   boolean autoCreate,
                                                boolean createParents)
    throws TemporarilyNotFoundException, FalsePositiveException, IOException;

    @Override
    public final boolean isReadOnly()
    throws NotWriteLockedException, FalsePositiveException {
        try {
            return autoMount().isReadOnly();
        } catch (NotWriteLockedException ex) {
            throw ex;
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws NotWriteLockedException, FalsePositiveException {
        try {
            return autoMount().getEntry(path) != null;
        } catch (NotWriteLockedException ex) {
            throw ex;
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws NotWriteLockedException, FalsePositiveException {
        try {
            return autoMount().isWritable(path);
        } catch (NotWriteLockedException ex) {
            throw ex;
        } catch (FalsePositiveException ex) {
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
            final BitField<InputOption> options)
    throws IOException {
        class Input extends InputSocket<AE> {
            boolean recursion;

            @Override
            public AE getLocalTarget()
            throws IOException {
                if (!autoSync(path, READ) && !recursion) {
                    recursion = true;
                    try {
                        getRemoteTarget(); // force autoSync for remote target!
                    } finally {
                        recursion = false;
                    }
                }
                final AE entry = Links.getTarget(autoMount().getEntry(path));
                if (null == entry)
                    throw new ArchiveEntryNotFoundException(getModel(), path,
                            "no such file or directory");
                return entry;
            }

            InputSocket<? extends AE> getInputSocket()
            throws IOException {
                final AE entry = getLocalTarget();
                if (DIRECTORY == entry.getType())
                    throw new ArchiveEntryNotFoundException(getModel(), path,
                            "cannot read directories");
                return BasicArchiveController.this.getInputSocket(entry).bind(this);
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return getInputSocket().newInputStream();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile()
            throws IOException {
                return getInputSocket().newReadOnlyFile();
            }
        } // class Input

        autoMount(); // detect false positives!
        if (isRoot(path)) {
            throw new ArchiveEntryNotFoundException(getModel(), path,
                    "cannot read directories");
        } else {
            return new Input();
        }
    }

    @Override
    public final OutputSocket<AE> getOutputSocket(
            final String path,
            final BitField<OutputOption> options)
    throws IOException {
        class Output extends OutputSocket<AE> {
            Operation<AE> link;

            AE getEntry()
            throws IOException {
                if (autoSync(path, WRITE))
                    link = null;
                if (null == link) {
                    final CommonEntry template = options.get(COPY_PROPERTIES)
                            ? getRemoteTarget()
                            : null;
                    // Start creating or overwriting the archive entry.
                    // This will fail if the entry already exists as a directory.
                    link = autoMount(options.get(CREATE_PARENTS))
                            .mknod( path, FILE, template,
                                    options.get(CREATE_PARENTS));
                }
                return link.getTarget().getTarget();
            }

            @Override
            protected void afterPeering() {
                link = null; // reset local target reference
            }

            @Override
            public AE getLocalTarget()
            throws IOException {
                if (options.get(APPEND))
                    return null; // FIXME: broken contract
                return getEntry();
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                final AE entry = getEntry();
                final OutputSocket<? extends AE> output = getOutputSocket(entry);
                final InputStream in = options.get(APPEND)
                        ? getInputSocket(entry).newInputStream() // FIXME: Crashes on new entry!
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

        if (isRoot(path)) {
            autoMount(); // detect false positives!
            throw new ArchiveEntryNotFoundException(getModel(), path,
                    "cannot write directories");
        } else {
            autoMount(options.get(CREATE_PARENTS)); // detect false positives!
            return new Output();
        }
    }

    @Override
    public final boolean mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<OutputOption> options)
    throws IOException {
        if (FILE != type && DIRECTORY != type)
            throw new ArchiveEntryNotFoundException(getModel(), path,
                    "not yet supported: mknod " + type);
        if (isRoot(path)) {
            try {
                autoMount(); // detect false positives!
            } catch (FalsePositiveException ex) {
                if (DIRECTORY != type)
                    throw ex;
                autoMount(true, options.get(CREATE_PARENTS));
                return true;
            }
            throw new ArchiveEntryNotFoundException(getModel(), path,
                    "directory exists already");
        } else { // !isRoot(entryName)
            final ArchiveFileSystem<AE> fileSystem
                    = autoMount(options.get(CREATE_PARENTS));
            final boolean created = null == fileSystem.getEntry(path);
            final Operation<AE> link = fileSystem.mknod(
                    path, type, template, options.get(CREATE_PARENTS));
            assert DIRECTORY != type || created : "mknod() must not overwrite directory entries!";
            link.run();
            return created;
        }
    }

    @Override
    public final void unlink(final String path)
    throws IOException {
        autoSync(path, null);
        if (isRoot(path)) {
            final ArchiveFileSystem<AE> fileSystem;
            try {
                fileSystem = autoMount();
            } catch (FalsePositiveException ex) {
                try {
                    // The enclosing archive controller will unlink our target
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
            // Just in case our target is an RAES encrypted ZIP file,
            // forget it's password as well.
            // TODO: Review: This is an archive driver dependency!
            // Calling it doesn't harm, but please consider a more opaque
            // way to model this, e.g. by calling a listener interface.
            PromptingKeyManager.resetKeyProvider(getModel().getMountPoint());
            // Delete the entry in the enclosing controller , too.
            throw new FalsePositiveException(new IOException());
        } else { // !isRoot(path)
            autoMount().unlink(path);
        }
    }

    @Override
    public final boolean isTouched() {
        return getModel().isTouched();
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
    throws SyncException, NotWriteLockedException;
}
