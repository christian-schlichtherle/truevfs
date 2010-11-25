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
package de.schlichtherle.truezip.io.archive.filesystem;

import de.schlichtherle.truezip.io.archive.entry.ArchiveEntry;
import java.io.IOException;
import java.util.EventListener;

/**
 * Used to notify implementations of an event in an {@link ArchiveFileSystem}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface ArchiveFileSystemListener<AE extends ArchiveEntry>
extends EventListener {

    /**
     * Called whenever the value of the property
     * {@link ArchiveFileSystem#isTouched() touched} is going to change in the
     * source archive file system model.
     * If this method throws an {@code IOException}), the modification of the
     * archive file system is vetoed.
     *
     * @throws IOException at the discretion of the implementation.
     */
    void beforeTouch(ArchiveFileSystemEvent<AE> event) throws IOException;
}
