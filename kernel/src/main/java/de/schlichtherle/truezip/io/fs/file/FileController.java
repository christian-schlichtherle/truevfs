/*
 * Copyright 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.fs.file;

import de.schlichtherle.truezip.io.fs.FsEntryName;
import de.schlichtherle.truezip.io.fs.FsController;
import java.net.URI;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.file.FileOutputStream;
import de.schlichtherle.truezip.io.fs.FsException;
import de.schlichtherle.truezip.io.fs.FsModel;
import de.schlichtherle.truezip.io.fs.FsInputOption;
import de.schlichtherle.truezip.io.fs.FsOutputOption;
import de.schlichtherle.truezip.io.fs.FsSyncException;
import de.schlichtherle.truezip.io.fs.FsSyncOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionHandler;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.Files.*;
import static de.schlichtherle.truezip.io.entry.Entry.*;
import static de.schlichtherle.truezip.io.entry.Entry.Access.*;
import static de.schlichtherle.truezip.io.fs.FsEntryName.*;
import static de.schlichtherle.truezip.io.fs.FsOutputOption.*;
import static java.io.File.separatorChar;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class FileController extends FsController<FsModel>  {

    private final FsModel model;
    private final File target;

    FileController(final FsModel model) {
        if (null != model.getParent())
            throw new IllegalArgumentException();
        URI uri = model.getMountPoint().getUri();
        // Postfix: Move Windows UNC host from authority to path.
        if ('\\' == separatorChar && null != uri.getRawAuthority()) {
            try {
                uri = new URI(  uri.getScheme(), "",
                                SEPARATOR + SEPARATOR + uri.getAuthority() + uri.getPath(),
                                uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }
        this.model = model;
        this.target = new File(uri);
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
    public FileEntry getEntry(FsEntryName name) throws IOException {
        final FileEntry entry = new FileEntry(target, name);
        return entry.getFile().exists() ? entry : null;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        final File file = new File(target, name.getPath());
        return file.canRead();
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        final File file = new File(target, name.getPath());
        return isCreatableOrWritable(file);
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        final File file = new File(target, name.getPath());
        if (!file.setReadOnly())
            throw new IOException();
    }

    @Override
    public boolean setTime(FsEntryName name, BitField<Access> types, long value)
    throws IOException {
        final File file = new File(target, name.getPath());
        boolean ok = true;
        for (final Access type : types)
            ok &= WRITE == type ? file.setLastModified(value) : false;
        return ok;
    }

    @Override
    public InputSocket<?> getInputSocket(
            FsEntryName name,
            BitField<FsInputOption> options) {
        return new FileEntry(target, name).getInputSocket();
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FsEntryName name,
            BitField<FsOutputOption> options,
            Entry template) {
        return new FileEntry(target, name).getOutputSocket(options, template);
    }

    @Override
    public void mknod(  final @NonNull FsEntryName name,
                        final @NonNull Type type,
                        final @NonNull BitField<FsOutputOption> options,
                        final @Nullable Entry template)
    throws IOException {
        final File file = new File(target, name.getPath());
        switch (type) {
            case FILE:
                if (options.get(EXCLUSIVE)) {
                    if (!file.createNewFile())
                        throw new IOException(file.getPath() + " (file exists already)");
                } else {
                    new FileOutputStream(file).close();
                }
                break;

            case DIRECTORY:
                if (!file.mkdir())
                    throw new IOException(file.getPath() + " (directory exists already)");
                break;

            default:
                throw new IOException(file.getPath() + " (entry type not supported: " + type + ")");
        }
        if (null != template) {
            final long time = template.getTime(WRITE);
            if (UNKNOWN != time && !file.setLastModified(time))
                throw new IOException(file.getPath() + " (cannot set last modification time)");
        }
    }

    @Override
    public void unlink(FsEntryName name)
    throws IOException {
        final File file = new File(target, name.getPath());
        if (!file.delete())
            throw new IOException(file.getPath() + " (cannot delete)");
    }

    @Override
    public <X extends IOException>
    void sync(  @NonNull BitField<FsSyncOption> options,
                @NonNull ExceptionHandler<? super FsSyncException, X> handler)
    throws X, FsException {
    }
}
