/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.nio.file.zip.raes;

import de.schlichtherle.truezip.crypto.raes.param.AesCipherParameters;
import de.schlichtherle.truezip.fs.archive.zip.raes.TestZipRaesDriver;
import de.schlichtherle.truezip.key.MockView;
import static de.schlichtherle.truezip.key.MockView.Action.CANCEL;
import static de.schlichtherle.truezip.key.MockView.Action.ENTER;
import de.schlichtherle.truezip.nio.file.TFileSystemProvider;
import de.schlichtherle.truezip.nio.file.TPath;
import de.schlichtherle.truezip.nio.file.TPathTestSuite;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import java.io.IOException;
import static java.nio.file.Files.*;
import javax.annotation.Nullable;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class ZipRaesPathIT extends TPathTestSuite<TestZipRaesDriver> {

    private @Nullable MockView<AesCipherParameters> view;

    @Override
    protected String getSuffixList() {
        return "tzp";
    }

    @Override
    protected TestZipRaesDriver newArchiveDriver(IOPoolProvider provider) {
        return new TestZipRaesDriver(provider, view);
    }

    @Override
    public void setUp() throws IOException {
        this.view = new MockView<AesCipherParameters>();
        super.setUp();
        final AesCipherParameters key = new AesCipherParameters();
        key.setPassword("secret".toCharArray());
        view.setKey(key);
        view.setAction(ENTER);
    }

    @Override
    public void tearDown() {
        try {
            view.setAction(ENTER);
        } finally {
            super.tearDown();
        }
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
