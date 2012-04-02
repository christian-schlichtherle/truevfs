/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.file.sample;

import de.truezip.file.TApplication;
import de.truezip.file.TConfig;
import de.truezip.file.TFile;
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
@Deprecated
public final class PathCat extends TApplication<IOException> {

    /** Equivalent to {@code System.exit(new PathCat().run(args));}. */
    public static void main(String[] args) throws IOException {
        System.exit(new PathCat().run(args));
    }

    @Override
    protected void setup() { }

    @Override
    protected int work(String[] args) throws IOException {
        for (String path : args)
            pathCat(path);
        return 0;
    }

// START SNIPPET: cat
    /**
     * Copies the contents of the parameter resource to the standard output.
     * <p>
     * The set of archive file extensions detected by this method is determined
     * by the {@linkplain TConfig#getArchiveDetector default archive detector}
     * and the respective file system driver providers on the class path.
     *
     * @param  resource the path name string of the resource to copy.
     * @throws IOException if accessing the resource results in an I/O error.
     */
    static void pathCat(String resource) throws IOException {
        new TFile(resource).output(System.out);
    }
// END SNIPPET: cat
}