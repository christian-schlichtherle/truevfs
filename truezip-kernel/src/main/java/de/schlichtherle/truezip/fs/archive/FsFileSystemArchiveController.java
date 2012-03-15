/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsLockModel;
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
abstract class FsFileSystemArchiveController<E extends FsArchiveEntry>
extends FsArchiveController<E> {

    /** The mount state of the archive file system. */
    private MountState<E> mountState = new ResetFileSystem();

    /**
     * Creates a new instance of FsFileSystemArchiveController
     */
    FsFileSystemArchiveController(FsLockModel model) {
        super(model);
    }

    @Override
    final FsArchiveFileSystem<E> autoMount(final boolean autoCreate)
    throws IOException {
        return mountState.autoMount(autoCreate);
    }

    final @Nullable FsArchiveFileSystem<E> getFileSystem() {
        return mountState.getFileSystem();
    }

    final void setFileSystem(@CheckForNull FsArchiveFileSystem<E> fileSystem) {
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
     *        {@code true}, a new file system with only a (virtual) root
     *        directory is created with its last modification time set to the
     *        system's current time.
     */
    abstract void mount(boolean autoCreate) throws IOException;

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private static abstract class MountState<E extends FsArchiveEntry> {
        abstract FsArchiveFileSystem<E> autoMount(boolean autoCreate)
        throws IOException;

        @Nullable FsArchiveFileSystem<E> getFileSystem() {
            return null;
        }

        abstract void setFileSystem(@CheckForNull FsArchiveFileSystem<E> fileSystem);
    } // MountState

    private final class ResetFileSystem extends MountState<E> {
        @Override
        FsArchiveFileSystem<E> autoMount(final boolean autoCreate)
        throws IOException {
            checkWriteLockedByCurrentThread();
            try {
                mount(autoCreate);
            } catch (FsPersistentFalsePositiveException ex) {
                // Cache exception for false positive file system.
                //   The state is reset when unlink() is called on the false
                // positive file system or sync().
                //   This is an important optimization: When accessing a false
                // positive archive file, a client application might perform
                // a lot of tests on it (isDirectory(), isFile(), isExisting(),
                // getLength(), etc). If the exception were not cached, each call
                // would run the file system initialization again, only to
                // result in another instance of the same exception type again.
                mountState = new FalsePositiveFileSystem(ex);
                //throw ex;
            }

            assert this != mountState;
            // DON'T just call mountState.getFileSystem()!
            // This would return null if mountState is an instance of
            // FalsePositiveFileSystem.
            return mountState.autoMount(autoCreate);
        }

        @Override
        void setFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            // Passing in null may happen by sync(*).
            if (fileSystem != null)
                mountState = new MountedFileSystem(fileSystem);
        }
    } // ResetFileSystem

    private final class MountedFileSystem extends MountState<E> {
        private final FsArchiveFileSystem<E> fileSystem;

        MountedFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            if (null == fileSystem)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        FsArchiveFileSystem<E> autoMount(boolean autoCreate) {
            return fileSystem;
        }

        @Override
        FsArchiveFileSystem<E> getFileSystem() {
            return fileSystem;
        }

        @Override
        void setFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            if (null != fileSystem)
                throw new IllegalArgumentException("File system already mounted!");
            mountState = new ResetFileSystem();
        }
    } // MountedFileSystem

    private final class FalsePositiveFileSystem extends MountState<E> {
        private FsFalsePositiveException exception;

        private FalsePositiveFileSystem(final FsFalsePositiveException exception) {
            if (null == exception)
                throw new NullPointerException();
            this.exception = exception;
        }

        @Override
        FsArchiveFileSystem<E> autoMount(boolean autoCreate)
        throws FsFalsePositiveException {
            throw exception;
        }

        @Override
        void setFileSystem(final FsArchiveFileSystem<E> fileSystem) {
            mountState = null != fileSystem
                    ? new MountedFileSystem(fileSystem)
                    : new ResetFileSystem();
        }
    } // FalsePositiveFileSystem
}
