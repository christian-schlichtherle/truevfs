#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.java.file;

import ${package}.java.Application;
import net.truevfs.access.TFile;
import net.truevfs.kernel.spec.FsSyncException;
import java.io.File;
import java.util.Arrays;

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
            graph(new TFile(arg), "", "");
        return 0;
    }

    private void graph(File file, String padding, String prefix) {
        if (!file.exists())
            throw new IllegalArgumentException(file + " (file or directory does not exist)");
        System.out.append(padding).append(prefix).println(file.getName());
        if (file.isDirectory()) {
            File[] entries = file.listFiles();
            Arrays.sort(entries);
            int l = entries.length - 1;
            if (0 <= l) {
                String nextPadding = padding;
                if (0 < prefix.length())
                    nextPadding += LAST_PREFIX.equals(prefix)
                            ? LAST_PADDING
                            : DEFAULT_PADDING;
                int i = 0;
                while (i < l)
                    graph(entries[i++], nextPadding, DEFAULT_PREFIX);
                graph(entries[i], nextPadding, LAST_PREFIX);
            }
        }
    }
}
