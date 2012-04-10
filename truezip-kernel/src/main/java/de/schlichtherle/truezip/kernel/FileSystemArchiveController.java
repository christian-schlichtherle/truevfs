/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.kernel;

import de.truezip.kernel.FsAccessOption;
import static de.truezip.kernel.FsAccessOption.EXCLUSIVE;
import de.truezip.kernel.FsArchiveEntry;
import de.truezip.kernel.util.BitField;
import java.io.IOException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * This abstract archive controller controls the mount state transition.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @param  <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
abstract class FileSystemArchiveController<E extends FsArchiveEntry>
extends ArchiveController<E> {

    /** The mount state of the archive file system. */
    private MountState<E> mountState = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(LockModel model) {
        super(model);
    }

    @Override
    final ArchiveFileSystem<E> autoMount(
            boolean autoCreate,
            BitField<FsAccessOption> options)
    throws IOException {
        return mountState.autoMount(autoCreate, options);
    }

    final @Nullable ArchiveFileSystem<E> getFileSystem() {
        return mountState.getFileSystem();
    }

    final void setFileSystem(@CheckForNull ArchiveFileSystem<E> fileSystem) {
        mountState.setFileSystem(fileSystem);
    }

    /**
     * Mounts the (virtual) archive file system from the target file.
     * This method is called while the write lock to mount the file system
     * for this controller is acquired.
     * <p>
     * Upon normal termination, this method is expected to have called
     * {@link #setFileSystem} to assign the fully initialized file system
     * to this controller.
     * Other than this, the method must not have any side effects on the
     * state of this class or its super class.
     * It may, however, have side effects on the state of the sub class.
     *
     * @param autoCreate If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     */
    abstract void mount(boolean autoCreate, BitField<FsAccessOption> options) throws IOException;

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private interface MountState<E extends FsArchiveEntry> {
        ArchiveFileSystem<E> autoMount( boolean autoCreate,
                                        BitField<FsAccessOption> options)
        throws IOException;

        @Nullable ArchiveFileSystem<E> getFileSystem();

        void setFileSystem(@CheckForNull ArchiveFileSystem<E> fileSystem);
    } // MountState

    private final class ResetFileSystem implements MountState<E> {
        @Override
        public ArchiveFileSystem<E> autoMount(
                final boolean autoCreate,
                final BitField<FsAccessOption> options)
        throws IOException {
            checkWriteLockedByCurrentThread();
            mount(autoCreate, options);
            assert this != mountState;
            return mountState.autoMount(autoCreate, options);
        }

        @Override
        public ArchiveFileSystem<E> getFileSystem() {
            return null;
        }

        @Override
        public void setFileSystem(final ArchiveFileSystem<E> fileSystem) {
            // Passing in null may happen by sync(*).
            if (fileSystem != null)
                mountState = new MountedFileSystem(fileSystem);
        }
    } // ResetFileSystem

    private final class MountedFileSystem implements MountState<E> {
        private final ArchiveFileSystem<E> fileSystem;

        MountedFileSystem(final ArchiveFileSystem<E> fileSystem) {
            if (null == fileSystem)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        public ArchiveFileSystem<E> autoMount(
                boolean autoCreate,
                BitField<FsAccessOption> options) {
            return fileSystem;
        }

        @Override
        public ArchiveFileSystem<E> getFileSystem() {
            return fileSystem;
        }

        @Override
        public void setFileSystem(final ArchiveFileSystem<E> fileSystem) {
            if (null != fileSystem)
                throw new IllegalArgumentException("File system already mounted!");
            mountState = new ResetFileSystem();
        }
    } // MountedFileSystem
}
