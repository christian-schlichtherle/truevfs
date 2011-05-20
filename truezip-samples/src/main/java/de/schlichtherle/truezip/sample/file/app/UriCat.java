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
package de.schlichtherle.truezip.sample.file.app;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.io.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A poor man's imitate of the cat(1) command line utility
 * for concatenating the contents of each parameter URI on the standard output.
 * The URI must be file-based, i.e. the top level file system scheme must
 * be {@code file}.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class UriCat extends CommandLineUtility {

    /** Equivalent to {@code System.exit(new CatPath().run(args));}. */
    public static void main(String[] args) throws FsSyncException {
        System.exit(new UriCat().run(args));
    }

    @Override
    protected int runChecked(String[] args)
    throws IOException, URISyntaxException {
        for (String path : args)
            uriCat(path);
        return 0;
    }

// START SNIPPET: cat
    /**
     * Copies the contents of the parameter resource to the standard output.
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
        InputStream in = new TFileInputStream(file);
        try {
            // Copy the data.
            Streams.cat(in, System.out);
        } finally {
            in.close(); // ALWAYS close the stream!
        }
    }
// END SNIPPET: cat
}
