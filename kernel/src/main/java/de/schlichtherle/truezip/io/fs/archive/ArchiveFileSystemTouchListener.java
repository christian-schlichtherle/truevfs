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
package de.schlichtherle.truezip.io.fs.archive;

import java.io.IOException;
import java.util.EventListener;

/**
 * Used to notify implementations of an event in an {@link ArchiveFileSystem}.
 *
 * @param   <E> The type of the archive entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
interface ArchiveFileSystemTouchListener<E extends ArchiveEntry>
extends EventListener {

    /**
     * Called immediately before the source archive file system is going to
     * get modified (touched) for the first time.
     * If this method throws an {@code IOException}), then the modification
     * is effectively vetoed.
     *
     * @throws IOException at the discretion of the implementation.
     */
    void beforeTouch(ArchiveFileSystemEvent<? extends E> event)
    throws IOException;

    /**
     * Called immediately after the source archive file system has been
     * modified (touched) for the first time.
     */
    void afterTouch(ArchiveFileSystemEvent<? extends E> event);
}
