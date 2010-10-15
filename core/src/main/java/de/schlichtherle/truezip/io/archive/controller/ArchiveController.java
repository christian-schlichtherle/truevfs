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

import de.schlichtherle.truezip.io.archive.filesystem.ArchiveFileSystem.Entry;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
interface ArchiveController<CE extends CommonEntry>
extends FileSystemController<CE> {

    @Override
    ArchiveModel getModel();

    @Override
    Entry<? extends CE> getEntry(String path);

    boolean isTouched();

    /**
     * Writes all changes to the contents of the target archive file to the
     * underlying file system.
     * As a side effect,
     * all data structures get reset (filesystem, entries, streams etc.)!
     * This method requires synchronization on the write lock!
     *
     * @param  options The non-{@code null} options for processing.
     * @throws NullPointerException if {@code options} or {@code builder} is
     * {@code null}.
     * @throws SyncException if any exceptional condition occurs
     * throughout the processing of the target archive file.
     * @see    Controllers#sync(URI, ExceptionBuilder, BitField)
     */
    <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E;
}
