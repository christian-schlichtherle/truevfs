/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.sample.kernel.app;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsDefaultDriver;
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsDefaultManager;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsUriModifier;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A poor man's blend of the cat(1) and wget(1) command line utility
 * for concatenating the contents of the parameter URIs on the standard output.
 * This version can address any URI scheme which is supported by a file system
 * driver which is available on the run-time class path.
 *
 * @see <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class Cat {

    public static void main(String[] args) throws IOException {
        for (String path : args)
            cat(path);
    }

// START SNIPPET: cat
    /**
     * Copies the contents of the parameter resource to the standard output.
     *
     * @param  resource the URI string of the resource to copy.
     * @throws IOException if accessing the resource results in an I/O error.
     * @throws IllegalArgumentException if {@code resource} does not
     *         conform to the syntax constraints for {@link URI}s.
     */
    static void cat(String resource) throws IOException {
        // Create a manager for the life cycle of controllers for federated
        // file systems.
        // Alternatively, we could use FsManagerLocator.SINGLETON.get();
        FsManager manager = new FsDefaultManager();
        try {
            // Search the class path for the set of all supported file system
            // drivers and build a composite driver from it.
            FsCompositeDriver
                    driver = new FsDefaultDriver(FsDriverLocator.SINGLETON);
            // Resolve the source socket.
            // Note that an absolute URI is required, so we may need to use the
            // TFile class for transformation from a normal path name.
            // Using the TFile class rather than the File class enables the
            // caller to specify archive files in a path name, but at the cost
            // of adding a dependency on the TrueZIP File* module.
            URI uri = URI.create(resource);
            uri = uri.isAbsolute() ? uri : new TFile(resource).toURI();
            FsPath path = FsPath.create(uri, FsUriModifier.CANONICALIZE);
            InputSocket<?> socket = manager
                    .getController(     path.getMountPoint(), driver)
                    .getInputSocket(    path.getEntryName(),
                                        BitField.noneOf(FsInputOption.class));
            // Copy the data.
            // For this small example, we could skip the call to in.close() or
            // use Streams.copy(in, out), but this would not be correct if this
            // were not just the end of the application.
            InputStream in = socket.newInputStream();
            try {
                // Copy the data.
                Streams.cat(in, System.out);
            } finally {
                in.close(); // ALWAYS close the stream!
            }
        } finally {
            // Commit all unsynchronized changes to the contents of federated
            // file systems, if any were accessed, and clean up temporary files
            // used for caching.
            manager.sync(FsManager.UMOUNT);
        }
    }
// END SNIPPET: cat
}
