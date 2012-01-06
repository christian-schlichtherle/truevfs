/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.rof;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class IntervalReadOnlyFileTest extends ReadOnlyFileTestSuite {

    private static final Logger logger = Logger.getLogger(
            IntervalReadOnlyFileTest.class.getName());

    private File temp2;

    @Before
    @Override
    public void setUp() throws IOException {
        temp2 = File.createTempFile(TEMP_FILE_PREFIX, null);
        try {
            final OutputStream out = new FileOutputStream(temp2);
            try {
                out.write(DATA);
                out.write(DATA);
                out.write(DATA);
            } finally {
                out.close();
            }
            assert 3 * DATA.length == temp2.length();
        } catch (IOException ex) {
            if (!temp2.delete())
                logger.log(Level.WARNING, "{0} (File.delete() failed)", temp2);
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
    public void tearDown() throws IOException {
        try {
            super.tearDown();
        } finally {
            if (temp2.exists() && !temp2.delete())
                logger.log(Level.WARNING, "{0} (could not delete)", temp2);
            
        }
    }
}
