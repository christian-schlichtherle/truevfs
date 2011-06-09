/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.fs.file.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import static java.nio.file.Files.*;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

import static de.schlichtherle.truezip.fs.file.nio.FileController.isCreatableOrWritable;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileControllerTest {

    private static final Logger logger = Logger.getLogger(
            FileControllerTest.class.getName());

    /**
     * Note that this test is not quite correctly designed: It tests the
     * operating system rather than the method.
     */
    @Test
    @Deprecated
    public void testIsWritableOrCreatable() throws IOException {
        final Path file = createTempFile("tzp-test", null);
        boolean result = isCreatableOrWritable(file);
        assertTrue(result);
        boolean total = true;
        final InputStream in = newInputStream(file);
        try {
            result = isCreatableOrWritable(file);
            total &= result;
        } finally {
            in.close();
        }
        if (!result)
            logger.finer("Overwriting a file which has an open FileInputStream is not tolerated!");
        final String[] modes = { "r", "rw", "rws", "rwd" };
        for (int i = 0, l = modes.length; i < l; i++) {
            final String mode = modes[i];
            final RandomAccessFile raf = new RandomAccessFile(file.toFile(), mode);
            try {
                result = isCreatableOrWritable(file);
                total &= result;
            } finally {
                raf.close();
            }
            if (!result)
                logger.log(Level.FINER, "Overwriting a file which has an open RandomAccessFile in \"{0}\" mode is not tolerated!", mode);
        }
        if (!total)
            logger.finer(
                    "Applications should ALWAYS close their streams or you may face strange 'errors'.\n"
                    + "Note that this issue is NOT AT ALL specific to TrueZIP, but rather imposed by this platform!");
        delete(file);
    }
}
