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

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import javax.swing.Icon;

/**
 * Provides read/write access to one or more file systems which are organized
 * in a chain of responsibility for file system federation.
 * The {@link FileSystemModel#getMountPoint() mount point} of the
 * {@link #getModel() file system model} addresses the file system at the head
 * of the chain of federated file systems.
 * <p>
 * Implementations of this interface must not throw a
 * {@link FileSystemException}, but rather resolve the issue internally instead.
 *
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
// FIXME: Push this back to FileSystemController and remove this interface!
public interface FederatedFileSystemController<E extends Entry>
extends FileSystemController<E> {

    @Override
    Icon getOpenIcon();

    @Override
    Icon getClosedIcon();

    @Override
    boolean isReadOnly();

    @Override
    FileSystemEntry<? extends E> getEntry(String path);

    @Override
    boolean isReadable(String path);

    @Override
    boolean isWritable(String path);

    @Override
    <X extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, X> builder,
                BitField<SyncOption> options)
    throws X;
}
