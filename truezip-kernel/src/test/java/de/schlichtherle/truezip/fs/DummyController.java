/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.entry.Entry.Access;
import de.schlichtherle.truezip.entry.Entry.Type;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.util.Map;
import javax.swing.Icon;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class DummyController<M extends FsModel> extends FsController<M> {

    private final @NonNull M model;
    private final @Nullable FsController<?> parent;

    public DummyController( final @NonNull M model,
                            final @CheckForNull FsController<?> parent) {
        assert null == model.getParent()
                ? null == parent
                : model.getParent().equals(parent.getModel());
        this.model = model;
        this.parent = parent;
    }

    @Override
    public M getModel() {
        return model;
    }

    @Override
    public FsController<?> getParent() {
        return parent;
    }

    @Override
    public Icon getOpenIcon() throws IOException {
        return null;
    }

    @Override
    public Icon getClosedIcon() throws IOException {
        return null;
    }

    @Override
    public boolean isReadOnly() throws IOException {
        return true;
    }

    @Override
    public FsEntry getEntry(FsEntryName name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        throw new IOException("Read only dummy file system controller!");
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        throw new IOException("Read only dummy file system controller!");
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        throw new IOException("Read only dummy file system controller!");
    }

    @Override
    public InputSocket<?> getInputSocket(FsEntryName name, BitField<FsInputOption> options) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputSocket<?> getOutputSocket(FsEntryName name, BitField<FsOutputOption> options, Entry template) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mknod(FsEntryName name, Type type, BitField<FsOutputOption> options, Entry template) throws IOException {
        throw new IOException("Read only dummy file system controller!");
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        throw new IOException("Read only dummy file system controller!");
    }

    @Override
    public <X extends IOException> void
    sync(   BitField<FsSyncOption> options,
            ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}
