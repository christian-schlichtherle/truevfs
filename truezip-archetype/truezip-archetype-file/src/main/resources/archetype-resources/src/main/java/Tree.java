#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ${package};

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;
import java.io.File;
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

    private void graph(File file, String padding, String prefix) {
        if (!file.exists())
            throw new IllegalArgumentException(file + " (file or directory does not exist)");
        PrintStream out = System.out;
        out.print(padding);
        out.print(prefix);
        out.println(file.getName());
        if (file.isDirectory()) {
            String nextPadding = padding;
            if (0 < prefix.length())
                nextPadding += LAST_PREFIX.equals(prefix)
                        ? LAST_PADDING
                        : DEFAULT_PADDING;
            File[] entries = file.listFiles();
            Arrays.sort(entries);
            int l = entries.length - 1;
            if (0 <= l) {
                int i = 0;
                while (i < l)
                    graph(entries[i++], nextPadding, DEFAULT_PREFIX);
                graph(entries[i], nextPadding, LAST_PREFIX);
            }
        }
    }
}
