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
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterFederatedFileSystemController<
        E extends Entry,
        FSC extends FederatedFileSystemController<? extends E>>
extends FilterFileSystemController<E, FSC>
implements FederatedFileSystemController<E> {

    protected FilterFederatedFileSystemController(FSC controller) {
        super(controller);
    }

    @Override
    public Icon getOpenIcon() {
        return controller.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() {
        return controller.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() {
        return controller.isReadOnly();
    }

    @Override
    public FileSystemEntry<? extends E> getEntry(String path) {
        return controller.getEntry(path);
    }

    @Override
    public boolean isReadable(String path) {
        return controller.isReadable(path);
    }

    @Override
    public boolean isWritable(String path) {
        return controller.isWritable(path);
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X {
        controller.sync(builder, options);
    }
}
