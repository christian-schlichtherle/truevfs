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
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.IOException;
import javax.swing.Icon;

/**
 * @param   <E> The type of the entries.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class FilterFileSystemController<
        E extends Entry,
        C extends FileSystemController<? extends E>>
extends AbstractFileSystemController<E>
implements FileSystemController<E> {

    /** The decorated file system controller. */
    protected final C controller; // FIXME: Encapsulate this!

    protected FilterFileSystemController(final C controller) {
        this.controller = controller;
    }

    @Override
    public FileSystemModel getModel() {
        return controller.getModel();
    }

    @Override
    public FileSystemController<?> getParent() {
        return controller.getParent();
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        return controller.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        return controller.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return controller.isReadOnly();
    }

    @Override
    public FileSystemEntry<? extends E> getEntry(FileSystemEntryName name)
    throws IOException {
        return controller.getEntry(name);
    }

    @Override
    public boolean isReadable(FileSystemEntryName name) throws IOException {
        return controller.isReadable(name);
    }

    @Override
    public boolean isWritable(FileSystemEntryName name) throws IOException {
        return controller.isWritable(name);
    }

    @Override
    public void setReadOnly(FileSystemEntryName name) throws IOException {
        controller.setReadOnly(name);
    }

    @Override
    public boolean setTime(FileSystemEntryName name, BitField<Access> types, long value)
    throws IOException {
        return controller.setTime(name, types, value);
    }

    @Override
    public InputSocket<? extends E> getInputSocket(
            FileSystemEntryName name,
            BitField<InputOption> options) {
        return controller.getInputSocket(name, options);
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        return controller.getOutputSocket(name, options, template);
    }

    @Override
    public boolean mknod(   FileSystemEntryName name,
                            Entry.Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        return controller.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FileSystemEntryName name) throws IOException {
        controller.unlink(name);
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X, FileSystemException {
        controller.sync(builder, options);
    }
}
