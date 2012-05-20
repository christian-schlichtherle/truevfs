/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.file.sample;

import net.truevfs.file.TFile;
import net.truevfs.file.TFileInputStream;
import net.truevfs.file.TVFS;
import net.truevfs.kernel.FsSyncException;
import net.truevfs.kernel.FsSyncWarningException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A collection of code snippets to demonstrate the usage of the API in the
 * TrueVFS Access File* module.
 * Note that these code snippets are useless in isolation, so they should never
 * get called in application code.
 * 
 * @author  Christian Schlichtherle
 */
@SuppressWarnings("CallToThreadDumpStack")
abstract class Usage {

    /** Can't touch this - hammer time! */
    private Usage() { }

    void cat1(String path) throws IOException {
// START SNIPPET: cat1
        try (InputStream in = new TFileInputStream(path)) {
            TFile.cat(in, System.out);
        }
// END SNIPPET: cat1
    }

    void cat2(String path) {
// START SNIPPET: cat2
        try {
            try (InputStream in = new TFileInputStream(path)) {
                TFile.cat(in, System.out);
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

    void performance1() throws IOException {
// START SNIPPET: performance1
        String[] names = {"a", "b", "c"};
        int n = names.length;
        for (int i = 0; i < n; i++) { // n * ...
            TFile entry = new TFile("archive.zip", names[i]); // O(1)
            entry.createNewFile(); // O(1)
            TVFS.umount(); // O(i + 1) !!
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
        TVFS.umount(); // new file: O(1); modified: O(n)
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