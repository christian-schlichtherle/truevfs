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
package de.schlichtherle.truezip.io.fs;

import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import javax.swing.Icon;

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
public abstract class FsDecoratorController<
        M extends FSModel,
        C extends FsController<? extends M>>
extends FsController<M> {

    /** The decorated file system controller. */
    protected final C delegate;

    private volatile M model;

    /**
     * Constructs a new decorating file system controller.
     *
     * @param controller the decorated file system controller.
     */
    protected FsDecoratorController(@NonNull final C controller) {
        if (null == controller)
            throw new NullPointerException();
        this.delegate = controller;
    }

    @Override
    public final M getModel() {
        return null != model ? model : (model = delegate.getModel());
    }

    @Override
    public FsController<?> getParent() {
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
    public FSEntry getEntry(FSEntryName name)
    throws IOException {
        return delegate.getEntry(name);
    }

    @Override
    public boolean isReadable(FSEntryName name) throws IOException {
        return delegate.isReadable(name);
    }

    @Override
    public boolean isWritable(FSEntryName name) throws IOException {
        return delegate.isWritable(name);
    }

    @Override
    public void setReadOnly(FSEntryName name) throws IOException {
        delegate.setReadOnly(name);
    }

    @Override
    public boolean setTime(FSEntryName name, BitField<Access> types, long value)
    throws IOException {
        return delegate.setTime(name, types, value);
    }

    @Override
    public InputSocket<?> getInputSocket(
            FSEntryName name,
            BitField<FSInputOption> options) {
        return delegate.getInputSocket(name, options);
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FSEntryName name,
            BitField<FSOutputOption> options,
            Entry template) {
        return delegate.getOutputSocket(name, options, template);
    }

    @Override
    public void mknod(
            @NonNull FSEntryName name,
            @NonNull Type type,
            @NonNull BitField<FSOutputOption> options,
            @CheckForNull Entry template)
    throws IOException {
        delegate.mknod(name, type, options, template);
    }

    @Override
    public void unlink(FSEntryName name) throws IOException {
        delegate.unlink(name);
    }

    @Override
    public <X extends IOException>
    void sync(
            @NonNull BitField<FSSyncOption> options,
            @NonNull ExceptionHandler<? super FSSyncException, X> handler)
    throws X, FSException {
        delegate.sync(options, handler);
    }
}
