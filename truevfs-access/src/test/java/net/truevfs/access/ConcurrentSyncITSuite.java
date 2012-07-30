/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access;

import static net.java.truecommons.shed.ConcurrencyUtils.*;
import net.java.truecommons.shed.ConcurrencyUtils.TaskFactory;
import net.java.truecommons.shed.ConcurrencyUtils.TaskJoiner;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import net.truevfs.kernel.spec.FsArchiveDriver;
import static net.truevfs.kernel.spec.FsSyncOptions.SYNC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Performs integration testing of a particular {@link FsArchiveDriver}
 * by using the API of the TrueVFS Access File* module.
 *
 * @param  <D> The type of the archive driver.
 * @author Christian Schlichtherle
 */
public abstract class ConcurrentSyncITSuite<D extends FsArchiveDriver<?>>
extends ConfiguredClientTestBase<D> {

    /**
     * The prefix for temporary files, which is {@value}.
     * This value should identify the TrueVFS Access File* module in order to
     * ensure that no two temporary files are shared between tests of the
     * TrueVFS Access Path API and the TrueVFS Access File* API.
     */
    private static final String TEMP_FILE_PREFIX = "tzp-sync";

    private static final int NUM_REPEATS = 10;

    private File createTempArchive() throws IOException {
        // TODO: Removing .getCanonicalFile() causes archive.rm_r() to
        // fail in testCopyContainingOrSameFiles() - explain why!
        return File.createTempFile(TEMP_FILE_PREFIX, getExtension()).getCanonicalFile();
    }

    @Test
    public void testConcurrentSync() throws Exception {
        class RoundTripFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(final int threadNum) {
                class RoundTrip implements Callable<Void> {
                    @Override
                    public Void call() throws Exception {
                        for (int i = 0; i < NUM_REPEATS; i++)
                            roundTrip(threadNum);
                        return null;
                    }
                } // RoundTrip
                return new RoundTrip();
            }
        } // RoundTripFactory

        class SyncFactory implements TaskFactory {
            @Override
            public Callable<?> newTask(int threadNum) {
                class Sync implements Callable<Void> {
                    @Override
                    public Void call() throws IOException {
                        while (!Thread.interrupted()) // test and clear status!
                            TVFS.sync(SYNC);
                        return null;
                    }
                } // Sync
                return new Sync();
            }
        } // SyncFactory

        // Trigger sync mayhem!
        Throwable ex = null;
        final TaskJoiner sync = start(NUM_CPU_THREADS, new SyncFactory());
        try {
            start(NUM_IO_THREADS, new RoundTripFactory()).join();
        } catch (final InterruptedException | ExecutionException ex2) {
            ex = ex2;
            throw ex2;
        } finally {
            try {
                sync.cancel();
                sync.join();
            } catch (final InterruptedException | ExecutionException ex2) {
                if (null == ex) throw ex2;
                ex.addSuppressed(ex2);
            }
        }
    }

    void roundTrip(final int i) throws IOException {
        final File temp = createTempArchive();
        TFile.rm(temp);
        final TFile archive = new TFile(temp);
        final TFile file = new TFile(archive, i + getExtension() + "/" + i);
        roundTrip(file);
        archive.rm_r();
    }

    private void roundTrip(final TFile outer) throws IOException {
        final TFile inner = new TFile(outer.getParentFile(),
                "inner" + getExtension() + "/" + outer.getName());
        // This particular sequence has been selected because of its increased
        // likeliness to fail in case the sync logic of the selective entry
        // cache is not correct.
        create(inner);
        check(inner);
        inner.mv(outer);
        check(outer);
        outer.mv(inner);
        check(inner);
        inner.rm();
    }

    private void create(final TFile file) throws IOException {
        try (final OutputStream out = new TFileOutputStream(file)) {
            out.write(getData());
        }
        assertEquals(getDataLength(), file.length());
    }

    private void check(final TFile file) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(getDataLength());
        try {
            file.output(out);
        } finally {
            out.close();
        }
        assertTrue(Arrays.equals(getData(), out.toByteArray()));
    }
}
