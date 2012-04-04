/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sbc;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Protects all access to its decorated seekable byte channel via a
 * {@link Lock} object.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public class LockSeekableByteChannel
extends DecoratingSeekableByteChannel {

    /** The lock on which this object synchronizes. */
    protected final Lock lock;

    /**
     * Constructs a new lock seekable byte channel.
     *
     * @param sbc the seekable byte channel to decorate.
     * @param lock the lock to use.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    public LockSeekableByteChannel(
            final @Nullable @WillCloseWhenClosed SeekableByteChannel sbc,
            final Lock lock) {
        super(sbc);
        if (null == (this.lock = lock))
            throw new NullPointerException();
    }

    @Override
    public boolean isOpen() {
        lock.lock();
        try {
            return sbc.isOpen();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        lock.lock();
        try {
            return sbc.read(dst);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        lock.lock();
        try {
            return sbc.write(src);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long position() throws IOException {
        lock.lock();
        try {
            return sbc.position();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SeekableByteChannel position(long pos) throws IOException {
        lock.lock();
        try { 
            sbc.position(pos);
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public long size() throws IOException {
        lock.lock();
        try {
            return sbc.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        lock.lock();
        try {
            sbc.truncate(size);
        } finally {
            lock.unlock();
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        lock.lock();
        try {
            sbc.close();
        } finally {
            lock.unlock();
        }
    }
}
