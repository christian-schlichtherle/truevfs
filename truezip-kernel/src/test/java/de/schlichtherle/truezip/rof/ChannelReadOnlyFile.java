/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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

package de.schlichtherle.truezip.rof;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import net.jcip.annotations.NotThreadSafe;

/**
 * A {@link ReadOnlyFile} implementation using file channels.
 *
 * @author Christian Schlichtherle
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
