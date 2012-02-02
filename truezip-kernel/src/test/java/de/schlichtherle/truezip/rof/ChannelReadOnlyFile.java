/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A {@link ReadOnlyFile} implementation using file channels.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
public final class ChannelReadOnlyFile extends AbstractReadOnlyFile {

    /** For use by {@link #read()} only! */
    private final ByteBuffer singleByteBuffer = ByteBuffer.allocate(1);

    private final FileChannel channel;

    public ChannelReadOnlyFile(File file) throws FileNotFoundException {
        channel = new FileInputStream(file).getChannel();
    }

    @Override
    public long length() throws IOException {
        return channel.size();
    }

    @Override
    public long getFilePointer() throws IOException {
        return channel.position();
    }

    @Override
    public void seek(long fp) throws IOException {
        try {
            channel.position(fp);
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public int read() throws IOException {
        singleByteBuffer.position(0);
        if (channel.read(singleByteBuffer) == 1)
            return singleByteBuffer.get(0) & 0xff;
        else
            return -1;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return channel.read(ByteBuffer.wrap(buf, off, len));
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
