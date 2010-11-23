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

import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantReadWriteLock;
import java.net.URI;

import static de.schlichtherle.truezip.io.archive.entry.ArchiveEntry.SEPARATOR;

/**
 * Defines the common properties of any archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class ArchiveModel implements FileSystemModel {
    private final FileSystemModel parentModel;
    private final URI mountPoint;
    private final ReentrantLock readLock;
    private final ReentrantLock writeLock;
    private final TouchListener touchListener;
    private boolean touched;

    ArchiveModel(   final URI mountPoint,
                    final FileSystemModel parent,
                    final TouchListener touchListener) {
        assert "file".equals(mountPoint.getScheme());
        assert !mountPoint.isOpaque();
        assert mountPoint.getPath().endsWith(SEPARATOR);
        assert mountPoint.equals(mountPoint.normalize());

        this.parentModel = parent;
        this.mountPoint = mountPoint;
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        this.touchListener = touchListener;
    }

    @Override
    public FileSystemModel getParentModel() {
        return parentModel;
    }

    @Override
    public URI getMountPoint() {
        return mountPoint;
    }

    @Override
    public boolean isTouched() {
        return touched;
    }

    void setTouched(final boolean newTouched) {
        final boolean oldTouched = touched;
        touched = newTouched;
        if (newTouched != oldTouched)
            notifyTouchListener();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append(":")
                .append(getMountPoint().toString())
                .append("?touched=")
                .append(touched)
                .toString();
    }

    private void notifyTouchListener() {
        if (null != touchListener)
            touchListener.setTouched(touched);
    }

    ReentrantLock readLock() {
        return readLock;
    }

    ReentrantLock writeLock() {
        return writeLock;
    }
}
