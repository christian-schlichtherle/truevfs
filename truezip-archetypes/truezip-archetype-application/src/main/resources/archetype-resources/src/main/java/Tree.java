#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;
import java.io.PrintStream;

/**
 * Command line utility to print the tree graph of the directory structure of
 * its file or directory arguments to the standard output.
 * Instead of a directory, you can name any configured archive file type as an
 * argument, too.
 * E.g. if the JAR for the module {@code truezip-driver-zip} is present on the
 * run time class path and the path name argument is {@code archive.zip} and
 * this file actually exists as a ZIP file, then the tree graph of the
 * directory structure of this ZIP file will get printed.
 * 
 * @param <E> the {@link Exception} class to throw by {@link #work} and thus
 *       {@link #run}, too.
 * @author Christian Schlichtherle
 */
public class Tree extends Application<RuntimeException> {

    private static final String DEFAULT_PREFIX  = "|-- ";
    private static final String LAST_PREFIX     = "`-- ";
    private static final String DEFAULT_PADDING = "|   ";
    private static final String LAST_PADDING    = "    ";
    private static final PrintStream out = System.out;

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

    private void graph(final TFile file, final String padding, final String prefix) {
        if (!file.exists())
            throw new IllegalArgumentException(file.getPath() + " (file or directory does not exist)");
        out.print(padding);
        out.print(prefix);
        out.println(file.getName());
        if (file.isDirectory()) {
            String nextPadding = padding;
            if (0 < prefix.length())
                nextPadding += LAST_PREFIX.equals(prefix)
                        ? LAST_PADDING
                        : DEFAULT_PADDING;
            final TFile[] entries = file.listFiles();
            final int l = entries.length - 1;
            if (0 <= l) {
                int i = 0;
                while (i < l)
                    graph(entries[i++], nextPadding, DEFAULT_PREFIX);
                graph(entries[i], nextPadding, LAST_PREFIX);
            }
        }
    }
}
