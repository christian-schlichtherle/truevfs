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
import de.schlichtherle.truezip.fs.FsCompositeDriver;
import de.schlichtherle.truezip.fs.FsDefaultDriver;
import de.schlichtherle.truezip.fs.FsInputOption;
import de.schlichtherle.truezip.fs.FsManager;
import de.schlichtherle.truezip.fs.FsOutputOption;
import de.schlichtherle.truezip.fs.FsPath;
import de.schlichtherle.truezip.fs.FsUriModifier;
import de.schlichtherle.truezip.fs.sl.FsDriverLocator;
import de.schlichtherle.truezip.fs.sl.FsManagerLocator;
import de.schlichtherle.truezip.socket.IOSocket;
import de.schlichtherle.truezip.socket.InputSocket;
import de.schlichtherle.truezip.socket.OutputSocket;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.net.URI;

/**
 * A poor man's blend of the cp(1) or curl(1) command line utilities
 * for copying the contents of the first parameter URI to the second parameter
 * URI.
 * 
 * @deprecated Since TrueZIP 7.2, the new TrueZIP Path API provides the same
 *             functionality with much more ease.
 *             Use the Maven archetype for the module TrueZIP Path instead.
 *             Its group ID is {@code de.schlichtherle.truezip}.
 *             Its artifact ID is {@code truezip-archetype-path}.
 * @see        <a href="http://curl.haxx.se/">cURL and libcurl - Home Page</a>
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
public final class Copy {

    /** You cannot instantiate this class. */
    private Copy() {
    }

    public static void main(String[] args) throws IOException {
        copy(args[0], args[1]);
    }

// START SNIPPET: copy
    /**
     * Copies the contents of the parameter source to the parameter destination.
     *
     * @param  src the URI string of the copy source.
     * @param  dst the URI string of the copy destination.
     * @throws IOException if accessing the peers results in an I/O error.
     * @throws IllegalArgumentException if {@code src} or {@code src} does not
     *         conform to the syntax constraints for {@link URI}s.
     */
    static void copy(String src, String dst) throws IOException {
        // Get a manager for the life cycle of controllers for federated
        // file systems.
        // Alternatively, we could use new FsDefaultManager();
        FsManager manager = FsManagerLocator.SINGLETON.get();
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
            URI srcUri = URI.create(src);
            srcUri = srcUri.isAbsolute() ? srcUri : new TFile(src).toURI();
            FsPath srcPath = FsPath.create(srcUri, FsUriModifier.CANONICALIZE);
            InputSocket<?> srcSocket = manager
                    .getController(     srcPath.getMountPoint(), driver)
                    .getInputSocket(    srcPath.getEntryName(),
                                        BitField.noneOf(FsInputOption.class));
            // Resolve the destination socket. Again, we need an absolute URI.
            URI dstUri = URI.create(dst);
            dstUri = dstUri.isAbsolute() ? dstUri : new TFile(dst).toURI();
            FsPath dstPath = FsPath.create(dstUri, FsUriModifier.CANONICALIZE);
            OutputSocket<?> dstSocket = manager
                    .getController(     dstPath.getMountPoint(), driver)
                    .getOutputSocket(   dstPath.getEntryName(),
                                        BitField.of(FsOutputOption.CREATE_PARENTS,
                                                    FsOutputOption.EXCLUSIVE),
                                        srcSocket.getLocalTarget());
            IOSocket.copy(srcSocket, dstSocket); // copy the data
        } finally {
            // Commit all unsynchronized changes to the contents of federated
            // file systems, if any were accessed, and clean up temporary files
            // used for caching.
            manager.sync(FsManager.UMOUNT);
        }
    }
// END SNIPPET: copy
}
