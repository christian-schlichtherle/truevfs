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
package de.schlichtherle.truezip.fs.file.nio;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;
import javax.swing.Icon;
import net.jcip.annotations.ThreadSafe;

import static de.schlichtherle.truezip.entry.Entry.*;
import static de.schlichtherle.truezip.entry.Entry.Access.*;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;

/**
 * A controller for a mount point of the operating system's file system.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class FileController extends FsController<FsModel>  {

    private static final OpenOption[] RWD_OPTIONS = {
        StandardOpenOption.READ,
        StandardOpenOption.WRITE,
        StandardOpenOption.DSYNC,
    };

    private final FsModel model;
    private final Path target;

    FileController(final FsModel model) {
        if (null != model.getParent())
            throw new IllegalArgumentException();
        this.model = model;
        this.target = Paths.get(model.getMountPoint().toUri());
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
        return exists(entry.getPath()) ? entry : null;
    }

    @Override
    public boolean isReadable(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        return Files.isReadable(file);
    }

    @Override
    public boolean isWritable(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        return isCreatableOrWritable(file);
    }

    /**
     * Returns {@code true} if the given file can be created or exists
     * and at least one byte can be successfully written to it - the file is
     * restored to its previous state afterwards.
     * This is a much stronger test than {@link java.io.File#canWrite()}.
     */
    static boolean isCreatableOrWritable(final Path file) {
        try {
            try {
                createFile(file);
                try {
                    return isCreatableOrWritable(file);
                } finally {
                    delete(file);
                }
            } catch (FileAlreadyExistsException ex) {
                if (Files.isWritable(file)) {
                    // Some operating and file system combinations make File.canWrite()
                    // believe that the file is writable although it's not.
                    // We are not that gullible, so let's test this...
                    final long time = getLastModifiedTime(file).toMillis();
                    if (0 > time) {
                        // lastModified() may return negative values but setLastModified()
                        // throws an IAE for negative values, so we are conservative.
                        // See issue #18.
                        return false;
                    }
                    try {
                        setLastModifiedTime(file, FileTime.fromMillis(time + 1));
                    } catch (IOException ex2) {
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
                        final SeekableByteChannel
                                sbc = newByteChannel(file, RWD_OPTIONS);
                        try {
                            final ByteBuffer buf = ByteBuffer.allocate(1);
                            final boolean empty;
                            byte octet;
                            if (-1 == sbc.read(buf)) {
                                octet = (byte) 0; // assume first byte is 0
                                empty = true;
                            } else {
                                octet = buf.get(0);
                                empty = false;
                            }
                            // Let's test if we can overwrite the first byte.
                            // See issue #29.
                            sbc.position(0);
                            buf.rewind();
                            sbc.write(buf);
                            try {
                                // Rewrite original content and check success.
                                sbc.position(0);
                                buf.rewind();
                                sbc.read(buf);
                                final byte check = buf.get(0);
                                // This should always return true unless the storage
                                // device is faulty.
                                ok = octet == check;
                            } finally {
                                if (empty)
                                    sbc.truncate(0);
                            }
                        } finally {
                            sbc.close();
                        }
                    } finally {
                        try {
                            setLastModifiedTime(file, FileTime.fromMillis(time));
                        } catch (IOException ex2) {
                            // This may happen on Windows and normally means that
                            // somebody else has opened this file meanwhile
                            // (regardless of read or write mode).
                            // Be conservative: We don't allow (further) writing to
                            // this file!
                            ok = false;
                        }
                    }
                    return ok;
                } else { // if (!Files.isWritable(file)) {
                    return false;
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public void setReadOnly(FsEntryName name) throws IOException {
        Path file = target.resolve(name.getPath());
        // Confirmed: There is no equivalent NIO.2 method, e.g. something like
        //   setAttribute(file, "readOnly", Boolean.TRUE, null);
        // is not available!
        if (!file.toFile().setReadOnly())
            if (exists(file)) // just guessing here
                throw new AccessDeniedException(file.toString());
            else
                throw new FileNotFoundException(file.toString());
    }

    @Override
    public boolean setTime( final FsEntryName name,
                            final BitField<Access> types,
                            final long value)
    throws IOException {
        final Path file = target.resolve(name.getPath());
        final FileTime time = FileTime.fromMillis(value);
        getFileAttributeView(file, BasicFileAttributeView.class).setTimes(
                types.get(WRITE)  ? time : null,
                types.get(READ)   ? time : null,
                types.get(CREATE) ? time : null);
        return types.clear(WRITE).clear(READ).clear(CREATE).isEmpty();
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
        final Path file = target.resolve(name.getPath());
        switch (type) {
            case FILE:
                if (options.get(EXCLUSIVE))
                    createFile(file);
                else
                    newOutputStream(file).close();
                break;

            case DIRECTORY:
                createDirectory(file);
                break;

            default:
                throw new IOException(file + " (entry type not supported: " + type + ")");
        }
        if (null != template) {
            final long time = template.getTime(WRITE);
            if (UNKNOWN != time)
                setLastModifiedTime(file, FileTime.fromMillis(time));
        }
    }

    @Override
    public void unlink(FsEntryName name)
    throws IOException {
        Path file = target.resolve(name.getPath());
        delete(file);
    }

    @Override
    public <X extends IOException>
    void sync(  BitField<FsSyncOption> options,
                ExceptionHandler<? super FsSyncException, X> handler)
    throws X {
    }
}
