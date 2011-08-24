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
package de.schlichtherle.truezip.sample.file.nio;

import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.nio.file.TFileSystemProvider;
import de.schlichtherle.truezip.nio.file.TPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A collection of code snippets to demonstrate the usage of the API in the
 * TrueZIP Path module.
 * Because most of the snippets are useless in isolation, this class is not
 * available for applications.
 * The benefit of having this class at all is that these code snippets get
 * checked by the compiler and are subject for automated refactoring.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
abstract class Usage {

    /** Nope! */
    private Usage() {
    }

    void cat1(String name) throws IOException {
// START SNIPPET: cat1
        Path path = new TPath(name);
        InputStream in = Files.newInputStream(path);
        try {
            Streams.cat(in, System.out);
        } finally {
            in.close(); // ALWAYS close the stream!
        }
// END SNIPPET: cat1
    }

    void cat2(String name) {
// START SNIPPET: cat2
        try {
            Path path = new TPath(name);
            InputStream in = Files.newInputStream(path);
            try {
                Streams.cat(in, System.out);
            } finally {
                in.close(); // ALWAYS close the stream!
            }
        } catch (IOException ouch) {
            ouch.printStackTrace();
        }
// END SNIPPET: cat2
    }

    void umount1() {
// START SNIPPET: umount1
        try {
            TFileSystemProvider.umount(); // with or without parameters
        } catch (SequentialIOException ouch) {
            // Print the sequential I/O exception chain in order of
            // descending priority and ascending appearance.
            ouch.sortPriority().printStackTrace();
            //ouch.printStackTrace(); // equivalent
        }
// END SNIPPET: umount1
    }

    void umount2() {
// START SNIPPET: umount2
        try {
            TFileSystemProvider.umount(); // with or without parameters
        } catch (SequentialIOException ouch) {
            // Print the sequential I/O exception chain in order of
            // appearance instead.
            ouch.sortAppearance().printStackTrace();
        }
// END SNIPPET: umount2
    }

    void umount3() {
// START SNIPPET: umount3
        try {
            TFileSystemProvider.umount(); // with or without parameters
        } catch (FsSyncWarningException oops) {
            // Only objects of the class FsSyncWarningException exist in
            // the exception chain - we ignore this.
        } catch (FsSyncException ouch) {
            // At least one exception occured which is not just an
            // FsSyncWarningException.
            // This indicates loss of data and needs to be handled.
            // Print the sequential I/O exception chain in order of
            // descending priority and ascending appearance.
            ouch.printStackTrace();
            //ouch.sortPriority().printStackTrace(); // equivalent
        }
// END SNIPPET: umount3
    }
}
