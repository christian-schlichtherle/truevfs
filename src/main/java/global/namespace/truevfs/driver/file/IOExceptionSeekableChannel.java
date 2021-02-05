/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.driver.file;

import global.namespace.truevfs.commons.io.DecoratingSeekableChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

/**
 * A decorating seekable byte channel which saves the last {@link IOException}
 * from its decorated seekable byte channel in a
 * {@link #exception protected field} for later use.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
abstract class IOExceptionSeekableChannel extends DecoratingSeekableChannel {

    /** The nullable last I/O exception. */
    Optional<IOException> exception = Optional.empty();

    /**
     * Constructs a new I/O exception seekable byte channel.
     *
     * @param channel the nullable seekable byte channel to decorate.
     */
    IOExceptionSeekableChannel(SeekableByteChannel channel) {
        super(channel);
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        try {
            return channel.read(dst);
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
        try {
            return channel.write(src);
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public long position() throws IOException {
        try {
            return channel.position();
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public SeekableByteChannel position(final long newPosition) throws IOException {
        try {
            channel.position(newPosition);
            return this;
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public long size() throws IOException {
        try {
            return channel.size();
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public SeekableByteChannel truncate(final long size) throws IOException {
        try {
            channel.truncate(size);
            return this;
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
        } catch (IOException e) {
            exception = Optional.of(e);
            throw e;
        }
    }
}
