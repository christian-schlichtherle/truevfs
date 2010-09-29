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

import de.schlichtherle.truezip.util.concurrent.lock.ReentrantReadWriteLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReadWriteLock;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.driver.ArchiveEntry;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.File;
import java.net.URI;

import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR;
import static de.schlichtherle.truezip.io.archive.driver.ArchiveEntry.SEPARATOR_CHAR;
import static de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystems.isRoot;
import static de.schlichtherle.truezip.io.Paths.cutTrailingSeparators;

/**
 * Defines the common properties for accessing and updating an archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
final class ArchiveModel<AE extends ArchiveEntry> implements ArchiveDescriptor {

    private final URI mountPoint;
    private final URI enclMountPoint;
    private final URI enclPath;
    private final ReentrantLock  readLock;
    private final ReentrantLock writeLock;
    private final File target;
    private ArchiveFileSystem<AE> fileSystem;

    ArchiveModel(final URI mountPoint, final URI enclMountPoint) {
        assert "file".equals(mountPoint.getScheme());
        assert !mountPoint.isOpaque();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        assert mountPoint.equals(mountPoint.normalize());
        assert enclMountPoint == null || "file".equals(enclMountPoint.getScheme());
        assert enclMountPoint == null || mountPoint.getPath().startsWith(enclMountPoint.getPath());
        //assert enclMountPoint == null || enclMountPoint.getPath().endsWith(SEPARATOR);

        this.mountPoint = mountPoint;
        this.enclMountPoint = enclMountPoint;
        this.enclPath = null == enclMountPoint
                ? null : enclMountPoint.relativize(mountPoint);
        this.target = new File(mountPoint);
        final ReadWriteLock rwl = new ReentrantReadWriteLock();
        this.readLock  = rwl.readLock();
        this.writeLock = rwl.writeLock();
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

    /**
     * Returns the model for the enclosing archive file of this
     * model's target archive file or {@code null} if it's not enclosed in
     * another archive file.
     */
    URI getEnclMountPoint() {
        return enclMountPoint;
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

    ReentrantLock readLock() {
        return readLock;
    }

    ReentrantLock writeLock() {
        return writeLock;
    }

    /**
     * Returns the canonical or at least normalized absolute file for the
     * target archive file.
     */
    File getTarget() {
        return target;
    }

    ArchiveFileSystem<AE> getFileSystem() {
        return fileSystem;
    }

    void setFileSystem(final ArchiveFileSystem<AE> fileSystem) {
        this.fileSystem = fileSystem;
    }

    /**
     * Returns {@code true} if and only if the archive file system has been
     * touched, i.e. if an operation changed its state.
     */
    boolean isTouched() {
        return null == fileSystem ? false : fileSystem.isTouched();
    }
}
