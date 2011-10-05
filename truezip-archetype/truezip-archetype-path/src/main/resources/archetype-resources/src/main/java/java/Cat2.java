#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java;

import de.schlichtherle.truezip.nio.file.TPath;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This command line utility concatenates the contents of the parameter paths
 * on the standard output.
 * 
 * @see     <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author  Christian Schlichtherle
 * @version $Id$
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
            Path path = new TPath(arg);
            // If the path refers to an entry in an archive file, the TrueZIP
            // Kernel will create a cache entry for it.
            // This is inefficient in comparison with copying an input stream.
            // Don't use in production code!
            try (SeekableByteChannel src = Files.newByteChannel(path)) {
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
