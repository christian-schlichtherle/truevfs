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
package de.schlichtherle.truezip.nio.file.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.fs.archive.zip.raes.TestZipRaesDriver;
import de.schlichtherle.truezip.key.MockView;
import static de.schlichtherle.truezip.key.MockView.Action.*;
import de.schlichtherle.truezip.nio.file.TFileSystemProvider;
import de.schlichtherle.truezip.nio.file.TPath;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import static java.nio.file.Files.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesPathTest extends TPathTestSuite<TestZipRaesDriver> {

    private @Nullable MockView<AesCipherParameters> view;

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver() {
        return new TestZipRaesDriver(IO_POOL_PROVIDER, view);
    }

    @Override
    public void setUp() throws Exception {
        this.view = new MockView<AesCipherParameters>();
        super.setUp();
        final AesCipherParameters key = new AesCipherParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
        view.setAction(ENTER);
    }

    @Override
    public void tearDown() throws Exception {
        view.setAction(ENTER);
        super.tearDown();
    }

    @Test
    public void testCancelling() throws IOException {
        view.setAction(CANCEL);

        final TPath archive = getArchive();
        assertFalse(exists(newNonArchivePath(archive)));

        final TPath entry1 = archive.resolve("entry1");
        try {
            createDirectory(entry1);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        try {
            createFile(entry1);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }

        final TPath entry2 = archive.resolve("entry2");
        try {
            createDirectory(entry2);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
        try {
            createFile(entry2);
            fail("An IOException should have been thrown because password prompting has been disabled!");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testFileStatus() throws IOException {
        final TPath archive = getArchive();
        final TPath inner = archive.resolve("inner" + getSuffix());

        createDirectory(archive);
        createDirectory(inner);

        TFileSystemProvider.umount();
        view.setAction(CANCEL);
        assertTrue(exists(archive));
        assertFalse(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        TFileSystemProvider.umount();
        view.setAction(ENTER);
        assertTrue(exists(archive));
        assertTrue(isDirectory(archive));
        assertFalse(isRegularFile(archive));

        view.setAction(CANCEL);
        assertTrue(exists(inner));
        assertFalse(isDirectory(inner));
        assertFalse(isRegularFile(inner));

        TFileSystemProvider.umount();
        try {
            archive.toFile().rm_r();
            fail();
        } catch (IOException expected) {
        }
            
        TFileSystemProvider.umount();
        view.setAction(ENTER);
        archive.toFile().rm_r();
    }

    /**
     * Skipped because appending to a RAES encrypted ZIP file is not possible
     * by design.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public void testGrowing() {
    }
}
