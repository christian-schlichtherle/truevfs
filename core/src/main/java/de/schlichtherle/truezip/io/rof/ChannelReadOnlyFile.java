/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.rof;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A {@link ReadOnlyFile} implementation using file channels.
 * <p>
 * This class is <em>not</em> thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ChannelReadOnlyFile extends AbstractReadOnlyFile {

    /** For use by {@link #read()} only! */
    private final ByteBuffer singleByteBuffer = ByteBuffer.allocate(1);

    private final FileChannel channel;

    public ChannelReadOnlyFile(File file) throws FileNotFoundException {
        channel = new FileInputStream(file).getChannel();
    }

    public long length() throws IOException {
        return channel.size();
    }

    public long getFilePointer() throws IOException {
        return channel.position();
    }

    public void seek(long fp) throws IOException {
        try {
            channel.position(fp);
        } catch (IllegalArgumentException iae) {
            throw new IOException(iae);
        }
    }

    public int read() throws IOException {
        singleByteBuffer.position(0);
        if (channel.read(singleByteBuffer) == 1)
            return singleByteBuffer.array()[0] & 0xff;
        else
            return -1;
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        return channel.read(ByteBuffer.wrap(buf, off, len));
    }

    public void close() throws IOException {
        channel.close();
    }
}
