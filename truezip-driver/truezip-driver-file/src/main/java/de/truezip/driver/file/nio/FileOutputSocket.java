/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file.nio;

import de.truezip.driver.file.io.IOExceptionOutputStream;
import de.truezip.driver.file.io.IOExceptionSeekableByteChannel;
import de.truezip.kernel.cio.Entry;
import static de.truezip.kernel.cio.Entry.Access.*;
import static de.truezip.kernel.cio.Entry.UNKNOWN;
import de.truezip.kernel.cio.IOSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.option.FsAccessOption;
import static de.truezip.kernel.option.FsAccessOption.*;
import de.truezip.kernel.util.BitField;
import static de.truezip.kernel.util.Maps.initialCapacity;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.Boolean.TRUE;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output socket for a file entry.
 *
 * @see    FileInputSocket
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class FileOutputSocket extends OutputSocket<FileEntry> {

    private static final int
            INITIAL_CAPACITY = initialCapacity(FsAccessOption.values().length);
    private static final StandardOpenOption[] 
            WRITE_STANDARD_OPEN_OPTION = {
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
            };

    private final               FileEntry                entry;
    private final               BitField<FsAccessOption> options;
    private final @CheckForNull Entry                    template;

    FileOutputSocket(   final               FileEntry                entry,
                        final               BitField<FsAccessOption> options,
                        final @CheckForNull Entry                    template) {
        assert null != entry;
        assert null != options;
        if (options.get(EXCLUSIVE) && options.get(APPEND))
            throw new IllegalArgumentException();
        this.entry    = entry;
        this.options  = options;
        this.template = template;
    }

    @Override
    public FileEntry getLocalTarget() {
        return entry;
    }

    private FileEntry begin() throws IOException {
        final FileEntry temp;
        final Path entryFile = entry.getPath();
        Boolean exists = null;
        if (options.get(EXCLUSIVE) && (exists = exists(entryFile)))
            throw new FileAlreadyExistsException(entry.toString());
        if (options.get(CACHE)) {
            // This is obviously NOT atomic.
            if (TRUE.equals(exists)
                    || null == exists && (exists = exists(entryFile))) {
                //if (!isWritable(entryFile)) throw new IOException(...)
                entryFile   .getFileSystem()
                            .provider()
                            .checkAccess(entryFile, AccessMode.WRITE);
            } else {
                createFile(entryFile);
            }
            temp = entry.createTempFile();
        } else {
            temp = entry;
        }
        if (options.get(CREATE_PARENTS) && !TRUE.equals(exists)) {
            final Path parentFile = entryFile.getParent();
            if (null != parentFile)
                createDirectories(parentFile);
        }
        return temp;
    }

    private void append(final FileEntry temp) throws IOException {
        if (temp != entry && options.get(APPEND) && exists(entry.getPath()))
            IOSocket.copy(entry.getInputSocket(), temp.getOutputSocket());
    }

    private Set<OpenOption> optionSet() {
        final Set<OpenOption> set = new HashSet<OpenOption>(INITIAL_CAPACITY);
        Collections.addAll(set, WRITE_STANDARD_OPEN_OPTION);
        if (options.get(APPEND)) {
            set.add(StandardOpenOption.APPEND);
            set.remove(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (options.get(EXCLUSIVE))
            set.add(StandardOpenOption.CREATE_NEW);
        return set;
    }

    private OpenOption[] optionArray() {
        final Set<OpenOption> set = optionSet();
        return set.toArray(new OpenOption[set.size()]);
    }

    private void close(final FileEntry temp, final boolean commit)
    throws IOException {
        final Path entryFile = entry.getPath();
        if (temp != entry) {
            final Path tempFile = temp.getPath();
            copyAttributes(tempFile);
            if (commit) {
                try {
                    move(tempFile, entryFile, REPLACE_EXISTING);
                } catch (final IOException ex) {
                    // Slow.
                    /*Files.copy(tempFile, entryFile,
                            StandardCopyOption.REPLACE_EXISTING);*/
                    // Fast.
                    IOSocket.copy(  temp.getInputSocket(),
                                    entry.getOutputSocket());
                    copyAttributes(entryFile);
                }
                release(temp, null);
            } else {
                // Leave temp file for post-mortem analysis.
            }
        } else {
            copyAttributes(entryFile);
        }
    }

    private void copyAttributes(final Path file) throws IOException {
        final Entry template = this.template;
        if (null == template)
            return;
        getFileAttributeView(file, BasicFileAttributeView.class)
                .setTimes(  toFileTime(template.getTime(WRITE)),
                            toFileTime(template.getTime(READ)),
                            toFileTime(template.getTime(CREATE)));
    }

    private static @Nullable FileTime toFileTime(long time) {
        return UNKNOWN == time ? null : FileTime.fromMillis(time);
    }

    private void release(
            final FileEntry temp,
            final @CheckForNull IOException ex)
    throws IOException {
        try {
            temp.release();
        } catch (IOException ex2) {
            ex2.initCause(ex);
            throw ex2;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public SeekableByteChannel newSeekableByteChannel() throws IOException {
        final FileEntry temp = begin();

        final class Channel extends IOExceptionSeekableByteChannel {
            boolean closed;

            Channel() throws IOException {
                super(newByteChannel(temp.getPath(), optionSet()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                super.close();
                closed = true;
                close(temp, null == exception);
            }
        } // Channel

        try {
            append(temp);
            return new Channel();
        } catch (IOException ex) {
            release(temp, ex);
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public OutputStream newOutputStream() throws IOException {
        final FileEntry temp = begin();

        final class Stream extends IOExceptionOutputStream {
            boolean closed;

            @CreatesObligation
            @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
            Stream() throws IOException {
                super(Files.newOutputStream(temp.getPath(), optionArray()));
            }

            @Override
            public void close() throws IOException {
                if (closed)
                    return;
                super.close();
                closed = true;
                close(temp, null == exception);
            }
        } // Stream

        try {
            append(temp);
            return new Stream();
        } catch (IOException ex) {
            release(temp, ex);
            throw ex;
        }
    }
}