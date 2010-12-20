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
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

/**
 * Decorates another file system controller.
 * <p>
 * This class is thread-safe if and only if the decorated file system
 * controller and its parent file system controller are thread-safe.
 *
 * @param   <M> The type of the file system model.
 * @param   <C> The type of the decorated file system controller.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class DecoratingFileSystemController<
        M extends FileSystemModel,
        C extends FileSystemController<? extends M>>
extends FileSystemController<M> {

    /** The decorated file system controller. */
    protected final C delegate;

    private volatile M model;

    /**
     * Constructs a new decorating file system controller.
     *
     * @param controller the decorated file system controller.
     */
    protected DecoratingFileSystemController(@NonNull final C controller) {
        if (null == controller)
            throw new NullPointerException();
        this.delegate = controller;
    }

    @Override
    public final M getModel() {
        return null != model ? model : (model = delegate.getModel());
    }

    @Override
    public FileSystemController<?> getParent() {
        return delegate.getParent();
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        return delegate.getOpenIcon();
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        return delegate.getClosedIcon();
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return delegate.isReadOnly();
    }

    @Override
    public FileSystemEntry getEntry(FileSystemEntryName name)
    throws IOException {
        return delegate.getEntry(name);
    }

    @Override
    public boolean isReadable(FileSystemEntryName name) throws IOException {
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FileSystemEntryName name) throws IOException {
        return delegate.isWritable(name);
    }

    @Override
    public void setReadOnly(FileSystemEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(FileSystemEntryName name, BitField<Access> types, long value)
    throws IOException {
        return delegate.setTime(name, types, value);
    }

    @Override
    public InputSocket<?> getInputSocket(
            FileSystemEntryName name,
            BitField<InputOption> options) {
        return delegate.getInputSocket(name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FileSystemEntryName name,
            BitField<OutputOption> options,
            Entry template) {
        return delegate.getOutputSocket(name, options, template);
    }

    @Override
    public boolean mknod(   FileSystemEntryName name,
                            Entry.Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        return delegate.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FileSystemEntryName name) throws IOException {
        delegate.unlink(name);
    }

    @Override
    public <X extends IOException>
    void sync(  final BitField<SyncOption> options, final ExceptionBuilder<? super SyncException, X> builder)
    throws X, FileSystemException {
        delegate.sync(options, builder);
    }
}
