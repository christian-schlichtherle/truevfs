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
 * Like it's super class, but does not throw {@link FileSystemException}s
 * because either the file system is not federated or this exception type is
 * handled by the implementation of this abstract class.
 *
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class ComponentFileSystemController<E extends Entry>
extends FileSystemController<E> {

    @Override
    public abstract Icon getOpenIcon();

    @Override
    public abstract Icon getClosedIcon();

    @Override
    public abstract boolean isReadOnly();

    @Override
    public abstract FileSystemEntry<? extends E> getEntry(String path);

    @Override
    public abstract boolean isReadable(String path);

    @Override
    public abstract boolean isWritable(String path);

    @Override
    public abstract <E extends IOException>
    void sync(  ExceptionBuilder<? super SyncException, E> builder,
                BitField<SyncOption> options)
    throws E;
}
