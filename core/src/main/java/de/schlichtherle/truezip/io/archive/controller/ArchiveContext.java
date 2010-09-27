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

import de.schlichtherle.truezip.io.archive.ArchiveDescriptor;
import de.schlichtherle.truezip.util.concurrent.lock.ReentrantLock;
import java.io.File;

/**
 * Describes the controller context of an archive file.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
interface ArchiveContext extends ArchiveDescriptor {

    /**
     * Returns the context for the enclosing archive file of this
     * context's target archive file or {@code null} if it's not enclosed in
     * another archive file.
     */
    ArchiveContext getEnclContext();

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
}
