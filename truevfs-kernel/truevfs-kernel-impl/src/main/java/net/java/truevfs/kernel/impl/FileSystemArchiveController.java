/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;
import net.java.truevfs.kernel.spec.FsArchiveEntry;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.Optional;

/**
 * This abstract archive controller controls the mount state transition.
 * It is up to the sub-class to implement the actual mounting/unmounting strategy.
 * <p>
 * This controller is an emitter of {@link net.java.truecommons.shed.ControlFlowException}s, for example
 * when {@linkplain net.java.truevfs.kernel.impl.NeedsWriteLockException requiring a write lock}.
 *
 * @param <E> the type of the archive entries.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
abstract class FileSystemArchiveController<E extends FsArchiveEntry>
        extends BasicArchiveController<E>
        implements MountState<E>, ReentrantReadWriteLockAspect {

    /**
     * The mount state of the archive file system.
     */
    private MountState<E> mountState = new ResetFileSystem();

    @Override
    public final Optional<ArchiveFileSystem<E>> getFileSystem() {
        return mountState.getFileSystem();
    }

    @Override
    public final void setFileSystem(Optional<ArchiveFileSystem<E>> fileSystem) {
        mountState.setFileSystem(fileSystem);
    }

    @Override
    public final ArchiveFileSystem<E> autoMount(BitField<FsAccessOption> options, boolean autoCreate) throws IOException {
        return mountState.autoMount(options, autoCreate);
    }

    /**
     * Mounts the (virtual) archive file system from the target file.
     * <p>
     * Upon normal termination, this method is expected to have called {@link #setFileSystem(Optional)} to assign the
     * fully initialized file system to this controller.
     * Other than this, the method must not have any side effects on the state of this class or its super class.
     * It may, however, have side effects on the state of the sub class.
     * <p>
     * The implementation may safely assume that the write lock for the file system is acquired.
     *
     * @param options    the options for accessing the file system entry.
     * @param autoCreate If this is {@code true} and the archive file does not exist, then a new archive file system
     *                   with only a virtual root directory is created with its last modification time set to the
     *                   system's current time.
     * @throws IOException on any I/O error.
     */
    abstract void mount(BitField<FsAccessOption> options, boolean autoCreate) throws IOException;

    private final class ResetFileSystem implements MountState<E> {

        @Override
        public Optional<ArchiveFileSystem<E>> getFileSystem() {
            return Optional.empty();
        }

        @Override
        public void setFileSystem(final Optional<ArchiveFileSystem<E>> fileSystem) {
            // Passing in None may happen by sync(*).
            fileSystem.ifPresent(fs -> mountState = new MountedFileSystem(fs));
        }

        @Override
        public ArchiveFileSystem<E> autoMount(final BitField<FsAccessOption> options, final boolean autoCreate) throws IOException {
            checkWriteLockedByCurrentThread();
            mount(options, autoCreate);
            return mountState.getFileSystem().get();
        }
    }

    private final class MountedFileSystem implements MountState<E> {

        final ArchiveFileSystem<E> fs;

        MountedFileSystem(final ArchiveFileSystem<E> fs) {
            this.fs = fs;
        }

        @Override
        public Optional<ArchiveFileSystem<E>> getFileSystem() {
            return Optional.of(fs);
        }

        @Override
        public void setFileSystem(final Optional<ArchiveFileSystem<E>> fileSystem) {
            if (fileSystem.isPresent()) {
                throw new IllegalStateException("File system already mounted!");
            }
            mountState = new ResetFileSystem();
        }

        @Override
        public ArchiveFileSystem<E> autoMount(BitField<FsAccessOption> options, boolean autoCreate) throws IOException {
            return fs;
        }
    }
}
