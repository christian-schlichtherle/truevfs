/*
 * Copyright (C) 2010 Schlichtherle IT Services
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

import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantReadWriteLock;
import de.schlichtherle.truezip.io.archive.driver.ArchiveDriver;
import de.schlichtherle.truezip.io.archive.descriptor.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import de.schlichtherle.truezip.io.socket.file.FileEntry;
import java.net.URI;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;

/**
 * Defines the common properties for accessing and updating an archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ArchiveModel<AE extends ArchiveEntry>
implements ArchiveDescriptor {

    private final ReentrantLock readLock;
    private final ReentrantLock writeLock;
    private final URI mountPoint;
    private final ArchiveModel<?> enclModel;
    private final URI enclPath;
    private final FileEntry target; // TODO: make this support other virtual file systems.
    private final ArchiveDriver<AE> driver;
    private boolean touched;
    private final TouchListener touchListener;

    ArchiveModel(   final URI mountPoint,
                    final ArchiveDriver<AE> driver,
                    final ArchiveModel<?> enclModel,
                    final TouchListener touchListener) {
        assert "file".equals(mountPoint.getScheme());
        assert !mountPoint.isOpaque();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        assert mountPoint.equals(mountPoint.normalize());
        assert driver != null;

        this.mountPoint = mountPoint;
        if (null == enclModel) {
            this.enclModel = null;
            this.enclPath = null;
        } else {
            this.enclModel = enclModel;
            this.enclPath = enclModel.getMountPoint().relativize(mountPoint);
        }
        this.target = new FileEntry(mountPoint);
        this.driver = driver;
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        this.touchListener = touchListener;
        notifyTouchListener();
    }

    ReentrantLock readLock() {
        return readLock;
    }

    /**
     * @throws NotWriteLockedByCurrentThreadException if the read lock is
     *         held by the current thread.
     */
    void ensureNotReadLockedByCurrentThread(
            final NotWriteLockedByCurrentThreadException ex) {
        if (readLock.isHeldByCurrentThread())
            throw new NotWriteLockedByCurrentThreadException(ex);
    }

    ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * @throws NotWriteLockedByCurrentThreadException if the write lock is not
     *         held by the current thread.
     */
    void ensureWriteLockedByCurrentThread() {
        if (!writeLock.isHeldByCurrentThread())
            throw new NotWriteLockedByCurrentThreadException();
    }

    @Override
    public URI getMountPoint() {
        return mountPoint;
    }

    /** Returns {@code "model:" + }{@link #getMountPoint()}{@code .}{@link Object#toString()}. */
    @Override
    public String toString() {
        return "model:" + getMountPoint().toString();
    }

    ArchiveModel<?> getEnclModel() {
        return enclModel;
    }

    /**
     * Returns the model for the enclosing archive file of this
     * model's target archive file or {@code null} if it's not enclosed in
     * another archive file.
     */
    URI getEnclMountPoint() {
        return null == enclModel ? null : enclModel.getMountPoint();
    }

    /**
     * Resolves the given relative {@code path} against the relative path of
     * the target archive file within its enclosing archive file.
     *
     * @throws NullPointerException if the target archive file is not enclosed
     *         within another archive file.
     */
    String getEnclPath(final String path) {
        return isRoot(path)
                ? cutTrailingSeparators(enclPath.toString(), SEPARATOR_CHAR)
                : enclPath.resolve(path).toString();
    }

    /**
     * Returns the canonical or at least normalized absolute file for the
     * target archive file.
     */
    FileEntry getTarget() {
        return target;
    }

    ArchiveDriver<AE> getDriver() {
        return driver;
    }

    public boolean isTouched() {
        return touched;
    }

    public void setTouched(final boolean newTouched) {
        final boolean oldTouched = touched;
        touched = newTouched;
        if (newTouched != oldTouched)
            notifyTouchListener();
    }

    private void notifyTouchListener() {
        if (null != touchListener)
            touchListener.setTouched(touched);
    }
}
