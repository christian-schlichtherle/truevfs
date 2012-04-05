/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Protects all access to its decorated seekable byte channel via a
 * {@link Lock} object.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockSeekableChannel
extends DecoratingSeekableChannel {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new lock seekable byte channel.
     *
     * @param channel the seekable byte channel to decorate.
     * @param lock the lock to use.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockSeekableChannel(
            final @Nullable @WillCloseWhenClosed SeekableByteChannel channel,
            final Lock lock) {
        super(channel);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    @GuardedBy("lock")
    public boolean isOpen() {
        lock.lock();
        try {
            return channel.isOpen();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public int read(ByteBuffer dst) throws IOException {
        lock.lock();
        try {
            return channel.read(dst);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public int write(ByteBuffer src) throws IOException {
        lock.lock();
        try {
            return channel.write(src);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
    public long position() throws IOException {
        lock.lock();
        try {
            return channel.position();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
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
    @GuardedBy("lock")
    public long size() throws IOException {
        lock.lock();
        try {
            return channel.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    @GuardedBy("lock")
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
    @GuardedBy("lock")
    public void close() throws IOException {
        lock.lock();
        try {
            channel.close();
        } finally {
            lock.unlock();
        }
    }
}
