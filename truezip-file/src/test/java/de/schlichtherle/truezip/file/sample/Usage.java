/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file.sample;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.fs.FsSyncException;
import de.schlichtherle.truezip.fs.FsSyncWarningException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A collection of code snippets to demonstrate the usage of the API in the
 * TrueZIP File* module.
 * Note that these code snippets are useless in isolation, so they should never
 * get called in application code.
 * 
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@SuppressWarnings("CallToThreadDumpStack")
abstract class Usage {

    /** Nope! */
    private Usage() {
    }

    void cat1(String path) throws IOException {
// START SNIPPET: cat1
        InputStream in = new TFileInputStream(path);
        try {
            TFile.cat(in, System.out);
        } finally {
            in.close(); // ALWAYS close the stream!
        }
// END SNIPPET: cat1
    }

    void cat2(String path) {
// START SNIPPET: cat2
        try {
            InputStream in = new TFileInputStream(path);
            try {
                TFile.cat(in, System.out);
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
            TFile.umount(); // with or without parameters
        } catch (FsSyncException ouch) {
            // Print the sequential I/O exception chain in order of descending
            // priority and ascending appearance.
            // This is the default so you wouldn't have to call sortPriority().
            ouch.sortPriority().printStackTrace();
            //ouch.printStackTrace(); // equivalent
        }
// END SNIPPET: umount1
    }

    void umount2() {
// START SNIPPET: umount2
        try {
            TFile.umount(); // with or without parameters
        } catch (FsSyncException ouch) {
            // Print the sequential I/O exception chain strictly in order of
            // ascending appearance instead.
            ouch.sortAppearance().printStackTrace();
        }
// END SNIPPET: umount2
    }

    void umount3() {
// START SNIPPET: umount3
        try {
            TFile.umount(); // with or without parameters
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

    void performance1() throws IOException {
// START SNIPPET: performance1
        String[] names = {"a", "b", "c"};
        int n = names.length;
        for (int i = 0; i < n; i++) { // n * ...
            TFile entry = new TFile("archive.zip", names[i]); // O(1)
            entry.createNewFile(); // O(1)
            TFile.umount(); // O(i + 1) !!
        }
        // Overall: O(n*n) !!!
// END SNIPPET: performance1
    }

    void performance2() throws IOException {
// START SNIPPET: performance2
        String[] names = {"a", "b", "c"};
        int n = names.length;
        for (int i = 0; i < n; i++) { // n * ...
            TFile entry = new TFile("archive.zip", names[i]); // O(1)
            entry.createNewFile(); // O(1)
        }
        TFile.umount(); // new file: O(1); modified: O(n)
        // Overall: O(n)
// END SNIPPET: performance2
    }

    void performance3() throws IOException {
// START SNIPPET: performance3
        String[] names = { "a", "b", "c" };
        int n = names.length;
        for (int i = 0; i < n; i++) { // n * ...
            TFile entry = new TFile("archive.zip", names[i]); // O(1)
            entry.createNewFile(); // First modification: O(1)
            entry.createNewFile(); // Second modification triggers remount: O(i + 1) !!
        }
        // Overall: O(n*n) !!!
// END SNIPPET: performance3
    }

    void performance4() throws IOException {
// START SNIPPET: performance4
        long time = System.currentTimeMillis();
        String[] names = { "a", "b", "c" };
        int n = names.length;
        for (int i = 0; i < n; i++) { // n * ...
            TFile entry = new TFile("archive.zip", names[i]); // O(1)
            entry.createNewFile(); // First modification: O(1)
            entry.setLastModified(time); // Second modification triggers remount: O(i + 1) !!
        }
        // Overall: O(n*n) !!!
// END SNIPPET: performance4
    }
}
