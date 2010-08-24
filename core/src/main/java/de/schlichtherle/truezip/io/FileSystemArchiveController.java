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

package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.io.archive.controller.ArchiveControllerExceptionHandler;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.util.Action;
import java.io.CharConversionException;
import java.io.IOException;

/**
 * This archive controller implements the automounting functionality.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
abstract class FileSystemArchiveController extends ArchiveController {

    /** The mount state of the archive file system. */
    private AutoMounter autoMounter = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(
            java.io.File target,
            ArchiveController enclController,
            String enclEntryName,
            ArchiveDriver driver) {
        super(target, enclController, enclEntryName, driver);
    }

    final boolean isTouched() {
        ArchiveFileSystem fileSystem = getFileSystem();
        return fileSystem != null && fileSystem.isTouched();
    }

    /**
     * Called by this controller's {@link ArchiveFileSystem} to notify it
     * that the file system has been touched.
     * A file system is touched if an operation has been performed on it
     * which modifies it.
     * <p>
     * <b>Warning:</b> The write lock of this controller must
     * be acquired while this method is called!
     */
    void touch() throws IOException {
        assert writeLock().isLockedByCurrentThread();
        setScheduled(true);
    }

    final ArchiveFileSystem autoMount(final boolean create)
    throws IOException {
        assert readLock().isLockedByCurrentThread() || writeLock().isLockedByCurrentThread();
        return autoMounter.autoMount(create);
    }

    final ArchiveFileSystem getFileSystem() {
        return autoMounter.getFileSystem();
    }

    final void setFileSystem(ArchiveFileSystem fileSystem) {
        autoMounter.setFileSystem(fileSystem);
    }

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the sub classes.
     */
    private static abstract class AutoMounter {
        abstract ArchiveFileSystem autoMount(boolean create)
        throws IOException;

        ArchiveFileSystem getFileSystem() {
            return null;
        }

        abstract void setFileSystem(ArchiveFileSystem fileSystem);
    } // class AutoMounter

    private class ResetFileSystem extends AutoMounter {
        ArchiveFileSystem autoMount(final boolean create)
        throws IOException {
            try {
                class Mounter implements Action<IOException> {
                    public void run() throws IOException {
                        // Check state again: Another thread may have changed
                        // it while we released all read locks in order to
                        // acquire the write lock!
                        if (autoMounter == ResetFileSystem.this) {
                            mount(create);
                            assert autoMounter instanceof MountedFileSystem;
                        } else {
                            assert autoMounter != null;
                            assert !(autoMounter instanceof ResetFileSystem);
                        }
                    }
                } // class Mounter

                runWriteLocked(new Mounter());
            } catch (FalsePositiveException ex) {
                // Catch and cache exceptions for uncacheable false positives.
                // The state is reset when File.delete() is called on the false
                // positive archive file or File.update() or File.umount().
                //   This is an important optimization: When hitting a false
                // positive archive file, a client application might perform
                // a lot of tests on it (isDirectory(), isFile(), exists(),
                // length(), etc). If the exception were not cached, each call
                // would run the file system initialization again, only to
                // result in another instance of the same exception type again.
                //   Note that it is important to cache the exceptions for
                // cacheable false positives only: Otherwise, side effects
                // of the archive driver may not be accounted for.
                if (ex.isCacheable())
                    autoMounter = new FalsePositiveFileSystem(ex);
                throw ex;
            }

            assert autoMounter != this;
            // DON'T just call autoMounter.getFileSystem()!
            // This would return null if autoMounter is an instance of
            // FalsePositiveFileSystem.
            return autoMounter.autoMount(create);
        }

        void setFileSystem(ArchiveFileSystem fileSystem) {
            // Passing in null may happen by reset().
            if (fileSystem != null)
                autoMounter = new MountedFileSystem(fileSystem);
        }
    } // class ResetFileSystem

    private class MountedFileSystem extends AutoMounter {
        private final ArchiveFileSystem fileSystem;

        private MountedFileSystem(final ArchiveFileSystem fileSystem) {
            assert fileSystem != null : "It's illegal to use this state with null as the file system!";
            this.fileSystem = fileSystem;
        }

        ArchiveFileSystem autoMount(boolean create)
        throws IOException {
            return fileSystem;
        }

        @Override
        ArchiveFileSystem getFileSystem() {
            return fileSystem;
        }

        void setFileSystem(ArchiveFileSystem fileSystem) {
            assert fileSystem == null : "It's illegal to assign a file system to an archive controller which already has its file system mounted!";
            autoMounter = new ResetFileSystem();
        }
    } // class MountedFileSystem

    private class FalsePositiveFileSystem extends AutoMounter {
        private final FalsePositiveException exception;

        private FalsePositiveFileSystem(final FalsePositiveException exception) {
            assert exception != null : "It's illegal to use this state with null as the IOException!";
            this.exception = exception;
        }

        ArchiveFileSystem autoMount(boolean create)
        throws IOException {
            throw exception;
        }

        void setFileSystem(ArchiveFileSystem fileSystem) {
            assert fileSystem == null : "It's illegal to assign a file system to an archive controller for a false positive archive file!";
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
     * @param create If the archive file does not exist and this is
     *        {@code true}, a new file system with only a virtual root
     *        directory is created with its last modification time set to the
     *        system's current time.
     * @throws FalsePositiveException
     * @throws IOException On any other I/O related issue with the target file
     *         or the target file of any enclosing archive file's controller.
     */
    abstract void mount(boolean create)
    throws IOException;

    void reset(final ArchiveControllerExceptionHandler handler)
    throws ArchiveException {
        setFileSystem(null);
    }

    final ArchiveEntry createArchiveEntry(
            String entryName,
            ArchiveEntry blueprint)
    throws CharConversionException {
        return getDriver().createArchiveEntry(this, entryName, blueprint);
    }
}
