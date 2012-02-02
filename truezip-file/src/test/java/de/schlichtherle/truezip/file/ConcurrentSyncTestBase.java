/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import de.schlichtherle.truezip.fs.FsSyncException;
import static de.schlichtherle.truezip.fs.FsSyncOptions.SYNC;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import de.schlichtherle.truezip.socket.spi.ByteArrayIOPoolService;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.NUM_IO_THREADS;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskFactory;
import de.schlichtherle.truezip.util.ConcurrencyUtils.TaskJoiner;
import static de.schlichtherle.truezip.util.ConcurrencyUtils.runConcurrent;
import java.io.*;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Performs integration testing of a particular {@link FsArchiveDriver}
 * by using the API of the TrueZIP File* module.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public abstract class ConcurrentSyncTestBase<D extends FsArchiveDriver<?>>
extends TestBase<D> {

    private static final Logger
            logger = Logger.getLogger(ConcurrentSyncTestBase.class.getName());

    /**
     * The prefix for temporary files, which is {@value}.
     * This value should identify the TrueZIP File* module in order to
     * ensure that no two temporary files are shared between tests of the
     * TrueZIP Path API and the TrueZIP File* API.
     */
    private static final String TEMP_FILE_PREFIX = "tzp-sync";

    /** The data to get compressed. */
    private static final byte[] DATA = new byte[1024]; // enough to waste some heat on CPU cycles
    static {
        new Random().nextBytes(DATA);
    }

    protected static final IOPoolProvider
            IO_POOL_PROVIDER = new ByteArrayIOPoolService(4 * DATA.length / 3); // account for archive file type specific overhead

    private static final int NUM_REPEATS = 100;

    private File temp;
    private TFile archive;
    private byte[] data;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        temp = createTempFile();
        TFile.rm(temp);
        archive = new TFile(temp);
        data = DATA.clone();
    }

    private File createTempFile() throws IOException {
        // TODO: Removing .getCanonicalFile() causes archive.rm_r() to
        // fail in testCopyContainingOrSameFiles() - explain why!
        return File.createTempFile(TEMP_FILE_PREFIX, getSuffix()).getCanonicalFile();
    }

    @Override
    public void tearDown() {
        try {
            try {
                umount();
            } finally {
                archive = null;
                if (temp.exists() && !temp.delete())
                    throw new IOException(temp + " (could not delete)");
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.toString(), ex);
        } finally {
            super.tearDown();
        }
    }

    private void umount() throws FsSyncException {
        if (null != archive)
            TFile.umount(archive);
    }

    @Test
    public void testConcurrentSync() throws IOException, InterruptedException, ExecutionException {
        class SyncTask implements Callable<Void> {
            @Override
            public Void call() throws IOException {
                for (int i = 0; i++ < NUM_REPEATS; )
                    TFile.sync(SYNC);
                return null;
            }
        } // SyncTask

        class SyncTaskFactory implements TaskFactory {
            @Override
            public Callable<Void> newTask(int threadNum) {
                return new SyncTask();
            }
        } // SyncTaskFactory

        final TaskJoiner joiner = runConcurrent(
                NUM_IO_THREADS,
                new SyncTaskFactory());
        try {
            for (int i = 0; i++ < NUM_REPEATS; )
                assertInputOutput();
        } finally {
            joiner.join();
        }
    }

    private void assertInputOutput() throws IOException {
        //assertInputOutput(archive);
        
        //final TFile archiveTest = new TFile(archive, "test");
        //assertInputOutput(archiveTest);
        
        final TFile archiveInner = new TFile(archive, "inner" + getSuffix());
        final TFile archiveInnerTest = new TFile(archiveInner, "test");
        assertInputOutput(archiveInnerTest);
        archiveInner.rm();
        archive.rm();
    }

    private void assertInputOutput(final TFile file) throws IOException {
        assertInput(file);
        assertOutput(file);
        file.rm();
    }

    private void assertInput(final TFile file) throws IOException {
        final InputStream in = new ByteArrayInputStream(data);
        try {
            file.input(in);
        } finally {
            in.close();
        }
        assertEquals(data.length, file.length());
    }

    private void assertOutput(final TFile file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
        try {
            file.output(out);
        } finally {
            out.close();
        }
        assertTrue(Arrays.equals(data, out.toByteArray()));
    }
}
