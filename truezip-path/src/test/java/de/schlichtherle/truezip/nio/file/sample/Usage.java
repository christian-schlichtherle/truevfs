/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file.sample;

import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import de.schlichtherle.truezip.io.SequentialIOException;
import de.schlichtherle.truezip.io.Streams;
import de.schlichtherle.truezip.nio.file.TFileSystemProvider;
import de.schlichtherle.truezip.nio.file.TPath;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
@DefaultAnnotation(NonNull.class)
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
