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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.archive.controller.Controllers;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface SyncableFileSystemController<CE extends CommonEntry>
extends FileSystemController<CE> {

    @Override
    SyncableFileSystemModel getModel();

    /**
     * Writes all changes to the contents of the file system to its underlying
     * file system.
     *
     * @param  options the non-{@code null} synchronization options.
     * @throws NullPointerException if {@code builder} or {@code options} is
     *         {@code null}.
     * @throws SyncException if any exceptional condition occurs
     *         throughout the synchronization of the file system.
     * @see    SyncableFileSystemModel#isTouched
     * @see    Controllers#sync(URI, ExceptionBuilder, BitField)
     */
    <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E;
}
