/*
 * Copyright 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.http;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.fs.FsEntryName;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncOption;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.*;

/**
 * A file system controller for the HTTP(S) schemes.
 * 
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class HttpController extends FsController<FsModel>  {

    private final HttpDriver driver;
    private final FsModel model;

    HttpController(final HttpDriver driver, final FsModel model) {
        if (null != model.getParent())
            throw new IllegalArgumentException();
        assert null != driver;
        this.driver = driver;
        this.model = model;
    }

    HttpDriver getDriver() {
        return driver;
    }

    @Override
    public FsModel getModel() {
        return model;
    }

    @Override
    public FsController<?> getParent() {
        return null;
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
        return false;
    }

    @Override
    public HttpEntry getEntry(FsEntryName name) throws IOException {
        HttpEntry entry = new HttpEntry(this, model.getMountPoint(), name);
        return null != entry.getType() ? entry : null;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        return null != getEntry(name);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        return false;
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value)
    throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsInputOption> options) {
        return new HttpEntry(this, model.getMountPoint(), name).getInputSocket();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            @CheckForNull Entry template) {
        return new HttpEntry(this, model.getMountPoint(), name).getOutputSocket(options, template);
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final @CheckForNull Entry template)
    throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public void unlink(FsEntryName name)
    throws IOException {
        throw new ReadOnlyFileSystemTypeException();
    }

    @Override
    public <X extends IOException>
    void sync(  BitField<FsSyncOption> options,
                ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}
