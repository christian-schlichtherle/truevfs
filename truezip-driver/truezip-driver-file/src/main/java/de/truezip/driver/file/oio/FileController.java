/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.oio;

import de.truezip.kernel.cio.Entry;
import de.truezip.kernel.cio.Entry.Access;
import static de.truezip.kernel.cio.Entry.Access.WRITE;
import de.truezip.kernel.cio.Entry.Type;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.fs.FsController;
import de.truezip.kernel.fs.FsModel;
import de.truezip.kernel.fs.FsSyncException;
import de.truezip.kernel.fs.addr.FsEntryName;
import static de.truezip.kernel.fs.addr.FsEntryName.SEPARATOR;
import de.truezip.kernel.fs.option.FsInputOption;
import de.truezip.kernel.fs.option.FsOutputOption;
import static de.truezip.kernel.fs.option.FsOutputOption.EXCLUSIVE;
import de.truezip.kernel.fs.option.FsSyncOption;
import de.truezip.kernel.util.BitField;
import de.truezip.kernel.util.ExceptionHandler;
import java.io.File;
import static java.io.File.separatorChar;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

/**
 * A file system controller with a prospective directory in the platform file
 * system as its mount point.
 *
 * @author Christian Schlichtherle
 */
@Immutable
final class FileController extends FsController<FsModel>  {

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
            if (file.exists())
                throw new IOException(file + " (access denied)"); // just guessing here
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
