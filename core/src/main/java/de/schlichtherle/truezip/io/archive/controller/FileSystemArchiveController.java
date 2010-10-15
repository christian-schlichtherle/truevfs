/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.entry.CommonEntry;

/**
 * This archive controller controls the mount state transition.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FileSystemArchiveController<CE extends CommonEntry>
extends        BasicArchiveController     <CE> {

    /** The mount state of the archive file system. */
    private MountState mountState = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(ArchiveModel model) {
        super(model);
    }

    final void ensureWriteLockedByCurrentThread()
    throws NotWriteLockedException {
        getModel().ensureWriteLockedByCurrentThread();
    }

    @Override
    final ArchiveFileSystem<CE> autoMount(
            final boolean autoCreate,
            final boolean createParents)
    throws FalsePositiveException, NotWriteLockedException {
        assert !createParents || autoCreate;
        return mountState.autoMount(autoCreate, createParents);
    }

    final ArchiveFileSystem<CE> getFileSystem() {
        return mountState.getFileSystem();
    }

    final void setFileSystem(ArchiveFileSystem<CE> fileSystem) {
        mountState.setFileSystem(fileSystem);
    }

    /**
     * Mounts the virtual file system from the target file.
     * This method is called while the write lock to mount the file system
     * for this controller is acquired.
     * <p>
     * Upon normal termination, this method is expected to have called
     * {@link setFileSystem} to assign the fully initialized file system
     * to this controller.
     * Other than this, the method must not have any side effects on the
     * state of this class or its super class.
     * It may, however, have side effects on the state of the sub class.
     *
     * @param autoCreate If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @throws FalsePositiveException
     */
    abstract void mount(boolean autoCreate, boolean createParents)
    throws FalsePositiveException;

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private abstract class MountState {
        abstract ArchiveFileSystem<CE> autoMount(   boolean autoCreate,
                                                    boolean createParents)
        throws FalsePositiveException, NotWriteLockedException;

        ArchiveFileSystem<CE> getFileSystem() {
            return null;
        }

        abstract void setFileSystem(ArchiveFileSystem<CE> fileSystem);
    } // class AutoMounter

    private class ResetFileSystem extends MountState {
        @Override
        ArchiveFileSystem<CE> autoMount(final boolean autoCreate,
                                        final boolean createParents)
        throws FalsePositiveException, NotWriteLockedException {
            ensureWriteLockedByCurrentThread();
            try {
                mount(autoCreate, createParents);
            } catch (FalsePositiveException ex) {
                // Catch and cache exceptions for non-transient false positives.
                // The state is reset when unlink() is called on the false
                // positive archive file or sync().
                //   This is an important optimization: When accessing a false
                // positive archive file, a client application might perform
                // a lot of tests on it (isDirectory(), isFile(), isExisting(),
                // getLength(), etc). If the exception were not cached, each call
                // would run the file system initialization again, only to
                // result in another instance of the same exception type again.
                //   Note that it is important to cache the exceptions for
                // non-transient false positives only: Otherwise, side effects
                // of the archive driver may not be accounted for.
                if (!ex.isTransient())
                    mountState = new FalsePositiveFileSystem(ex);
                throw ex;
            }

            assert this != mountState;
            // DON'T just call autoMounter.getFileSystem()!
            // This would return null if autoMounter is an instance of
            // FalsePositiveFileSystem.
            return mountState.autoMount(autoCreate, createParents);
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<CE> fileSystem) {
            // Passing in null may happen by sync(*).
            if (fileSystem != null)
                mountState = new MountedFileSystem(fileSystem);
        }
    } // class ResetFileSystem

    private class MountedFileSystem extends MountState {
        private final ArchiveFileSystem<CE> fileSystem;

        MountedFileSystem(final ArchiveFileSystem<CE> fileSystem) {
            if (fileSystem == null)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        ArchiveFileSystem<CE> autoMount(    boolean autoCreate,
                                            boolean createParents) {
            return fileSystem;
        }

        @Override
        ArchiveFileSystem<CE> getFileSystem() {
            return fileSystem;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<CE> fileSystem) {
            if (fileSystem != null)
                throw new IllegalArgumentException("File system already mounted!");
            mountState = new ResetFileSystem();
        }
    } // class MountedFileSystem

    private class FalsePositiveFileSystem extends MountState {
        private FalsePositiveException exception;

        private FalsePositiveFileSystem(final FalsePositiveException exception) {
            if (exception == null)
                throw new NullPointerException();
            this.exception = exception;
        }

        @Override
        ArchiveFileSystem<CE> autoMount(    boolean autoCreate,
                                            boolean createParents)
        throws FalsePositiveException, NotWriteLockedException {
            if (!autoCreate)
                throw exception;

            ensureWriteLockedByCurrentThread();
            try {
                mount(autoCreate, createParents);
            } catch (FalsePositiveException ex) {
                throw exception = ex;
            }

            assert this != mountState;
            // DON'T just call autoMounter.getFileSystem()!
            // This would return null if autoMounter is an instance of
            // FalsePositiveFileSystem.
            return mountState.autoMount(autoCreate, createParents);
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<CE> fileSystem) {
            mountState = null != fileSystem
                    ? new MountedFileSystem(fileSystem)
                    : new ResetFileSystem();
        }
    } // class FalsePositiveFileSystem
}
