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
package de.schlichtherle.truezip.io.filesystem.file;

import de.schlichtherle.truezip.io.filesystem.FileSystemEntryName;
import de.schlichtherle.truezip.io.filesystem.FileSystemException;
import de.schlichtherle.truezip.io.filesystem.FileSystemController;
import java.net.URI;
import de.schlichtherle.truezip.io.entry.Entry;
import de.schlichtherle.truezip.io.entry.Entry.Access;
import de.schlichtherle.truezip.io.entry.Entry.Type;
import de.schlichtherle.truezip.io.filesystem.FileSystemModel;
import de.schlichtherle.truezip.io.filesystem.SyncException;
import de.schlichtherle.truezip.io.filesystem.SyncOption;
import de.schlichtherle.truezip.io.socket.FileInputSocket;
import de.schlichtherle.truezip.io.socket.FileOutputSocket;
import de.schlichtherle.truezip.io.socket.InputOption;
import de.schlichtherle.truezip.io.socket.OutputOption;
import de.schlichtherle.truezip.io.socket.InputSocket;
import de.schlichtherle.truezip.io.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import de.schlichtherle.truezip.util.ExceptionBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.io.Files.isCreatableOrWritable;
import static de.schlichtherle.truezip.io.entry.Entry.Access.WRITE;
import static de.schlichtherle.truezip.io.filesystem.FileSystemEntry.SEPARATOR;
import static java.io.File.separatorChar;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
final class FileController extends FileSystemController<FileSystemModel>  {

    private final FileSystemModel model;
    private final File target;

    FileController(final FileSystemModel model) {
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
    public FileSystemModel getModel() {
        return model;
    }

    @Override
    public FileSystemController<?> getParent() {
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
    public FileEntry getEntry(FileSystemEntryName path) throws IOException {
        final FileEntry entry = FileEntry.get(target, path.getPath());
        return entry.getFile().exists() ? entry : null;
    }

    @Override
    public boolean isReadable(FileSystemEntryName path) throws IOException {
        final File file = new File(target, path.getPath());
        return file.canRead();
    }

    @Override
    public boolean isWritable(FileSystemEntryName path) throws IOException {
        final File file = new File(target, path.getPath());
        return isCreatableOrWritable(file);
    }

    @Override
    public void setReadOnly(FileSystemEntryName path) throws IOException {
        final File file = new File(target, path.getPath());
        if (!file.setReadOnly())
            throw new IOException();
    }

    @Override
    public boolean setTime(FileSystemEntryName path, BitField<Access> types, long value)
    throws IOException {
        final File file = new File(target, path.getPath());
        boolean ok = true;
        for (final Access type : types)
            ok &= WRITE == type ? file.setLastModified(value) : false;
        return ok;
    }

    @Override
    public InputSocket<?> getInputSocket(
            FileSystemEntryName path,
            BitField<InputOption> options) {
        return FileInputSocket.get( FileEntry.get(target, path.getPath()),
                                    options.clear(InputOption.CACHE));
    }

    @Override
    public OutputSocket<?> getOutputSocket(
            FileSystemEntryName path,
            BitField<OutputOption> options,
            Entry template) {
        return FileOutputSocket.get(FileEntry.get(target, path.getPath()), options, template);
    }

    @Override
    public boolean mknod(   FileSystemEntryName path,
                            Type type,
                            BitField<OutputOption> options,
                            Entry template)
    throws IOException {
        final File file = new File(target, path.getPath());
        switch (type) {
            case FILE:
                return file.createNewFile();

            case DIRECTORY:
                return file.mkdir();

            default:
                throw new IOException(file.getPath() + " (entry type not supported: " + type + ")");
        }
    }

    @Override
    public void unlink(FileSystemEntryName path)
    throws IOException {
        final File file = new File(target, path.getPath());
        if (!file.delete())
            throw new IOException(file.getPath() + " (cannot delete)");
    }

    @Override
    public <X extends IOException>
    void sync(  final ExceptionBuilder<? super SyncException, X> builder,
                final BitField<SyncOption> options)
    throws X, FileSystemException {
    }
}
