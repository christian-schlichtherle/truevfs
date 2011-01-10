/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.archive;

import de.schlichtherle.truezip.fs.FsConcurrentModel;
import de.schlichtherle.truezip.fs.FsFalsePositiveException;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.util.BitField;
import java.io.FileNotFoundException;
import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;

/**
 * This archive controller controls the mount state transition.
 * It is up to the sub class to implement the actual mounting/unmounting
 * strategy.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
abstract class FileSystemArchiveController<E extends ArchiveEntry>
extends BasicArchiveController<E> {

    /** The mount state of the archive file system. */
    private MountState<E> mountState = new ResetFileSystem();

    /**
     * Creates a new instance of FileSystemArchiveController
     */
    FileSystemArchiveController(FsConcurrentModel model) {
        super(model);
    }

    @Override
    final ArchiveFileSystem<E> autoMount(
            final boolean autoCreate,
            final BitField<FsOutputOption> options)
    throws IOException {
        return mountState.autoMount(autoCreate, options);
    }

    final ArchiveFileSystem<E> getFileSystem() {
        return mountState.getFileSystem();
    }

    final void setFileSystem(ArchiveFileSystem<E> fileSystem) {
        mountState.setFileSystem(fileSystem);
    }

    /**
     * Mounts the (virtual) archive file system from the target file.
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
     *        {@code true}, a new file system with only a (virtual) root
     *        directory is created with its last modification time set to the
     *        system's current time.
     */
    abstract void mount(boolean autoCreate, BitField<FsOutputOption> options)
    throws IOException;

    /**
     * Represents the mount state of the archive file system.
     * This is an abstract class: The state is implemented in the subclasses.
     */
    private static abstract class MountState<E extends ArchiveEntry> {
        abstract ArchiveFileSystem<E> autoMount(boolean autoCreate,
                                                BitField<FsOutputOption> options)
        throws IOException;

        ArchiveFileSystem<E> getFileSystem() {
            return null;
        }

        abstract void setFileSystem(ArchiveFileSystem<E> fileSystem);
    } // class MountState

    private class ResetFileSystem extends MountState<E> {
        @Override
        ArchiveFileSystem<E> autoMount( final boolean autoCreate,
                                        final BitField<FsOutputOption> options)
        throws IOException {
            getModel().assertWriteLockedByCurrentThread();
            try {
                mount(autoCreate, options);
            } catch (FsFalsePositiveException ex) {
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
            return mountState.autoMount(autoCreate, options);
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<E> fileSystem) {
            // Passing in null may happen by sync(*).
            if (fileSystem != null)
                mountState = new MountedFileSystem(fileSystem);
        }
    } // class ResetFileSystem

    private class MountedFileSystem extends MountState<E> {
        private final ArchiveFileSystem<E> fileSystem;

        MountedFileSystem(final ArchiveFileSystem<E> fileSystem) {
            if (fileSystem == null)
                throw new NullPointerException();
            this.fileSystem = fileSystem;
        }

        @Override
        ArchiveFileSystem<E> autoMount(boolean autoCreate,
                                        BitField<FsOutputOption> options) {
            return fileSystem;
        }

        @Override
        ArchiveFileSystem<E> getFileSystem() {
            return fileSystem;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<E> fileSystem) {
            if (fileSystem != null)
                throw new IllegalArgumentException("File system already mounted!");
            mountState = new ResetFileSystem();
        }
    } // class MountedFileSystem

    private class FalsePositiveFileSystem extends MountState<E> {
        private FsFalsePositiveException exception;

        private FalsePositiveFileSystem(final FsFalsePositiveException exception) {
            if (exception == null)
                throw new NullPointerException();
            this.exception = exception;
        }

        @Override
        ArchiveFileSystem<E> autoMount( boolean autoCreate,
                                        BitField<FsOutputOption> options)
        throws FsFalsePositiveException {
            throw exception;
        }

        @Override
        void setFileSystem(final ArchiveFileSystem<E> fileSystem) {
            mountState = null != fileSystem
                    ? new MountedFileSystem(fileSystem)
                    : new ResetFileSystem();
        }
    } // class FalsePositiveFileSystem
}
