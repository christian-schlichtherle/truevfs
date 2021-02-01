/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access.sample;

import java.io.IOException;
import global.namespace.truevfs.access.TApplication;
import global.namespace.truevfs.access.TFile;

/**
 * A poor man's imitate of the cat(1) command line utility
 * for concatenating the contents of each parameter path name on the standard
 * output.
 *
 * @deprecated Use the Maven archetype for the module TrueVFS Access instead.
 *             Its group ID is {@code net.java.truevfs}.
 *             Its artifact ID is {@code truevfs-archetype-access}.
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
     * by the current archive detector
     * {@code TConfig.current().getArchiveDetector()}
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