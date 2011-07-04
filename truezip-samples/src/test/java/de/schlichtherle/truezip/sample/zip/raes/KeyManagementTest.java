/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.schlichtherle.truezip.sample.zip.raes;

import de.schlichtherle.truezip.fs.FsSyncException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author     Christian Schlichtherle
 * @version    $Id$
 * @deprecated Just because the unit under test has been deprecated.
 */
public class KeyManagementTest {

    private static final Logger logger = Logger.getLogger(
            KeyManagementTest.class.getName());

    private static boolean lenientBackup;
    private static TArchiveDetector detectorBackup;

    private static final String TEMP_FILE_PREFIX = "tzp";
    private static final String TEMP_FILE_SUFFIX = ".tzp";

    private static final Random rnd = new Random();

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        rnd.nextBytes(DATA);
    }

    private File temp;
    private char[] password;
    private byte[] data;

    @BeforeClass
    public static void setUpClass() {
        lenientBackup = TFile.isLenient();
        detectorBackup = TFile.getDefaultArchiveDetector();
    }

    @Before
    public void setUp() throws IOException {
        TFile.umount();
        temp = createTempFile();
        assertTrue(temp.delete());
        password = "secret".toCharArray();
        data = DATA.clone();
    }

    private static File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() may cause archive.rm_r() to
        // fail in some cases - explain why!
        // For more info, refer to TFileTestSuite.
        return File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX).getCanonicalFile();
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
        TFile.setDefaultArchiveDetector(detectorBackup);
        TFile.setLenient(lenientBackup);
    }

    @Test
    public void testSetPassword() throws IOException {
        final TFile archive = new TFile(temp);
        KeyManagement.setPassword(archive, password);
        makeArchive(archive);
    }

    @Test
    public void testSetAllPasswords() throws IOException {
        KeyManagement.setAllPasswords(password);
        makeArchive(new TFile(temp));
    }

    private void makeArchive(TFile archive) throws IOException {
        OutputStream out = new TFileOutputStream(new TFile(archive, "entry"));
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
