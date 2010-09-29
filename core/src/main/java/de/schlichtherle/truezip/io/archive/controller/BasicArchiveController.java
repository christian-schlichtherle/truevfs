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
import de.schlichtherle.truezip.io.socket.entry.FilterCommonEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystemEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.socket.IOReferences;
import de.schlichtherle.truezip.io.socket.output.CommonOutputProvider;
import de.schlichtherle.truezip.io.socket.input.CommonInputProvider;
import de.schlichtherle.truezip.io.socket.output.CommonOutputSocket;
import de.schlichtherle.truezip.io.socket.input.CommonInputSocket;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.EntryOperation;
import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.InputException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.io.socket.IOReference;
import de.schlichtherle.truezip.key.PromptingKeyManager;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import javax.swing.Icon;

import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.APPEND;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.CREATE_PARENTS;
import static de.schlichtherle.truezip.io.archive.controller.ArchiveController.IOOption.PRESERVE;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.DIRECTORY;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.FILE;
import static de.schlichtherle.truezip.io.socket.entry.CommonEntry.Type.SPECIAL;
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
abstract class BasicArchiveController<AE extends ArchiveEntry>
extends     ArchiveController<AE>
implements  CommonInputProvider<AE>,
            CommonOutputProvider<AE> {

    BasicArchiveController(ArchiveModel<AE> model) {
        super(model);
    }

    // TODO: Document this!
    abstract int waitAllInputStreamsByOtherThreads(long timeout);

    // TODO: Document this!
    abstract int waitAllOutputStreamsByOtherThreads(long timeout);

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     */
    final void reset() throws ArchiveSyncException {
        final ArchiveSyncExceptionBuilder builder
                = new DefaultArchiveSyncExceptionBuilder();
        reset(builder);
        builder.check();
    }

    /**
     * Resets the archive controller to its initial state - all changes to the
     * archive file which have not yet been updated get lost!
     * Thereafter, the archive controller will behave as if it has just been
     * created and any subsequent operations on its entries will remount
     * the virtual file system from the archive file again.
     * <p>
     * This method should be overridden by subclasses, but must still be
     * called when doing so.
     */
    abstract void reset(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException;

    @Override
    public CommonInputSocket<? extends CommonEntry> getInputSocket(String path)
    throws IOException {
        assert path != null;

        try {
            return getInputSocket0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getInputSocket(getEnclPath(path));
        }
    }

    private CommonInputSocket<? extends CommonEntry> getInputSocket0(
            final String path)
    throws IOException {
        class InputSocket extends CommonInputSocket<AE> {
            IOReference<AE> link = this;

            AE getEntry() throws IOException {
                if (hasNewData(path)) {
                    link = this;
                    class AutoSync implements IOOperation {
                        @Override
                        public void run() throws IOException {
                            autoSync(path);
                        }
                    }
                    runWriteLocked(new AutoSync());
                }
                if (this == link) {
                    try {
                        link = autoMount().getEntry(path);
                    } catch (FalsePositiveException alreadyDetected) {
                        throw new AssertionError(alreadyDetected);
                    }
                }
                return IOReferences.deref(link);
            }

            CommonInputSocket<AE> getInputSocket() throws IOException {
                final AE entry = getEntry();
                if (null != entry && DIRECTORY == entry.getType())
                    throw new ArchiveEntryNotFoundException(
                            BasicArchiveController.this, path,
                            "cannot read directories");
                final CommonInputSocket<AE> input;
                if (null == entry ||
                        null == (input = BasicArchiveController.this.newInputSocket(entry)))
                    throw new ArchiveEntryNotFoundException(
                            BasicArchiveController.this, path,
                            "no such file or directory");
                return input.chain(this);
            }

            @Override
            protected void beforeConnectComplete() {
                link = this; // reset local target reference
            }

            @Override
            protected void afterConnectComplete() {
                getTarget();
            }

            @Override
            public AE getTarget() {
                readLock().lock();
                try {
                    try {
                        return getEntry();
                    } catch (IOException resolveToNull) {
                        return null; // FIXME: interface contract violation
                    }
                } finally {
                    readLock().unlock();
                }
            }

            @Override
            public InputStream newInputStream()
            throws IOException {
                readLock().lock();
                try {
                    return getInputSocket().newInputStream();
                } finally {
                    readLock().unlock();
                }
            }

            @Override
            public ReadOnlyFile newReadOnlyFile() throws IOException {
                readLock().lock();
                try {
                    return getInputSocket().newReadOnlyFile();
                } finally {
                    readLock().unlock();
                }
            }
        }

        assert path != null;

        readLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    autoMount(); // detect false positives!
                } catch (FalsePositiveException ex) {
                    throw ex;
                } catch (ArchiveEntryNotFoundException ex) {
                    if (isRoot(ex.getPath()))
                        throw new FalsePositiveException(this, path, ex);
                    // TODO: throw new ArchiveEntryFalsePositiveException(ex); ?!?! archive entry not found is not really an archive entry false positive ?!?!
                    return getEnclController().getInputSocket(getEnclPath(path));
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "cannot read directories");
            } else {
                autoMount(); // detect false positives!
                return new InputSocket();
            }
        } finally {
            readLock().unlock();
        }
    }

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
    public CommonOutputSocket<? extends CommonEntry> getOutputSocket(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        assert path != null;

        try {
            return getOutputSocket0(path, options);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getOutputSocket(
                    getEnclPath(path), options);
        }
    }

    private CommonOutputSocket<? extends CommonEntry> getOutputSocket0(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        class OutputSocket extends CommonOutputSocket<AE> {
            EntryOperation<AE> link;

            AE getEntry() throws IOException {
                if (hasNewData(path)) {
                    link = null;
                    autoSync(path);
                }
                if (null == link) {
                    try {
                        // Start creating or overwriting the archive entry.
                        // This will fail if the entry already exists as a directory.
                        link = autoMount(options.get(CREATE_PARENTS))
                                .mknod(path, FILE,
                                    options.get(PRESERVE)
                                        ? getPeerTarget()
                                        : null,
                                    options.get(CREATE_PARENTS));
                    } catch (FalsePositiveException alreadyDetected) {
                        throw new AssertionError(alreadyDetected);
                    }
                }
                return link.getTarget().getTarget();
            }

            @Override
            protected void beforeConnectComplete() {
                link = null; // reset local target reference
            }

            @Override
            protected void afterConnectComplete() {
                getTarget();
            }

            @Override
            public AE getTarget() {
                if (options.get(APPEND))
                    return null;
                class GetTarget implements IOOperation {
                    AE entry;

                    @Override
                    public void run() throws IOException {
                        entry = getEntry();
                    }
                }
                try {
                    return (AE) runWriteLocked(new GetTarget()).entry;
                } catch (IOException ex) {
                    return null; // FIXME: interface contract violation
                }
            }

            @Override
            public OutputStream newOutputStream()
            throws IOException {
                class NewOutputStream implements IOOperation {
                    OutputStream out;

                    @Override
                    public void run() throws IOException {
                        final AE entry = getEntry();
                        final CommonOutputSocket<AE> output
                                = newOutputSocket(entry);
                        final InputStream in = options.get(APPEND)
                                ? newInputSocket(entry).newInputStream()
                                : null;
                        try {
                            out = output
                                    .chain(null != in ? null : OutputSocket.this)
                                    .newOutputStream();
                            try {
                                link.run();
                                if (in != null)
                                    Streams.cat(in, out);
                            } catch (IOException ex) {
                                out.close(); // may throw another exception!
                                throw ex;
                            }
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
                }
                return runWriteLocked(new NewOutputStream()).out;
            }
        }

        assert path != null;

        writeLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    autoMount(); // detect false positives!
                } catch (FalsePositiveException ex) {
                    throw ex;
                } catch (ArchiveEntryNotFoundException ex) {
                    if (isRoot(ex.getPath()))
                        throw new FalsePositiveException(this, path, ex);
                    // TODO: throw new ArchiveEntryFalsePositiveException(ex); ??? not found is not really a false positive ???
                    return getEnclController().getOutputSocket(
                            getEnclPath(path), options);
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "cannot write directories");
            } else {
                autoMount(options.get(CREATE_PARENTS)); // detect false positives!
                return new OutputSocket();
            }
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public abstract CommonOutputSocket<AE> newOutputSocket(AE target)
    throws IOException;

    @Override
    public final Icon getOpenIcon()
    throws FalsePositiveException {
        try {
            return getOpenIcon0();
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getOpenIcon();
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private Icon getOpenIcon0()
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            autoMount(); // detect false positives!
            return getDriver().getOpenIcon(this);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final Icon getClosedIcon()
    throws FalsePositiveException {
        try {
            return getClosedIcon0();
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getClosedIcon();
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private Icon getClosedIcon0()
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            autoMount(); // detect false positives!
            return getDriver().getClosedIcon(this);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final boolean isReadOnly()
    throws FalsePositiveException {
        try {
            return isReadOnly0();
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isReadOnly();
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return true;
        }
    }

    private boolean isReadOnly0()
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().isReadOnly();
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final Entry<?> getEntry(final String path)
    throws FalsePositiveException {
        try {
            return getEntry0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().getEntry(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return null;
        }
    }

    private Entry<?> getEntry0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().getEntry(path);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            /** @see ArchiveDriver#newInputShop! */
            if (isRoot(path) && ex.getCause() instanceof FileNotFoundException)
                return new SpecialFileEntry<ArchiveEntry>(getEnclController()
                        .getEntry(getEnclPath(path))
                        .getTarget()); // the exception asserts that the entry exists as a file!
            throw ex;
        } finally {
            readLock().unlock();
        }
    }

    private static final class SpecialFileEntry<AE extends ArchiveEntry>
    extends FilterCommonEntry<AE>
    implements Entry<AE> {
        SpecialFileEntry(AE target) {
            super(target);
        }

        @Override
        public Type getType() {
            assert FILE == super.getType();
            return SPECIAL;
        }

        @Override
        public Set<String> list() {
            return null;
        }

        @Override
        public AE getTarget() {
            return target;
        }
    }

    @Override
    public final boolean isReadable(final String path)
    throws FalsePositiveException {
        try {
            return isReadable0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isReadable(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isReadable0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().getEntry(path) != null;
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final boolean isWritable(final String path)
    throws FalsePositiveException {
        try {
            return isWritable0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            return getEnclController().isWritable(getEnclPath(path));
        } catch (FalsePositiveException ex) {
            throw ex;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean isWritable0(final String path)
    throws FalsePositiveException, IOException {
        readLock().lock();
        try {
            return autoMount().isWritable(path);
        } finally {
            readLock().unlock();
        }
    }

    @Override
    public final void setReadOnly(final String path)
    throws IOException {
        try {
            setReadOnly0(path);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().setReadOnly(getEnclPath(path));
        }
    }

    private void setReadOnly0(final String path)
    throws IOException {
        writeLock().lock();
        try {
            autoMount().setReadOnly(path);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final void setTime(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        try {
            setTime0(path, types, value);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().setTime(getEnclPath(path), types, value);
        }
    }

    private void setTime0(
            final String path,
            final BitField<Access> types,
            final long value)
    throws IOException {
        writeLock().lock();
        try {
            autoSync(path);
            autoMount().setTime(path, types, value);
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    public final void mknod(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<IOOption> options)
    throws IOException {
        try {
            mknod0(path, type, template, options);
        } catch (ArchiveEntryFalsePositiveException ex) {
            getEnclController().mknod(getEnclPath(path), type, template, options);
        } catch (FalsePositiveException ex) {
            throw ex;
        }
    }

    private void mknod0(
            final String path,
            final Type type,
            final CommonEntry template,
            final BitField<IOOption> options)
    throws FalsePositiveException, IOException {
        if (FILE != type && DIRECTORY != type)
            throw new ArchiveEntryNotFoundException(this, path,
                    "not yet supported: mknod " + type);
        writeLock().lock();
        try {
            if (isRoot(path)) {
                try {
                    autoMount(); // detect false positives!
                } catch (FalsePositiveException ex) {
                    throw ex;
                } catch (ArchiveEntryNotFoundException ex) {
                    switch (type) {
                        case FILE:
                            if (isRoot(ex.getPath()))
                                throw new FalsePositiveException(this, path, ex);
                            // TODO: throw new ArchiveEntryFalsePositiveException(ex); ??? not found is not really a false positive ???
                            getEnclController().mknod(
                                    getEnclPath(path), type, template, options);
                            break;

                        case DIRECTORY:
                            autoMount(true, options.get(CREATE_PARENTS));
                    }
                    return;
                }
                throw new ArchiveEntryNotFoundException(this, path,
                        "directory exists already");
            } else { // !isRoot(entryName)
                switch (type) {
                    case FILE:
                        getOutputSocket0(path, options)
                                .newOutputStream()
                                .close();
                        break;

                    case DIRECTORY:
                        autoMount(options.get(CREATE_PARENTS))
                                .mknod(path, DIRECTORY, template, options.get(CREATE_PARENTS))
                                .run();
                }
            }
        } finally {
            writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("ThrowableInitCause")
    public final void unlink(
            final String path,
            final BitField<IOOption> options)
    throws IOException {
        try {
            unlink0(path, options);
        } catch (DirectoryArchiveEntryFalsePositiveException ex) {
            getEnclController().unlink(getEnclPath(path), options);
        } catch (FileArchiveEntryFalsePositiveException ex) {
            /** @see ArchiveDriver#newInputShop! */
            // FIXME: What if we remove this special case? We could probably delete a RAES encrypted ZIP file with an unknown password. Would we want this?
            if (isRoot(path)) {
                final ArchiveFileSystemEntry entry = getEnclController().getEntry(getEnclPath(path));
                if (null == entry || entry.getType() != DIRECTORY
                    && ex.getCause() instanceof FileNotFoundException) {
                    throw (IOException) new IOException(ex.toString()).initCause(ex); // mask!
                }
            }
            getEnclController().unlink(getEnclPath(path), options);
        }
    }

    private void unlink0(final String path, final BitField<IOOption> options)
    throws IOException {
        writeLock().lock();
        try {
            autoSync(path);
            if (isRoot(path)) {
                // Get the file system or die trying!
                final ArchiveFileSystem<AE> fileSystem;
                try {
                    fileSystem = autoMount();
                } catch (FalsePositiveException ex) {
                    // The File instance is going to delete the target file
                    // anyway, so we need to reset now.
                    try {
                        reset();
                    } catch (ArchiveSyncException cannotHappen) {
                        throw new AssertionError(cannotHappen);
                    }
                    throw ex;
                }
                // We are actually working on the controller's target file.
                if (!fileSystem.getEntry(path).list().isEmpty())
                    throw new IOException("archive file system not empty!");
                final int outputStreams = waitAllOutputStreamsByOtherThreads(50);
                // TODO: Review: This policy may be changed - see method start.
                assert outputStreams <= 0
                        : "Entries for open output streams should not be deletable!";
                // Note: CommonEntry for open input streams ARE deletable!
                final int inputStreams = waitAllInputStreamsByOtherThreads(50);
                if (inputStreams > 0 || outputStreams > 0)
                    throw new IOException("archive file has open streams!");
                reset();
                // Just in case our target is an RAES encrypted ZIP file,
                // forget it's password as well.
                // TODO: Review: This is an archive driver dependency!
                // Calling it doesn't harm, but please consider a more opaque
                // way to model this, e.g. by calling a listener interface.
                PromptingKeyManager.resetKeyProvider(getMountPoint());
                // Delete the target file or the entry in the enclosing
                // archive file, too.
                if (isHostFileSystemEntryTarget()) { // FIXME: Do not use this method!
                    // The target file of the controller is NOT enclosed
                    // in another archive file.
                    if (!getTarget().delete())
                        throw new IOException("couldn't delete archive file!");
                } else {
                    // The target file of the controller IS enclosed in
                    // another archive file.
                    getEnclController().unlink(getEnclPath(path), options);
                }
            } else { // !isRoot(path)
                autoMount().unlink(path);
            }
        } finally {
            writeLock().unlock();
        }
    }
}
