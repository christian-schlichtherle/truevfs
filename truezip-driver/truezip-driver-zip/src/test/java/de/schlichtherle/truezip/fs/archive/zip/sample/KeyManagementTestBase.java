/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.sample;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.file.TFileOutputStream;
import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public class KeyManagementTestBase {

    private static final Logger logger = Logger.getLogger(
            KeyManagementTestBase.class.getName());

    private static final String PREFIX = "tzp";
    protected static final String SUFFIX = "eaff";
    protected static final String PASSWORD = "secret";

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    protected File temp;
    protected byte[] data;

    @Before
    public void setUp() throws IOException {
        temp = createTempFile();
        assertTrue(temp.delete());
        data = DATA.clone();
    }

    @After
    public void tearDown() throws IOException {
        try {
            try {
                TFile.umount();
            } finally {
                final File temp = this.temp;
                this.temp = null;
                if (null != temp && temp.exists() && !temp.delete())
                    throw new IOException(temp + " (could not delete)");
            }
        } catch (final IOException ex) {
            logger.log(Level.INFO,
                    "Failed to clean up temporary archive file (this error may be just implied)!",
                    ex);
        }
    }

    private static File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() may cause archive.rm_r() to
        // fail in some cases - explain why!
        // For more info, refer to TFileTestSuite.
        return File.createTempFile(PREFIX, "." + SUFFIX).getCanonicalFile();
    }

    protected static void roundTripTest(final TFile archive, final byte[] data)
    throws IOException {
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
