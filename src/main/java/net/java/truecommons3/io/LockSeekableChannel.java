/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * Protects all access to its decorated seekable byte channel via a
 * {@link Lock} object.
 *
 * @author Christian Schlichtherle
 */
public class LockSeekableChannel extends DecoratingSeekableChannel {

    /** The lock on which this object synchronizes. */
    private final Lock lock;

    /**
     * Constructs a new lock seekable channel.
     * Closing this channel will close the given channel.
     *
     * @param lock the lock to use for synchronization.
     * @param channel the channel to decorate.
     */
    public LockSeekableChannel(final Lock lock, final SeekableByteChannel channel) {
        super(channel);
        this.lock = Objects.requireNonNull(lock);
    }

    @Override
    public boolean isOpen() {
        lock.lock();
        try {
            return channel.isOpen();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        lock.lock();
        try {
            return channel.read(dst);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        lock.lock();
        try {
            return channel.write(src);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long position() throws IOException {
        lock.lock();
        try {
            return channel.position();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SeekableByteChannel position(long pos) throws IOException {
        lock.lock();
        try { 
            channel.position(pos);
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public long size() throws IOException {
        lock.lock();
        try {
            return channel.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        lock.lock();
        try {
            channel.truncate(size);
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            channel.close();
        } finally {
            lock.unlock();
        }
    }
}
