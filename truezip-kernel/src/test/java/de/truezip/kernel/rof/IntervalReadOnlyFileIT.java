/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.rof;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;

/**
 * @author Christian Schlichtherle
 */
public final class IntervalReadOnlyFileIT extends ReadOnlyFileTestSuite {

    private static final Logger
            logger = Logger.getLogger(IntervalReadOnlyFileIT.class.getName());

    private File temp2;

    @Before
    @Override
    public void setUp() throws IOException {
        temp2 = File.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try (final OutputStream out = new FileOutputStream(temp2)) {
                out.write(DATA);
                out.write(DATA);
                out.write(DATA);
            }
            assert 3 * DATA.length == temp2.length();
        } catch (final IOException ex) {
            if (!temp2.delete())
                throw new IOException(temp2 + " (could not delete)", ex);
            throw ex;
        }
        super.setUp(); // calls newReadOnlyFile(File)
    }

    @Override
    protected ReadOnlyFile newReadOnlyFile(File file) throws IOException {
        final ReadOnlyFile rof = new DefaultReadOnlyFile(temp2);
        rof.seek(DATA.length);
        return new IntervalReadOnlyFile(rof, DATA.length);
    }

    @After
    @Override
    public void tearDown() {
        try {
            super.tearDown();
        } finally {
            try {
                if (temp2.exists() && !temp2.delete())
                    throw new IOException(temp2 + " (could not delete)");
            } catch (IOException ex) {
                logger.log(Level.FINEST,
                        "Failed to clean up test file (this may be just an aftermath):",
                        ex);
            }
        }
    }
}
