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

/**
 * A poor man's imitate of the cat(1) command line utility
 * for concatenating the contents of each parameter path name on the standard
 * output.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class PathCat extends CommandLineUtility {

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
     *
     * @param  resource the path name string of the resource to copy.
     * @throws IOException if accessing the resource results in an I/O error.
     */
    static void pathCat(String resource) throws IOException {
        // Unless an explicit call to TFile.setDefaultArchiveDetector() has
        // been made, the TFile class will recognize any canonical archive file
        // suffix in a path name for which a file system driver is available on
        // the class path at run-time.
        // This statement is actually superfluous: The resource string could be
        // directly used as the parameter for the TFileInputStream.
        // It's only shown to explain what's going on behind the curtains.
        TFile file = new TFile(resource);
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
