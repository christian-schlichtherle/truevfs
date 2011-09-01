/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;
import java.io.IOException;

/**
 * A poor man's imitate of the cat(1) command line utility
 * for concatenating the contents of each parameter path name on the standard
 * output.
 *
 * @deprecated Use the Maven archetype for the module TrueZIP File* instead.
 *             Its group ID is {@code de.schlichtherle.truezip}.
 *             Its artifact ID is {@code truezip-archetype-file}.
 * @see        <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
public final class PathCat extends Application {

    /** Equivalent to {@code System.exit(new PathCat().run(args));}. */
    public static void main(String[] args) throws FsSyncException {
        System.exit(new PathCat().run(args));
    }

    @Override
    protected int runChecked(String[] args) throws IOException {
        for (String path : args)
            pathCat(path);
        return 0;
    }

// START SNIPPET: cat
    /**
     * Copies the contents of the parameter resource to the standard output.
     * <p>
     * The set of archive file suffixes detected by this method is determined
     * by the {@link TFile#getDefaultArchiveDetector() default archive detector}
     * and the respective file system driver providers on the class path.
     *
     * @param  resource the path name string of the resource to copy.
     * @throws IOException if accessing the resource results in an I/O error.
     */
    static void pathCat(String resource) throws IOException {
        TFile file = new TFile(resource);
        file.output(System.out);
    }
// END SNIPPET: cat
}
