#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java.file;

import ${package}.java.Application;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import net.java.truevfs.access.TFile;
import net.java.truevfs.kernel.spec.FsSyncException;

/**
 * This command line utility prints the tree graph of the directory structure
 * of its file or directory arguments to the standard output.
 * Instead of a directory, you can name any configured archive file type as an
 * argument, too.
 * <p>
 * For example, if the JAR for the module {@code truevfs-driver-zip} is present
 * on the run time class path and the path name argument is {@code archive.zip}
 * and this file actually exists as a ZIP file, then the tree graph of the
 * directory structure of this ZIP file gets printed.
 *
 * @author  Christian Schlichtherle
 */
public class Tree extends Application<IOException> {

    private static final String DEFAULT_PREFIX  = "|-- ";
    private static final String LAST_PREFIX     = "`-- ";
    private static final String DEFAULT_PADDING = "|   ";
    private static final String LAST_PADDING    = "    ";

    public static void main(String[] args) throws IOException {
        System.exit(new Tree().run(args));
    }

    @Override
    protected int work(String[] args) throws IOException {
        if (0 >= args.length) args = new String[] { "." };
        for (String arg : args) graph(new TFile(arg), "", "");
        return 0;
    }

    private void graph(
            final File file,
            final String padding,
            final String prefix)
    throws IOException {
        if (!file.exists())
            throw new IllegalArgumentException(file + " (file or directory does not exist)");
        System.out.append(padding).append(prefix).println(file.getName());
        if (file.isDirectory()) {
            final File[] entries = file.listFiles();
            if (null == entries)
                throw new IOException(file + " (cannot list directory");
            Arrays.sort(entries);
            final int l = entries.length - 1;
            if (0 <= l) {
                String nextPadding = padding;
                if (0 < prefix.length())
                    nextPadding += LAST_PREFIX.equals(prefix)
                            ? LAST_PADDING
                            : DEFAULT_PADDING;
                int i = 0;
                while (i < l) graph(entries[i++], nextPadding, DEFAULT_PREFIX);
                graph(entries[i], nextPadding, LAST_PREFIX);
            }
        }
    }
}
