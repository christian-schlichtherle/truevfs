/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.driver.file;

import net.java.truecommons.cio.AbstractOutputSocket;
import net.java.truecommons.cio.Entry;
import net.java.truecommons.cio.InputSocket;
import net.java.truecommons.cio.IoSockets;
import net.java.truecommons.shed.BitField;
import net.java.truevfs.kernel.spec.FsAccessOption;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static java.nio.file.Files.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static net.java.truecommons.cio.Entry.Access.*;
import static net.java.truecommons.cio.Entry.UNKNOWN;
import static net.java.truecommons.shed.HashMaps.initialCapacity;
import static net.java.truevfs.kernel.spec.FsAccessOption.*;

/**
 * An output socket for a file entry.
 *
 * @see    FileInputSocket
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
final class FileOutputSocket extends AbstractOutputSocket<FileNode> {

    private static final int
            INITIAL_CAPACITY = initialCapacity(FsAccessOption.values().length);

    private static final StandardOpenOption[]
            WRITE_STANDARD_OPEN_OPTION = {
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
            };

    private final BitField<FsAccessOption> options;
    private final FileNode node;
    private final Optional<? extends Entry> template;

    FileOutputSocket(
            final BitField<FsAccessOption> options,
            final FileNode node,
            final Optional<? extends Entry> template) {
        assert null != node;
        this.node = node;
        if (options.get(EXCLUSIVE) && options.get(APPEND))
            throw new IllegalArgumentException();
        this.options = options;
        this.template = template;
    }

    @Override
    public FileNode target() { return node; }

    private FileNode begin() throws IOException {
        final FileNode buffer;
        final Path entryFile = node.getPath();
        Boolean exists = null;
        if (options.get(EXCLUSIVE) && (exists = exists(entryFile))) {
            throw new FileAlreadyExistsException(node.toString());
        }
        if (options.get(CACHE)) {
            // This is obviously NOT properly isolated.
            if (TRUE.equals(exists) || null == exists && (exists = exists(entryFile))) {
                //if (!isWritable(entryFile)) throw new IOException(...)
                entryFile   .getFileSystem()
                            .provider()
                            .checkAccess(entryFile, AccessMode.WRITE);
            } else {
                createFile(entryFile);
            }
            buffer = node.createIoBuffer();
        } else {
            buffer = node;
        }
        if (options.get(CREATE_PARENTS) && !TRUE.equals(exists)) {
            final Path parentFile = entryFile.getParent();
            if (null != parentFile) {
                try {
                    createDirectories(parentFile);
                } catch (IOException ex) {
                    // Workaround for bug in Oracle JDK 1.7.0_51:
                    // A call to Files.createDirectories("C:\\") will fail on
                    // Windows although it shouldn't.
                    // Likewise, a call to Files.createDirectories("/") will
                    // fail on Mac OS X although it shouldn't.
                    // On Linux, it supposedly works however.
                    if (!isDirectory(parentFile)) throw ex;
                }
            }
        }
        return buffer;
    }

    void append(final FileNode buffer) throws IOException {
        if (buffer != node && options.get(APPEND) && exists(node.getPath())){
            IoSockets.copy(node.input(), buffer.output());
        }
    }

    Set<OpenOption> optionSet() {
        final Set<OpenOption> set = new HashSet<>(INITIAL_CAPACITY);
        Collections.addAll(set, WRITE_STANDARD_OPEN_OPTION);
        if (options.get(APPEND)) {
            set.add(StandardOpenOption.APPEND);
            set.remove(StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (options.get(EXCLUSIVE))
            set.add(StandardOpenOption.CREATE_NEW);
        return set;
    }

    OpenOption[] optionArray() {
        final Set<OpenOption> set = optionSet();
        return set.toArray(new OpenOption[set.size()]);
    }

    void close(final FileNode buffer, final boolean commit)
    throws IOException {
        final Path entryFile = node.getPath();
        if (buffer != node) {
            final Path bufferFile = buffer.getPath();
            updateProperties(bufferFile);
            if (commit) {
                try {
                    move(bufferFile, entryFile, REPLACE_EXISTING);
                } catch (final IOException ex) {
                    // Slow:
                    /*Files.copy(bufferFile, entryFile,
                            StandardCopyOption.REPLACE_EXISTING);*/
                    // Fast:
                    IoSockets.copy(buffer.input(), node.output());
                    updateProperties(entryFile);
                }
                buffer.release();
            } else {
                // Leave bufferFile for post-mortem analysis.
            }
        } else {
            updateProperties(entryFile);
        }
    }

    private void updateProperties(final Path file) throws IOException {
        final Optional<? extends Entry> template = this.template;
        if (template.isPresent()) {
            final Entry t = template.get();
            getFileAttributeView(file, BasicFileAttributeView.class)
                    .setTimes(toFileTime(t.getTime(WRITE)), toFileTime(t.getTime(READ)), toFileTime(t.getTime(CREATE)));
        }
    }

    private static @Nullable FileTime toFileTime(long time) {
        return UNKNOWN == time ? null : FileTime.fromMillis(time);
    }

    IOException release(
            final IOException ex,
            final FileNode buffer)
    throws IOException {
        try {
            buffer.release();
        } catch (final IOException ex2) {
            ex.addSuppressed(ex2);
        }
        return ex;
    }

    @Override
    public SeekableByteChannel channel(final Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        final FileNode buffer = begin();

        class Channel extends IOExceptionSeekableChannel {

            boolean closed;

            Channel() throws IOException {
                super(newByteChannel(buffer.getPath(), optionSet()));
            }

            @Override
            public void close() throws IOException {
                if (!closed) {
                    super.close();
                    closed = true;
                    FileOutputSocket.this.close(buffer, !exception.isPresent());
                }
            }
        }

        try {
            append(buffer);
            return new Channel();
        } catch (IOException ex) {
            throw release(ex, buffer);
        }
    }

    @Override
    public OutputStream stream(final Optional<? extends InputSocket<? extends Entry>> peer) throws IOException {
        final FileNode buffer = begin();

        class Stream extends IOExceptionOutputStream {
            boolean closed;

            Stream() throws IOException {
                super(Files.newOutputStream(buffer.getPath(), optionArray()));
            }

            @Override
            public void close() throws IOException {
                if (!closed) {
                    super.close();
                    closed = true;
                    FileOutputSocket.this.close(buffer, !exception.isPresent());
                }
            }
        }

        try {
            append(buffer);
            return new Stream();
        } catch (IOException ex) {
            throw release(ex, buffer);
        }
    }
}
