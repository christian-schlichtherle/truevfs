/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.path.sample;

import de.truezip.file.TVFS;
import de.truezip.kernel.FsSyncException;
import de.truezip.kernel.FsSyncWarningException;
import de.truezip.kernel.io.Streams;
import de.truezip.path.TPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * A collection of code snippets to demonstrate the usage of the API in the
 * TrueZIP Path module.
 *
 * @deprecated This class contains code snippets which are useless in
 *             isolation, so it should not get used in applications.
 * @author     Christian Schlichtherle
 * @version    $Id$
 */
@Deprecated
@SuppressWarnings("CallToThreadDumpStack")
abstract class Usage {

    /** Can't touch this - hammer time! */
    private Usage() { }

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