/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.file.sample;

import net.truevfs.file.TApplication;
import net.truevfs.file.TConfig;
import net.truevfs.file.TFile;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A poor man's imitate of the cat(1) command line utility
 * for concatenating the contents of each parameter URI on the standard output.
 * The URI must be file-based, i.e. the top level file system scheme must
 * be {@code file}.
 *
 * @deprecated Use the Maven archetype for the module TrueVFS Access File* instead.
 *             Its group ID is {@code de.schlichtherle.truevfs}.
 *             Its artifact ID is {@code truevfs-archetype-file}.
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
@Deprecated
public final class UriCat extends TApplication<Exception> {

    /** Equivalent to {@code System.exit(new CatPath().run(args));}. */
    public static void main(String[] args) throws Exception {
        System.exit(new UriCat().run(args));
    }

    @Override
    protected void setup() { }

    @Override
    protected int work(String[] args) throws Exception {
        for (String path : args)
            uriCat(path);
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
     * @param  resource the URI string of the resource to copy.
     *         The URI must be file-based, i.e. the top level file system
     *         scheme must be {@code file}.
     * @throws IOException if accessing the resource results in an I/O error.
     * @throws URISyntaxException if {@code resource} does not
     *         conform to the syntax constraints for {@link URI}s.
     */
    static void uriCat(String resource) throws IOException, URISyntaxException {
        URI uri = new URI(resource);
        TFile file = uri.isAbsolute() ? new TFile(uri) : new TFile(resource);
        file.output(System.out);
    }
// END SNIPPET: cat
}