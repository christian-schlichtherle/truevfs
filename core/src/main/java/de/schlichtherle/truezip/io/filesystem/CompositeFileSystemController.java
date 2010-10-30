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

import de.schlichtherle.truezip.io.archive.controller.Archives;
import de.schlichtherle.truezip.io.entry.CommonEntry;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;

/**
 * Provides the methods to describe a composite file system and synchronize its
 * (virtual) file system with its parent file system.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public interface CompositeFileSystemController<CE extends CommonEntry>
extends          FileSystemController         <CE                    > {

    /** Returns the non-{@code null} composite file system model. */
    @Override
    CompositeFileSystemModel getModel();

    /**
     * Writes all changes to the contents of this composite file system to its
     * parent file system.
     *
     * @param  <E> the type of the assembled {@code IOException} to throw.
     * @param  builder the non-{@code null} exception builder to use for the
     *         assembly of an {@code IOException} from the given
     *         {@code SyncException}s.
     * @param  options the non-{@code null} synchronization options.
     * @throws NullPointerException if {@code builder} or {@code options} is
     *         {@code null}.
     * @throws IOException if any exceptional condition occurs throughout the
     *         synchronization of this composite file system.
     * @see    CompositeFileSystemModel#isTouched
     * @see    Archives#sync
     */
    <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E;
}
