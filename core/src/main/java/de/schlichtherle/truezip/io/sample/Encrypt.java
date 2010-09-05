/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

package de.schlichtherle.truezip.io.sample;

import de.schlichtherle.truezip.io.file.File;
import de.schlichtherle.truezip.io.file.RaesFiles;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ResourceBundle;

/**
 * Encrypts the contents of the file provided as the first argument
 * into the RAES file provided as the second argument for the main method.
 * <p>
 * Please note that you should not use this utility to encrypt a plain
 * ZIP file to an RAES encrypted ZIP file (usually a files with a
 * {@code ".tzp"} or {@code ".zip.rae"} suffix).
 * This is because RAES encrypted ZIP files use the &quot;UTF-8&quot;
 * as their character set, whereas plain ZIP files use &quot;IBM437&quot;,
 * a.k.a. &quot;CP437&quot;.
 * To encrypt a plain ZIP file to an RAES encrypted ZIP file, use the
 * {@code "cp"} command of the {@link NZip} class instead.
 * This class knows about the correct character sets for the various
 * flavours of ZIP compatible files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class Encrypt extends CommandLineUtility {

    private static final String CLASS_NAME
            = "de.schlichtherle.truezip.io.samples.Encrypt";
    private static final ResourceBundle resources
            = ResourceBundle.getBundle(CLASS_NAME);

    public Encrypt() {
    }

    public Encrypt(OutputStream out, OutputStream err, boolean autoFlush) {
        super(out, err, autoFlush);
    }

    /** Equivalent to {@code System.exit(new Encrypt().run(args));}. */
    public static void main(final String[] args) {
        System.exit(new Encrypt().run(args));
    }

    public boolean runWithException(final String[] args)
    throws IllegalUsageException, IOException {
        if (args.length != 2)
            throw new IllegalUsageException();

        if (new File(args[1]).isEntry())
            progressMonitor.start();

        RaesFiles.encrypt(args[0], args[1]);
        return true;
    }

    public class IllegalUsageException
            extends CommandLineUtility.IllegalUsageException {
        private static final long serialVersionUID = 2456163893462987342L;

        private IllegalUsageException() {
            super(resources.getString("usage"));
        }
    } // class IllegalUsageException
}
