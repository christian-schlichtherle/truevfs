/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes.sample;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.FsSyncException;
import static de.schlichtherle.truezip.fs.archive.zip.sample.KeyManagementIT.roundTripTest;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Deprecated
public class KeyManagementIT {

    private static final Logger logger = Logger.getLogger(
            KeyManagementIT.class.getName());

    private static final String PREFIX = "tzp";
    private static final String SUFFIX = "eaff";
    private static final String PASSWORD = "secret";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    private File temp;
    private byte[] data;

    @Before
    public void setUp() throws IOException {
        TFile.umount();
        temp = createTempFile();
        assertTrue(temp.delete());
        data = DATA.clone();
    }

    private static File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() may cause archive.rm_r() to
        // fail in some cases - explain why!
        // For more info, refer to TFileTestSuite.
        return File.createTempFile(PREFIX, "." + SUFFIX).getCanonicalFile();
    }

    @After
    public void tearDown() {
        // sync now to delete temps and free memory.
        // This prevents subsequent warnings about left over temporary files
        // and removes cached data from the memory, so it helps to start on a
        // clean sheet of paper with subsequent tests.
        try {
            TFile.umount();
        } catch (FsSyncException ex) {
            logger.log(Level.WARNING, ex.toString(), ex);
        }
        if (temp.exists() && !temp.delete())
            logger.log(Level.WARNING, "{0} (could not delete)", temp);
    }

    @Test
    public void testSetPasswords1() throws IOException {
        TArchiveDetector detector = KeyManagement.newArchiveDetector1(
                TFile.getDefaultArchiveDetector(),
                SUFFIX,
                PASSWORD.toCharArray());
        roundTripTest(new TFile(temp, detector), data);
    }

    @Test
    public void testSetPasswords2() throws IOException {
        TArchiveDetector detector = KeyManagement.newArchiveDetector2(
                TFile.getDefaultArchiveDetector(),
                SUFFIX,
                PASSWORD.toCharArray());
        roundTripTest(new TFile(temp, detector), data);
    }
}
