/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.file;

import de.truezip.kernel.io.DecoratingSeekableChannel;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating seekable byte channel which saves the last {@link IOException}
 * from its decorated seekable byte channel in a
 * {@link #exception protected field} for later use.
 *
 * @author Christian Schlichtherle
 */
abstract class IOExceptionSeekableChannel
extends DecoratingSeekableChannel {

    /** The nullable last I/O exception. */
    @CheckForNull IOException exception;

    /**
     * Constructs a new I/O exception seekable byte channel.
     *
     * @param channel the nullable seekable byte channel to decorate.
     */
    @CreatesObligation
    IOExceptionSeekableChannel(
            @Nullable @WillCloseWhenClosed SeekableByteChannel channel) {
        super(channel);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        try {
            return channel.read(dst);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        try {
            return channel.write(src);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public long position() throws IOException {
        try {
            return channel.position();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        try {
            channel.position(newPosition);
            return this;
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public long size() throws IOException {
        try {
            return channel.size();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        try {
            channel.truncate(size);
            return this;
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }
}
