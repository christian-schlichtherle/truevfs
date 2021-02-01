/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import global.namespace.truevfs.comp.io.Streams;
import global.namespace.truevfs.access.TPath;
import global.namespace.truevfs.access.TVFS;
import global.namespace.truevfs.kernel.spec.FsSyncException;
import global.namespace.truevfs.kernel.spec.FsSyncWarningException;

/**
 * A collection of code snippets to demonstrate the usage of the TPath class.
 *
 * @author Christian Schlichtherle
 */
@SuppressWarnings("CallToThreadDumpStack")
abstract class PathUsage {

    private PathUsage() { }

    void cat1(String name) throws IOException {
// START SNIPPET: cat1
        try (InputStream in = Files.newInputStream(new TPath(name))) {
            Streams.cat(in, System.out);
        }
// END SNIPPET: cat1
    }

    void cat2(String name) {
// START SNIPPET: cat2
        try {
            try (InputStream in = Files.newInputStream(new TPath(name))) {
                Streams.cat(in, System.out);
            }
        } catch (IOException ouch) {
            ouch.printStackTrace();
        }
// END SNIPPET: cat2
    }

    void umount1() {
// START SNIPPET: umount1
        try {
            TVFS.umount();
        } catch (FsSyncException ouch) {
            // This exception may have several suppressed exceptions for
            // different archive files.
            ouch.printStackTrace();
        }
// END SNIPPET: umount1
    }

    void umount2() {
// START SNIPPET: umount2
        try {
            TVFS.umount();
        } catch (FsSyncWarningException oops) {
            // Only objects of the class FsSyncWarningException may be
            // suppressed in this exception - we ignore this.
        } catch (FsSyncException ouch) {
            // At least one exception occured which is not just an
            // FsSyncWarningException.
            // This indicates loss of data and needs to get handled.
            ouch.printStackTrace();
        }
// END SNIPPET: umount2
    }
}