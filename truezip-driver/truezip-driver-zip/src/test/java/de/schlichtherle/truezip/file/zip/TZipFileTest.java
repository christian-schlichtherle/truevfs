/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.file.zip;

import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileTestSuite;
import de.schlichtherle.truezip.file.TFileWriter;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
import static de.schlichtherle.truezip.fs.FsOutputOption.*;
import de.schlichtherle.truezip.fs.FsScheme;
import de.schlichtherle.truezip.util.BitField;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TZipFileTest extends TFileTestSuite {

    public TZipFileTest() {
        super(FsScheme.create("zip"), new ZipDriver(IO_POOL_PROVIDER));
    }

    /*private static final String TEXT = "Hello world!";

    @Test
    public void testGrow() throws IOException {
        final TFile entry = new TFile(getArchive(), "entry");
        assertPrint(entry);

        final TFile file = newNonArchiveFile(getArchive());
        final long size = file.length();
        assertTrue(size > 0);

        final TConfig config = TConfig.push();
        try {
            config.setOutputPreferences(BitField.of(CREATE_PARENTS, GROW));
            assertPrint(entry);
        } finally {
            config.close();
        }

        assertTrue(file.length() >= 2 * size);
    }

    private void assertPrint(final TFile entry) throws IOException {
        final PrintWriter out = new PrintWriter(new TFileWriter(entry));
        try {
            out.println(TEXT);
        } finally {
            out.close();
        }
        TFile.umount();
    }*/
}
