#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java;

import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.nio.file.TPath;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * This command line utility prints the tree graph of the directory structure
 * of its file or directory arguments to the standard output.
 * Instead of a directory, you can name any configured archive file type as an
 * argument, too.
 * <p>
 * For example, if the JAR for the module {@code truezip-driver-zip} is present
 * on the run time class path and the path name argument is {@code archive.zip}
 * and this file actually exists as a ZIP file, then the tree graph of the
 * directory structure of this ZIP file gets printed.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class Tree extends Application<RuntimeException> {

    private static final String DEFAULT_PREFIX  = "|-- ";
    private static final String LAST_PREFIX     = "`-- ";
    private static final String DEFAULT_PADDING = "|   ";
    private static final String LAST_PADDING    = "    ";

    public static void main(String[] args) throws FsSyncException {
        System.exit(new Tree().run(args));
    }

    @Override
    protected int work(String[] args) {
        if (0 >= args.length)
            args = new String[] { "." };
        for (String arg : args)
            graph(new TPath(arg), "", "");
        return 0;
    }

    private void graph(Path file, String padding, String prefix) {
        if (!exists(file))
            throw new IllegalArgumentException(file + " (file or directory does not exist)");
        System.out.append(padding).append(prefix).println(file.getFileName());
        if (isDirectory(file)) {
            Set<Path> entries = new TreeSet<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file)) {
                for (Path member : stream)
                    entries.add(member);
            } catch (IOException ex) {
                throw new AssertionError(ex); // unless someone has changed the entry type in between
            }
            int l = entries.size() - 1;
            if (0 <= l) {
                String nextPadding = padding;
                if (0 < prefix.length())
                    nextPadding += LAST_PREFIX.equals(prefix)
                            ? LAST_PADDING
                            : DEFAULT_PADDING;
                final Iterator<Path> it = entries.iterator();
                for (int i = 0; i++ < l; )
                    graph(it.next(), nextPadding, DEFAULT_PREFIX);
                graph(it.next(), nextPadding, LAST_PREFIX);
            }
        }
    }
}
