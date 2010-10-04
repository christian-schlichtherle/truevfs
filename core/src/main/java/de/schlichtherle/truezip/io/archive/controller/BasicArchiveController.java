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

import de.schlichtherle.truezip.io.rof.ReadOnlyFile;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type;
import de.schlichtherle.truezip.io.socket.entry.CommonEntry.Access;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocketFactory;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocketFactory;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.EntryOperation;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.APPEND;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.PRESERVE;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.SyncOption.ABORT_CHANGES;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;

/**
 * This is the base class for any archive controller, providing all the
 * essential services required for accessing archive files.
 * Each instance of this class manages a globally unique archive file
 * (the <i>target file</i>) in order to allow random access to it as if it
 * were a regular directory in the real file system.
 * <p>
 * In terms of software patterns, an {@code ArchiveController} is
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
 * {code ArchiveController}, the path name of the archive file (called
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
extends        ArchiveController        <AE                     >
implements     CommonInputSocketFactory <AE                     >,
               CommonOutputSocketFactory<AE                     > {

    BasicArchiveController(ArchiveModel<AE> model) {
        super(model);
    }

    final ArchiveFileSystem<AE> autoMount()
    throws IOException {
        return autoMount(false, false);
    }

    final ArchiveFileSystem<AE> autoMount(boolean autoCreate)
    throws IOException {
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
     * @throws FalsePositiveEntryException
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    abstract ArchiveFileSystem<AE> autoMount(boolean autoCreate, boolean createParents)
    throws IOException;

    /**
     * Returns the driver instance which is used for the target archive.
     * All access to this method must be externally synchronized on this
     * controller's read lock!
     *
     * @return A valid reference to an {@link ArchiveDriver} object
     *         - never {@code null}.
     */
    final ArchiveDriver<AE> getDriver() {
        return getModel().getDriver();
    }

    @Override
    public final Icon getOpenIcon()
    throws FalsePositiveEntryException {
        try {
            autoMount(); // detect false positives!
            return getDriver().getOpenIcon(this);
        } catch (FalsePositiveEntryException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public final Icon getClosedIcon()
    throws FalsePositiveEntryException {
        try {
            autoMount(); // detect false positives!
            return getDriver().getClosedIcon(this);
        } catch (FalsePositiveEntryException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public final boolean isReadOnly()
    throws FalsePositiveEntryException {
        try {
            return autoMount().isReadOnly();
        } catch (FalsePositiveEntryException ex) {
            throw ex;
        } catch (IOException ex) {
            return true;
        }
    }

    @Override
    public final Entry<?> getEntry(final String path)
    throws FalsePositiveEntryException {
        try {
            return autoMount().getEntry(path);
        } catch (FalsePositiveEntryException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws FalsePositiveEntryException {
        try {
            return autoMount().getEntry(path) != null;
        } catch (FalsePositiveEntryException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws FalsePositiveEntryException {
        try {
            return autoMount().isWritable(path);
        } catch (FalsePositiveEntryException ex) {
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
    public final void setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        autoSync(path);
        autoMount().setTime(path, types, value);
    }

    @Override
    public final CommonInputSocket<?> newInputSocket(
            final String path)
    throws IOException {
        class InputSocket extends CommonInputSocket<AE> {
            AE getEntry() throws IOException {
                autoSync(path);
                try {
                    return IOReferences.deref(autoMount().getEntry(path));
                } catch (FalsePositiveEntryException alreadyDetected) {
                    throw new AssertionError(alreadyDetected);
                }
            }

            CommonInputSocket<AE> getInputSocket() throws IOException {
                final AE entry = getEntry();
                if (null != entry && DIRECTORY == entry.getType())
                    throw new EntryNotFoundException(
                            BasicArchiveController.this, path,
                            "cannot read directories");
                final CommonInputSocket<AE> input;
                if (null == entry ||
                        null == (input = BasicArchiveController.this.newInputSocket(entry)))
                    throw new EntryNotFoundException(
                            BasicArchiveController.this, path,
                            "no such file or directory");
                return input.share(this);
            }

            @Override
            protected void afterPeering() {
                getPeerTarget(); // TODO: This can't get removed - explain why!
            }

            @Override
            public AE getTarget() {
                try {
                    return getEntry(); // do NOT cache result - a sync on the same controller may happen any time after return from this method!
                } catch (IOException resolveToNull) {
                    return null; // TODO: interface contract violation!
                }
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                return getInputSocket().newInputStream();
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                return getInputSocket().newReadOnlyFile();
            }
        }

        if (isRoot(path)) {
            try {
                autoMount(); // detect false positives!
            } catch (FalsePositiveEntryException ex) {
                throw ex;
            } catch (EntryNotFoundException ex) {
                if (isRoot(ex.getPath()))
                    throw new FalsePositiveEntryException(this, path, ex);
                throw new FalsePositiveEnclosedEntryException(this, path, ex);
            }
            throw new EntryNotFoundException(this, path,
                    "cannot read directories");
        } else {
            autoMount(); // detect false positives!
            return new InputSocket();
        }
    } // class InputSocket

    /**
     * {@inheritDoc}
     * <p>
     * As an amendment to its interface contract, this method returns
     * {@code null} if no archive input is present.
     */
    @Override
    public abstract CommonInputSocket<AE> newInputSocket(AE target)
    throws IOException;

    @Override
    public final CommonOutputSocket<?> newOutputSocket(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        class OutputSocket extends CommonOutputSocket<AE> {
            EntryOperation<AE> link;

            AE getEntry() throws IOException {
                if (autoSync(path))
                    link = null;
                if (null == link) {
                    try {
                        final CommonEntry template = options.get(PRESERVE)
                                ? getPeerTarget()
                                : null;
                        // Start creating or overwriting the archive entry.
                        // This will fail if the entry already exists as a directory.
                        link = autoMount(options.get(CREATE_PARENTS))
                                .mknod( path, FILE, template,
                                        options.get(CREATE_PARENTS));
                    } catch (FalsePositiveEntryException alreadyDetected) {
                        throw new AssertionError(alreadyDetected);
                    }
                }
                return link.getTarget().getTarget();
            }

            @Override
            protected void beforePeering() {
                link = null; // reset local target reference
            }

            @Override
            protected void afterPeering() {
                getPeerTarget(); // TODO: This can't get removed - explain why!
            }

            @Override
            public AE getTarget() {
                if (options.get(APPEND))
                    return null;
                try {
                    return getEntry();
                } catch (IOException ex) {
                    return null; // TODO: interface contract violation!
                }
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                final AE entry = getEntry();
                final CommonOutputSocket<AE> output = newOutputSocket(entry);
                final InputStream in = options.get(APPEND)
                        ? newInputSocket(entry).newInputStream()
                        : null;
                try {
                    final OutputStream out = output
                            .share(null == in ? this : null)
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
        } // class OutputSocket

        if (isRoot(path)) {
            try {
                autoMount(); // detect false positives!
            } catch (FalsePositiveEntryException ex) {
                throw ex;
            } catch (EntryNotFoundException ex) {
                if (isRoot(ex.getPath()))
                    throw new FalsePositiveEntryException(this, path, ex);
                throw new FalsePositiveEnclosedEntryException(this, path, ex);
            }
            throw new EntryNotFoundException(this, path,
                    "cannot write directories");
        } else {
            autoMount(options.get(CREATE_PARENTS)); // detect false positives!
            return new OutputSocket();
        }
    }

    /**
     * Tests if the file system entry with the given path name has received or
     * is currently receiving new data via an output stream.
     * As an implication, the entry cannot receive new data from another
     * output stream before the next call to {@link #sync}.
     * Note that for directories this method will always return
     * {@code false}!
     */
    //abstract boolean hasNewData(String path);

    @Override
    public abstract CommonOutputSocket<AE> newOutputSocket(AE target)
    throws IOException;

    @Override
    public final void mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<IOOption> options)
    throws IOException {
        if (FILE != type && DIRECTORY != type)
            throw new EntryNotFoundException(this, path,
                    "not yet supported: mknod " + type);
        if (isRoot(path)) {
            try {
                autoMount(); // detect false positives!
            } catch (FalsePositiveEntryException ex) {
                throw ex;
            } catch (EntryNotFoundException ex) {
                switch (type) {
                    case FILE:
                        if (isRoot(ex.getPath()))
                            throw new FalsePositiveEntryException(this, path, ex);
                        throw new FalsePositiveEnclosedEntryException(this, path, ex);

                    case DIRECTORY:
                        autoMount(true, options.get(CREATE_PARENTS));
                }
                return;
            }
            throw new EntryNotFoundException(this, path,
                    "directory exists already");
        } else { // !isRoot(entryName)
            switch (type) {
                case FILE:
                    newOutputSocket(path, options).newOutputStream().close();
                    break;

                case DIRECTORY:
                    autoMount(options.get(CREATE_PARENTS))
                            .mknod(path, DIRECTORY, template, options.get(CREATE_PARENTS))
                            .run();
            }
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public final void unlink(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        autoSync(path);
        if (isRoot(path)) {
            // Get the file system or die trying!
            final ArchiveFileSystem<AE> fileSystem;
            try {
                fileSystem = autoMount();
            } catch (FalsePositiveEntryException ex) {
                // The File instance is going to delete the target file
                // anyway, so we need to reset now.
                try {
                    sync(   new DefaultArchiveSyncExceptionBuilder(),
                            BitField.of(ABORT_CHANGES));
                } catch (ArchiveSyncException cannotHappen) {
                    throw new AssertionError(cannotHappen);
                }
                throw ex;
            }
            // We are actually working on the controller's target file.
            if (!fileSystem.getEntry(path).list().isEmpty())
                throw new IOException("archive file system not empty!");
            sync(   new DefaultArchiveSyncExceptionBuilder(),
                    BitField.of(ABORT_CHANGES));
            // Just in case our target is an RAES encrypted ZIP file,
            // forget it's password as well.
            // TODO: Review: This is an archive driver dependency!
            // Calling it doesn't harm, but please consider a more opaque
            // way to model this, e.g. by calling a listener interface.
            PromptingKeyManager.resetKeyProvider(getMountPoint());
            // Delete the target file or the entry in the enclosing
            // archive file, too.
            throw isHostedDirectoryEntryTarget() // FIXME: Make this method redundant!
                    ? new FalsePositiveEntryException(
                        this, path, new IOException()) // TODO: Consider TransientIOException!
                    : new FalsePositiveEnclosedEntryException(
                        this, path, new IOException()); // dito
        } else { // !isRoot(path)
            autoMount().unlink(path);
        }
    }

    /**
     * Returns {@code true} if and only if this controller's target archive
     * file is <em>not</em> enclosed in another archive file or actually exists
     * as a false positive directory entry in the host file system.
     */
    // TODO: Move to ArchiveModel or UpdatingArchiveController and declare private.
    final boolean isHostedDirectoryEntryTarget() {
        ArchiveModel enclModel = getModel().getEnclModel();
        return null == enclModel || DIRECTORY == enclModel.getTarget().getType();
    }

    /**
     * Synchronizes the archive file only if the archive file has new data for
     * the file system entry with the given path name.
     * <p>
     * <b>Warning:</b> As a side effect,
     * all data structures may get reset (filesystem, entries, streams, etc.)!
     * This method may require synchronization on the write lock!
     *
     * @see    #sync(ArchiveSyncExceptionBuilder, BitField)
     * @throws ArchiveSyncException If any exceptional condition occurs
     *         throughout the synchronization of the target archive file.
     * @throws NotWriteLockedByCurrentThreadException
     * @return Whether or not a synchronization has been performed.
     */
    abstract boolean autoSync(String path) throws ArchiveSyncException;
}
