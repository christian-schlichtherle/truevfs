#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
package ${package};

import de.schlichtherle.truezip.nio.fsp.TPath;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;

/**
 * @author  Christian Schlichtherle
 * @version ${symbol_dollar}Id${symbol_dollar}
 */
public class Cat2 extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Cat2().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(8096);
        final WritableByteChannel dst = Channels.newChannel(System.out);
        for (String arg : args) {
            // If the path refers to an entry in an archive file, the TrueZIP
            // Kernel will create a cache entry for it.
            // This is inefficient in comparison with copying an input stream.
            // Don't use in production code!
            try (SeekableByteChannel src = Files.newByteChannel(new TPath(arg))) {
                // Naive read-then-write loop.
                // Don't use in production code!
                while (-1 != src.read(buf)) {
                    buf.flip();
                    dst.write(buf);
                    buf.compact();
                }
            }
        }
        return 0;
    }
}
