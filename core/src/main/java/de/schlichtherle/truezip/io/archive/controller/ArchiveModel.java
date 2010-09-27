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

import de.schlichtherle.truezip.io.IOOperation;
import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.File;
import java.io.IOException;

/**
 * Defines the common object for accessing and updating an archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
interface ArchiveModel extends ArchiveDescriptor {

    /**
     * Returns the model for the enclosing archive file of this
     * model's target archive file or {@code null} if it's not enclosed in
     * another archive file.
     */
    ArchiveModel getEnclModel();

    /**
     * Resolves the given relative {@code path} against the relative path of
     * the target archive file within its enclosing archive file.
     *
     * @throws NullPointerException if the target archive file is not enclosed
     *         within another archive file.
     */
    String getEnclPath(final String path);

    ArchiveController getController();

    ReentrantLock readLock();

    ReentrantLock writeLock();

    /**
     * Returns {@code true} if and only if the archive file system has been
     * touched, i.e. if an operation changed its state.
     */
    boolean isTouched();

    void setTouched(boolean touched);

    File getTarget();

    /** @deprecated This is a transitional feature helping to refactor the code to an MVC pattern. */
    ArchiveFileSystem<?> autoMount(boolean autoCreate) throws IOException;

    /** @deprecated This is a transitional feature helping to refactor the code to an MVC pattern. */
    <O extends IOOperation> O runWriteLocked(O operation) throws IOException;

    /** @deprecated This is a transitional feature helping to refactor the code to an MVC pattern. */
    boolean hasNewData(String path);

    /** @deprecated This is a transitional feature helping to refactor the code to an MVC pattern. */
    void autoSync(final String path) throws ArchiveSyncException;
}
