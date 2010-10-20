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

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This archive controller controls the mount state transition.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FileSystemArchiveController<AE extends ArchiveEntry>
extends        BasicArchiveController     <AE> {

    /** The mount state of the archive file system. */
    private MountState mountState = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(ArchiveModel model) {
        super(model);
    }

    final void assertWriteLockedByCurrentThread()
    throws NotWriteLockedException {
        getModel().assertWriteLockedByCurrentThread();
    }

    @Override
    final ArchiveFileSystem<AE> autoMount(
            final boolean autoCreate,
            final boolean createParents)
    throws IOException {
        assert !createParents || autoCreate;
        return mountState.autoMount(autoCreate, createParents);
    }

    final ArchiveFileSystem<AE> getFileSystem() {
        return mountState.getFileSystem();
    }

    final void setFileSystem(ArchiveFileSystem<AE> fileSystem) {
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
     */
    abstract void mount(boolean autoCreate, boolean createParents)
    throws IOException;

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private abstract class MountState {
        abstract ArchiveFileSystem<AE> autoMount(   boolean autoCreate,
                                                    boolean createParents)
        throws IOException;

        ArchiveFileSystem<AE> getFileSystem() {
            return null;
        }

        abstract void setFileSystem(ArchiveFileSystem<AE> fileSystem);
    } // class AutoMounter

    private class ResetFileSystem extends MountState {
        @Override
        ArchiveFileSystem<AE> autoMount(final boolean autoCreate,
                                        final boolean createParents)
        throws IOException {
            assertWriteLockedByCurrentThread();
            try {
                mount(autoCreate, createParents);
            } catch (FalsePositiveException ex) {
                // Catch and cache exceptions for false positive archive files.
                // The state is reset when unlink() is called on the false
                // positive archive file or sync().
                //   This is an important optimization: When accessing a false
                // positive archive file, a client application might perform
                // a lot of tests on it (isDirectory(), isFile(), isExisting(),
                // getLength(), etc). If the exception were not cached, each call
                // would run the file system initialization again, only to
                // result in another instance of the same exception type again.
                if (!(ex.getCause() instanceof FileNotFoundException))
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
        void setFileSystem(final ArchiveFileSystem<AE> fileSystem) {
            // Passing in null may happen by sync(*).
            if (fileSystem != null)
                mountState = new MountedFileSystem(fileSystem);
        }
    } // class ResetFileSystem

    private class MountedFileSystem extends MountState {
        private final ArchiveFileSystem<AE> fileSystem;

        MountedFileSystem(final ArchiveFileSystem<AE> fileSystem) {
            if (fileSystem == null)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        ArchiveFileSystem<AE> autoMount(    boolean autoCreate,
                                            boolean createParents) {
            return fileSystem;
        }

        @Override
        ArchiveFileSystem<AE> getFileSystem() {
            return fileSystem;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<AE> fileSystem) {
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
        ArchiveFileSystem<AE> autoMount(    boolean autoCreate,
                                            boolean createParents)
        throws FalsePositiveException {
            throw exception;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<AE> fileSystem) {
            mountState = null != fileSystem
                    ? new MountedFileSystem(fileSystem)
                    : new ResetFileSystem();
        }
    } // class FalsePositiveFileSystem
}
