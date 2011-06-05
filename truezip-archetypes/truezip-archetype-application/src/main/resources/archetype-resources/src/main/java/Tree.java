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

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;
import java.io.PrintStream;
import java.util.Arrays;

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
            graph(new TFile(arg), "", "");
        return 0;
    }

    private void graph(final TFile file, final String padding, final String prefix) {
        if (!file.exists())
            throw new IllegalArgumentException(file.getPath() + " (file or directory does not exist)");
        final PrintStream out = System.out;
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
            Arrays.sort(entries);
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
