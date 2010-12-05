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
        FSC extends FileSystemController<? extends E>>
extends AbstractFileSystemController<E>
implements FileSystemController<E> {

    /** The decorated file system controller. */
    protected FSC controller;

    protected FilterFileSystemController(final FSC controller) {
        this.controller = controller;
    }

    @Override
    public FileSystemModel getModel() {
        return controller.getModel();
    }

    @Override
    public FederatedFileSystemController<?> getParent() {
        return controller.getParent();
    }

    @Override
    public Icon getOpenIcon() throws FileSystemException {
        return controller.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() throws FileSystemException {
        return controller.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() throws FileSystemException {
        return controller.isReadOnly();
    }

    @Override
    public FileSystemEntry<? extends E> getEntry(String path)
    throws FileSystemException {
        return controller.getEntry(path);
    }

    @Override
    public boolean isReadable(String path) throws FileSystemException {
        return controller.isReadable(path);
    }

    @Override
    public boolean isWritable(String path) throws FileSystemException {
        return controller.isWritable(path);
    }

    @Override
    public void setReadOnly(String path) throws IOException {
        controller.setReadOnly(path);
    }

    @Override
    public boolean setTime(String path, BitField<Access> types, long value)
    throws IOException {
        return controller.setTime(path, types, value);
    }

    @Override
    public InputSocket<? extends E> getInputSocket(
            String path,
            BitField<InputOption> options) {
        return controller.getInputSocket(path, options);
    }

    @Override
    public OutputSocket<? extends E> getOutputSocket(
            String path,
            BitField<OutputOption> options,
            Entry template) {
        return controller.getOutputSocket(path, options, template);
    }

    @Override
    public boolean mknod(   String path,
                            Entry.Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        return controller.mknod(path, type, options, template);
    }

    @Override
    public void unlink(String path) throws IOException {
        controller.unlink(path);
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X, FileSystemException {
        controller.sync(builder, options);
    }
}
