/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.sample.kernel;

import global.namespace.truevfs.comp.cio.InputSocket;
import global.namespace.truevfs.comp.io.Streams;
import global.namespace.truevfs.comp.util.BitField;
import global.namespace.truevfs.kernel.api.*;
import global.namespace.truevfs.kernel.api.sl.FsDriverMapLocator;
import global.namespace.truevfs.kernel.api.sl.FsManagerLocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * A poor man's blend of the cat(1) and wget(1) command line utility
 * for concatenating the contents of the parameter URIs on the standard output.
 * This version can address any URI scheme which is supported by a file system
 * driver which is available on the run-time class path.
 *
 * @author Christian Schlichtherle
 * @see <a href="http://www.gnu.org/software/wget/">GNU Cat - Home Page</a>
 */
public final class Cat {

    /**
     * You cannot instantiate this class.
     */
    private Cat() {
    }

    public static void main(String[] args)
            throws IOException, URISyntaxException {
        for (String path : args)
            cat(path);
    }

// START SNIPPET: cat

    /**
     * Copies the contents of the parameter resource to the standard output.
     *
     * @param resource the URI string of the resource to copy.
     * @throws IOException              if accessing the resource results in an I/O error.
     * @throws IllegalArgumentException if {@code resource} does not
     *                                  conform to the syntax constraints for {@link URI}s.
     */
    static void cat(String resource)
            throws IOException, URISyntaxException {
        // Get a manager for the life cycle of controllers for federated
        // file systems.
        FsManager manager = FsManagerLocator.SINGLETON.get();
        try {
            // Search the class path for the set of all supported file system
            // drivers and build a composite driver from it.
            FsCompositeDriver driver = new FsSimpleCompositeDriver(
                    FsDriverMapLocator.SINGLETON);
            // Resolve the source socket.
            // Note that an absolute URI is required, so we may need to use the
            // File class for transformation from a normal path name.
            // Using the File class rather than the TFile class implies that
            // the caller cannot specify an archive file in a path name.
            // To overcome this limitation, you should use a TFile instead.
            // Unfortunately, this would introduce a cyclic dependency on the
            // module TrueVFS Access File*, so it's not an option for this sample.
            URI uri = new URI(resource);
            uri = uri.isAbsolute() ? uri : new File(resource).toURI();
            FsNodePath path = FsNodePath.create(uri, FsUriModifier.CANONICALIZE);
            InputSocket<?> socket = manager
                    .controller(driver, path.getMountPoint().get())
                    .input(BitField.noneOf(FsAccessOption.class), path.getNodeName());
            try (InputStream in = socket.stream(Optional.empty())) {
                Streams.cat(in, System.out); // copy the data
            }
        } finally {
            // Commit all unsynchronized changes to the contents of federated
            // file systems, if any were accessed, and clean up temporary files
            // used for caching.
            new FsSync()
                    .manager(manager)
                    .options(FsSyncOptions.UMOUNT)
                    .run();
        }
    }
// END SNIPPET: cat
}
