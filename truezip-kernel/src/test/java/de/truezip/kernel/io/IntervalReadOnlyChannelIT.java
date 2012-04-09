/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;

/**
 * @author Christian Schlichtherle
 */
public class IntervalReadOnlyChannelIT extends ReadOnlyChannelITSuite {

    private static final Logger
            logger = Logger.getLogger(IntervalReadOnlyChannelIT.class.getName());

    private Path temp2;

    @Before
    @Override
    public void setUp() throws IOException {
        temp2 = createTempFile(TEMP_FILE_PREFIX, null);
        try {
            try (final OutputStream out = newOutputStream(temp2)) {
                out.write(DATA);
                out.write(DATA);
                out.write(DATA);
            }
            assert 3 * DATA.length == size(temp2);
        } catch (final Throwable ex) {
            try {
                delete(temp2);
            } catch (final Throwable ex2) {
                ex.addSuppressed(ex2);
            }
            throw ex;
        }
        super.setUp(); // calls newChannel(Path)
    }

    @Override
    protected SeekableByteChannel newChannel(Path path) throws IOException {
        final SeekableByteChannel sbc = newByteChannel(temp2);
        sbc.position(DATA.length);
        return new IntervalReadOnlyChannel(sbc, DATA.length);
    }

    @After
    @Override
    public void tearDown() {
        try {
            super.tearDown();
        } finally {
            try {
                deleteIfExists(temp2);
            } catch (final IOException ex) {
                logger.log(Level.FINEST,
                        "Failed to clean up test file (this may be just an aftermath):",
                        ex);
            }
        }
    }
}
