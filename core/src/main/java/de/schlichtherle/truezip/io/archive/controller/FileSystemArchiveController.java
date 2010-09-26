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

import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.io.socket.common.input.CommonInputSocketService;
import de.schlichtherle.truezip.io.socket.common.output.CommonOutputSocketService;
import java.io.IOException;
import java.net.URI;

/**
 * This archive controller implements the automounting functionality.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FileSystemArchiveController<
        AE extends ArchiveEntry,
        AI extends CommonInputSocketService<AE>,
        AO extends CommonOutputSocketService<AE>>
extends BasicArchiveController<AE, AI, AO> {

    /** The mount state of the archive file system. */
    private AutoMounter autoMounter = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(
            URI mountPoint,
            URI enclMountPoint,
            ArchiveDriver driver) {
        super(mountPoint, enclMountPoint, driver);
    }

    @Override
    final boolean isTouched() {
        final ArchiveFileSystem fileSystem = getFileSystem();
        return fileSystem != null && fileSystem.isTouched();
    }

    @Override
    final ArchiveFileSystem<AE> autoMount(
            final boolean autoCreate,
            final boolean createParents)
    throws IOException {
        assert readLock().isHeldByCurrentThread() || writeLock().isHeldByCurrentThread();
        return autoMounter.autoMount(autoCreate, createParents);
    }

    final ArchiveFileSystem<AE> getFileSystem() {
        return autoMounter.getFileSystem();
    }

    final void setFileSystem(ArchiveFileSystem<AE> fileSystem) {
        autoMounter.setFileSystem(fileSystem);
    }

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private abstract class AutoMounter {

        abstract ArchiveFileSystem<AE> autoMount(
                boolean autoCreate,
                boolean createParents)
        throws IOException;

        ArchiveFileSystem<AE> getFileSystem() {
            return null;
        }

        abstract void setFileSystem(ArchiveFileSystem<AE> fileSystem);
    } // class AutoMounter

    private class ResetFileSystem extends AutoMounter {
        @Override
        ArchiveFileSystem<AE> autoMount(final boolean autoCreate, final boolean createParents)
        throws IOException {
            try {
                class Mounter implements IOOperation {
                    @Override
                    public void run() throws IOException {
                        // Check state again: Another thread may have changed
                        // it while we released all read locks in order to
                        // acquire the write lock!
                        if (autoMounter == ResetFileSystem.this) {
                            mount(autoCreate, createParents);
                            //assert autoMounter instanceof MountedFileSystem;
                        } else {
                            assert autoMounter != null;
                            //assert !(autoMounter instanceof ResetFileSystem);
                        }
                    }
                } // class Mounter
                runWriteLocked(new Mounter());
            } catch (FalsePositiveException ex) {
                // Catch and cache exceptions for non-transient false positives.
                // The state is reset when File.delete() is called on the false
                // positive archive file or File.update() or File.sync().
                //   This is an important optimization: When hitting a false
                // positive archive file, a client application might perform
                // a lot of tests on it (isDirectory(), isFile(), isExisting(),
                // getLength(), etc). If the exception were not cached, each call
                // would run the file system initialization again, only to
                // result in another instance of the same exception type again.
                //   Note that it is important to cache the exceptions for
                // non-transient false positives only: Otherwise, side effects
                // of the archive driver may not be accounted for.
                if (!ex.isTransient())
                    autoMounter = new FalsePositiveFileSystem(ex);
                throw ex;
            } catch (IOException ex) {
                throw ex;
            }

            assert autoMounter != this;
            // DON'T just call autoMounter.getFileSystem()!
            // This would return null if autoMounter is an instance of
            // FalsePositiveFileSystem.
            return autoMounter.autoMount(autoCreate, createParents);
        }

        @Override
        void setFileSystem(ArchiveFileSystem<AE> fileSystem) {
            // Passing in null may happen by reset().
            if (fileSystem != null)
                autoMounter = new MountedFileSystem(fileSystem);
        }
    } // class ResetFileSystem

    private class MountedFileSystem extends AutoMounter {
        private final ArchiveFileSystem<AE> fileSystem;

        private MountedFileSystem(final ArchiveFileSystem<AE> fileSystem) {
            if (fileSystem == null)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        ArchiveFileSystem<AE> autoMount(boolean autoCreate, boolean createParents) {
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
            autoMounter = new ResetFileSystem();
        }
    } // class MountedFileSystem

    private class FalsePositiveFileSystem extends AutoMounter {
        private final FalsePositiveException exception;

        private FalsePositiveFileSystem(final FalsePositiveException exception) {
            if (exception == null)
                throw new NullPointerException();
            this.exception = exception;
        }

        @Override
        ArchiveFileSystem<AE> autoMount(boolean autoCreate, boolean createParents)
        throws FalsePositiveException {
            throw exception;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<AE> fileSystem) {
            if (fileSystem != null)
                throw new IllegalArgumentException("False positive archive file cannot have file system!");
            autoMounter = new ResetFileSystem();
        }
    } // class FalsePositiveFileSystem

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
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    abstract void mount(boolean autoCreate, boolean createParents)
    throws IOException;

    @Override
    void reset(final ArchiveSyncExceptionHandler handler)
    throws ArchiveSyncException {
        setFileSystem(null);
    }
}
