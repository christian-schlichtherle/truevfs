/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.sample;

import net.truevfs.file.*;
import net.truevfs.kernel.FsSyncException;
import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public abstract class KeyManagementITSuite {

    private static final Logger
            logger = Logger.getLogger(KeyManagementITSuite.class.getName());

    private static final String PREFIX = "tzp";
    private static final String EXTENSION = "eaff";
    private static final String PASSWORD = "top secret";

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    private byte[] data;
    private File temp;
    private TFile archive;
    

    @Before
    public void setUp() throws IOException {
        data = DATA.clone();
        temp = createTempFile();
        assertTrue(temp.delete());
    }

    @After
    public void tearDown() throws IOException {
        try {
            try {
                umount();
            } finally {
                final File temp = this.temp;
                this.temp = null;
                if (null != temp && temp.exists() && !temp.delete())
                    throw new IOException(temp + " (could not delete)");
            }
        } catch (final IOException ex) {
            logger.log(Level.FINEST,
                    "Failed to clean up test file (this may be just an aftermath):",
                    ex);
        }
    }

    private static File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() may cause archive.rm_r() to
        // fail in some cases - explain why!
        // For more info, refer to TFileTestSuite.
        return File.createTempFile(PREFIX, "." + EXTENSION).getCanonicalFile();
    }

    private void umount() throws FsSyncException {
        if (null != archive)
            TVFS.umount(archive);
    }

    @Test
    public void testSetPasswords1() throws IOException {
        archive = new TFile(temp, newArchiveDetector1(EXTENSION, PASSWORD));
        roundTrip();
    }

    protected abstract TArchiveDetector
    newArchiveDetector1(String extension, String password);

    @Test
    public void testSetPasswords2() throws IOException {
        archive = new TFile(temp, newArchiveDetector2(EXTENSION, PASSWORD));
        roundTrip();
    }

    protected abstract TArchiveDetector
    newArchiveDetector2(String extension, String password);

    private void roundTrip() throws IOException {
        final TFile file = new TFile(archive, "entry");
        final OutputStream os = new TFileOutputStream(file);
        try {
            os.write(data);
        } finally {
            os.close();
        }
        final ByteArrayOutputStream
                baos = new ByteArrayOutputStream(data.length);
        final InputStream is = new TFileInputStream(file);
        try {
            TFile.cat(is, baos);
        } finally {
            is.close();
        }
        Arrays.equals(data, baos.toByteArray());
    }
}
