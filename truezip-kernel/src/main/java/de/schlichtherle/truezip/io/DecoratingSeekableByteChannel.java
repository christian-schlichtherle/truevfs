/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * An abstract decorator for a seekable byte channel.
 * This is optimized for performance and <em>without</em> multithreading
 * support.
 *
 * @since   TrueZIP 7.2
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class DecoratingSeekableByteChannel
implements SeekableByteChannel {

    /** The nullable decorated seekable byte channel. */
    protected @Nullable SeekableByteChannel delegate;

    /**
     * Constructs a new decorating seekable byte channel.
     *
     * @param channel the seekable byte channel to decorate.
     */
    protected DecoratingSeekableByteChannel(
            final @Nullable SeekableByteChannel channel) {
        this.delegate = channel;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return delegate.read(dst);
    }
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        delegate.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }
    
    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /**
     * Returns a string representation of this object for debugging and logging
     * purposes.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(getClass().getName())
                .append("[delegate=")
                .append(delegate)
                .append(']')
                .toString();
    }
}
