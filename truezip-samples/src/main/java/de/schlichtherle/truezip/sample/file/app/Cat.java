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
import de.schlichtherle.truezip.io.Streams;
import java.io.IOException;
import java.io.InputStream;

/**
 * A poor man's cat(1) command line utility which concatenates the contents
 * of the given files on the standard output.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Cat extends CommandLineUtility {

    /** Equivalent to {@code System.exit(new Cat().run(args));}. */
    public static void main(String[] args) {
        System.exit(new Cat().run(args));
    }

    @Override
    public int runChecked(String[] args) throws IOException {
        for (String path : args)
            cat(path);
        return 0;
    }

    private void cat(String path) throws IOException {
        // START SNIPPET: cat
        InputStream in = new TFileInputStream(path);
        try {
            Streams.cat(in, System.out);
        } finally {
            in.close();
        }
        // END SNIPPET: cat
    }
}
