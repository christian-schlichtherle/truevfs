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
package de.schlichtherle.truezip.fs.file;

import de.schlichtherle.truezip.fs.FsModelController;
import net.jcip.annotations.Immutable;
import java.util.Map;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import de.schlichtherle.truezip.entry.Entry;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsEntryName;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Icon;

import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static java.io.File.separatorChar;

/**
 * A file system controller with a prospective directory in the platform file
 * system as its mount point.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
final class FileController extends FsModelController<FsModel>  {

    private static final String TWO_SEPARATORS = SEPARATOR + SEPARATOR;

    private final File target;

    FileController(final FsModel model) {
        super(model);
        if (null != model.getParent())
            throw new IllegalArgumentException();
        URI uri = model.getMountPoint().toUri();
        if ('\\' == separatorChar && null != uri.getRawAuthority()) {
            try {
                // Postfix: Move Windows UNC host from authority to path
                // component because the File class can't deal with this.
                // Note that the authority parameter must not be null and that
                // you cannot use the UriBuilder class - using either of these
                // would result in the authority property of the new URI object
                // being equal to the original value again.
                // Note that the use of the buggy URI constructor is authorized
                // for this case!
                uri = new URI(  uri.getScheme(), "",
                                TWO_SEPARATORS + uri.getAuthority() + uri.getPath(),
                                uri.getQuery(), uri.getFragment());
            } catch (URISyntaxException ex) {
                throw new AssertionError(ex);
            }
        }
        this.target = new File(uri);
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
        FileEntry entry = new FileEntry(target, name);
        return entry.getFile().exists() ? entry : null;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        File file = new File(target, name.getPath());
        return file.canRead();
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        File file = new File(target, name.getPath());
        return file.canWrite();
    }

    @Override
    public boolean isExecutable(FsEntryName name) throws IOException {
        File file = new File(target, name.getPath());
        return file.canExecute();
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        final File file = new File(target, name.getPath());
        if (!file.setReadOnly())
            if (file.exists()) // just guessing here
                throw new IOException(file + " (access denied)");
            else
                throw new FileNotFoundException(file.toString());
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            Map<Access, Long> times,
            BitField<FsOutputOption> options)
    throws IOException {
        final File file = new File(target, name.getPath());
        boolean ok = true;
        for (Map.Entry<Access, Long> time : times.entrySet())
            ok &= WRITE == time.getKey() && file.setLastModified(time.getValue());
        return ok;
    }

    @Override
    public boolean setTime(
            FsEntryName name,
            BitField<Access> types,
            long value,
            BitField<FsOutputOption> options)
    throws IOException {
        final File file = new File(target, name.getPath());
        boolean ok = true;
        for (final Access type : types)
            ok &= WRITE == type && file.setLastModified(value);
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
            @CheckForNull Entry template) {
        return new FileEntry(target, name).getOutputSocket(options, template);
    }

    @Override
    public void mknod(  final FsEntryName name,
                        final Type type,
                        final BitField<FsOutputOption> options,
                        final @CheckForNull Entry template)
    throws IOException {
        final File file = new File(target, name.getPath());
        switch (type) {
            case FILE:
                if (options.get(EXCLUSIVE)) {
                    if (!file.createNewFile())
                        throw new IOException(file + " (file exists already)");
                } else {
                    new FileOutputStream(file).close();
                }
                break;
            case DIRECTORY:
                if (!file.mkdir())
                    if (file.exists())
                        throw new IOException(file + " (directory exists already)");
                    else
                        throw new IOException(file.toString());
                break;
            default:
                throw new IOException(file + " (entry type not supported: " + type + ")");
        }
        if (null != template) {
            final long time = template.getTime(WRITE);
            if (UNKNOWN != time && !file.setLastModified(time))
                throw new IOException(file + " (cannot set last modification time)");
        }
    }

    @Override
    public void unlink(FsEntryName name, BitField<FsOutputOption> options)
    throws IOException {
        final File file = new File(target, name.getPath());
        if (!file.delete())
            throw new IOException(file + " (cannot delete)");
    }

    @Override
    public <X extends IOException>
    void sync(  BitField<FsSyncOption> options,
                ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}
