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
