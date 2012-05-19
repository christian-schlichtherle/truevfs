/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.sample;

import de.truezip.kernel.*;
import static de.truezip.kernel.FsAccessOption.*;
import de.truezip.kernel.cio.IOSockets;
import de.truezip.kernel.cio.InputSocket;
import de.truezip.kernel.cio.OutputSocket;
import de.truezip.kernel.sl.FsDriverLocator;
import de.truezip.kernel.sl.FsManagerLocator;
import de.truezip.kernel.util.BitField;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * A poor man's blend of the cp(1) or curl(1) command line utilities
 * for copying the contents of the first parameter URI to the second parameter
 * URI.
 * 
 * @deprecated Since TrueVFS 7.2, the new TrueVFS Path API provides the same
 *             functionality with much more ease and comfort.
 *             Use the Maven archetype for the module TrueVFS Path instead.
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
     * @throws IOException if accessing the targets results in an I/O error.
     * @throws IllegalArgumentException if {@code src} or {@code src} does not
     *         conform to the syntax constraints for {@link URI}s.
     */
    static void copy(String src, String dst) throws IOException {
        // Get a manager for the life cycle of controllers for federated
        // file systems.
        FsManager manager = FsManagerLocator.SINGLETON.getManager();
        try {
            // Search the class path for the set of all supported file system
            // drivers and build a composite driver from it.
            FsCompositeDriver driver = new FsSimpleCompositeDriver(
                    FsDriverLocator.SINGLETON);
            // Resolve the source socket.
            // Note that an absolute URI is required, so we may need to use the
            // File class for transformation from a normal path name.
            // Using the File class rather than the TFile class implies that
            // the caller cannot specify an archive file in a path name.
            // To overcome this limitation, you should use a TFile instead.
            // Unfortunately, this would introduce a cyclic dependency on the
            // module TrueVFS File*, so it's not an option for this sample.
            URI srcUri = URI.create(src);
            srcUri = srcUri.isAbsolute() ? srcUri : new File(src).toURI();
            FsPath srcPath = FsPath.create(srcUri, FsUriModifier.CANONICALIZE);
            InputSocket<?> srcSocket = manager
                    .controller(driver, srcPath.getMountPoint())
                    .input(FsAccessOptions.NONE, srcPath.getEntryName());
            // Resolve the destination socket. Again, we need an absolute URI.
            URI dstUri = URI.create(dst);
            dstUri = dstUri.isAbsolute() ? dstUri : new File(dst).toURI();
            FsPath dstPath = FsPath.create(dstUri, FsUriModifier.CANONICALIZE);
            OutputSocket<?> dstSocket = manager
                    .controller(driver, dstPath.getMountPoint())
                    .output(BitField.of(CREATE_PARENTS, EXCLUSIVE),
                            dstPath.getEntryName(),
                            srcSocket.localTarget());
            IOSockets.copy(srcSocket, dstSocket);
        } finally {
            // Commit all unsynchronized changes to the contents of federated
            // file systems, if any were accessed, and clean up temporary files
            // used for caching.
            manager.sync(FsSyncOptions.UMOUNT);
        }
    }
// END SNIPPET: copy
}
