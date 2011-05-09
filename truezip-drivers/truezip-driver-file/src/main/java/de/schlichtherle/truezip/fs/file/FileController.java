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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import static java.io.File.separatorChar;

/**
 * A controller for a mount point of the operating system's file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class FileController extends FsController<FsModel>  {

    private final FsModel model;
    private final File target;

    FileController(final FsModel model) {
        if (null != model.getParent())
            throw new IllegalArgumentException();
        URI uri = model.getMountPoint().getUri();
        if ('\\' == separatorChar && null != uri.getRawAuthority()) {
            try {
                // Postfix: Move Windows UNC host from authority to path
                // component because the File class can't deal with this.
                // Note that the authority parameter must not be null and that
                // you cannot use the UriBuilder class - using either of these
                // would result in the authority property of the new URI object
                // being equal to the original value again.
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
        FileEntry entry = new FileEntry(target, name);
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

    /**
     * Returns {@code true} if the given file can be created or exists
     * and at least one byte can be successfully written to it - the file is
     * restored to its previous state afterwards.
     * This is a much stronger test than {@link File#canWrite()}.
     */
    static boolean isCreatableOrWritable(final File file) {
        try {
            if (file.createNewFile()) {
                return isCreatableOrWritable(file) && file.delete();
            } else if (file.canWrite()) {
                // Some operating and file system combinations make File.canWrite()
                // believe that the file is writable although it's not.
                // We are not that gullible, so let's test this...
                final long time = file.lastModified();
                if (time < 0) {
                    // lastModified() may return negative values but setLastModified()
                    // throws an IAE for negative values, so we are conservative.
                    // See issue #18.
                    return false;
                }
                if (!file.setLastModified(time + 1)) {
                    // This may happen on Windows and normally means that
                    // somebody else has opened this file
                    // (regardless of read or write mode).
                    // Be conservative: We don't allow writing to this file!
                    return false;
                }
                boolean ok;
                try {
                    // Open the file for reading and writing, requiring any
                    // update to its contents to be written to the filesystem
                    // synchronously.
                    // As Dr. Simon White from Catalysoft, Cambridge, UK reported,
                    // "rws" does NOT work on Mac OS X with Apple's Java 1.5
                    // Release 1 (equivalent to Sun's Java 1.5.0_02), however
                    // it DOES work with Apple's Java 1.5 Release 3.
                    // He also confirmed that "rwd" works on Apple's
                    // Java 1.5 Release 1, so we use this instead.
                    // Thank you very much for spending the time to fix this
                    // issue, Dr. White!
                    final RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    try {
                        final boolean empty;
                        int octet = raf.read();
                        if (octet == -1) {
                            octet = 0; // assume first byte is 0
                            empty = true;
                        } else {
                            empty = false;
                        }
                        // Let's test if we can overwrite the first byte.
                        // See issue #29.
                        raf.seek(0);
                        raf.write(octet);
                        try {
                            // Rewrite original content and check success.
                            raf.seek(0);
                            final int check = raf.read();
                            // This should always return true unless the storage
                            // device is faulty.
                            ok = octet == check;
                        } finally {
                            if (empty)
                                raf.setLength(0);
                        }
                    } finally {
                        raf.close();
                    }
                } finally {
                    if (!file.setLastModified(time)) {
                        // This may happen on Windows and normally means that
                        // somebody else has opened this file meanwhile
                        // (regardless of read or write mode).
                        // Be conservative: We don't allow (further) writing to
                        // this file!
                        ok = false;
                    }
                }
                return ok;
            } else { // if (!file.canWrite()) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
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
    void sync(  BitField<FsSyncOption> options,
                ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}
