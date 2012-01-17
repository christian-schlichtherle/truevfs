/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.sample;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Random;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class KeyManagementIT {

    private static final String PREFIX = "tzp";
    private static final String SUFFIX = "eaff";
    private static final String PASSWORD = "secret";
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

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
    public void tearDown() throws IOException {
        try {
            TFile.umount();
        } finally {
            if (temp.exists() && !temp.delete())
                throw new IOException(temp + " (could not delete)");
        }
    }

    @Test
    public void testSetPasswords1() throws IOException {
        TArchiveDetector detector = KeyManagement.newArchiveDetector1(
                TFile.getDefaultArchiveDetector(),
                SUFFIX,
                PASSWORD.getBytes(US_ASCII));
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

    public static void roundTripTest(TFile archive, byte[] data)
    throws IOException {
        TFile file = new TFile(archive, "entry");
        OutputStream out = new TFileOutputStream(file);
        try {
            out.write(data);
        } finally {
            out.close();
        }
        out = new ByteArrayOutputStream(data.length);
        InputStream in = new TFileInputStream(file);
        try {
            TFile.cat(in, out);
        } finally {
            in.close();
        }
        Arrays.equals(data, ((ByteArrayOutputStream) out).toByteArray());
    }
}
