/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.WillCloseWhenClosed;

/**
 * A decorating seekable byte channel which saves the last {@link IOException}
 * in a {@link #exception protected field} for later use.
 *
 * @since   TrueZIP 7.3.2
 * @author  Christian Schlichtherle
 * @deprecated This class will be removed in TrueZIP 8.
 * @version $Id$
 */
public abstract class IOExceptionSeekableByteChannel
extends DecoratingSeekableByteChannel {

    /** The nullable last I/O exception. */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    protected @CheckForNull IOException exception;

    /**
     * Constructs a new I/O exception seekable byte channel.
     *
     * @param channel the nullable seekable byte channel to decorate.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("OBL_UNSATISFIED_OBLIGATION")
    protected IOExceptionSeekableByteChannel(
            @Nullable @WillCloseWhenClosed SeekableByteChannel channel) {
        super(channel);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        try {
            return delegate.read(dst);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        try {
            return delegate.write(src);
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public long position() throws IOException {
        try {
            return delegate.position();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        try {
            delegate.position(newPosition);
            return this;
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public long size() throws IOException {
        try {
            return delegate.size();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        try {
            delegate.truncate(size);
            return this;
        } catch (IOException ex) {
            throw exception = ex;
        }
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }
    
    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        } catch (IOException ex) {
            throw exception = ex;
        }
    }
}
