#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java;

import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.nio.file.TPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This command line utility concatenates the contents of the parameter paths
 * on the standard output.
 * 
 * @see    <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author Christian Schlichtherle
 */
public class Cat1 extends Application<IOException> {

    public static void main(String[] args) throws IOException {
        System.exit(new Cat1().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        for (String arg : args) {
            Path path = new TPath(arg);
            //Files.copy(path, System.out); // naive read-then-write loop implementation
            try (InputStream in = Files.newInputStream(path)) {
                // Much faster: Uses multithreaded I/O with pooled threads and
                // ring buffers!
                Streams.cat(in, System.out);
            }
            
        }
        return 0;
    }
}
