/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access.it;

import net.java.truecommons.shed.ConcurrencyUtils.*;
import net.java.truevfs.access.ConfiguredClientTestBase;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileOutputStream;
import net.java.truevfs.access.TVFS;
import net.java.truevfs.kernel.spec.FsArchiveDriver;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import static net.java.truecommons.shed.ConcurrencyUtils.*;
import static net.java.truevfs.kernel.spec.FsSyncOptions.SYNC;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests a particular {@link FsArchiveDriver} using the API of the module
 * TrueVFS Access.
 *
 * @param <D> The type of the archive driver.
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

    @Test
    public void testConcurrentSync() throws ExecutionException, InterruptedException {
        Throwable t1 = null;
        final TaskJoiner sync = startSync();
        try {
            startRoundTrip().join();
        } catch (final Throwable t2) {
            t1 = t2;
            throw t2;
        } finally {
            try {
                sync.cancel();
                sync.join();
            } catch (final Throwable t2) {
                if (null == t1) {
                    throw t2;
                }
                t1.addSuppressed(t2);
            }
        }
    }

    private TaskJoiner startRoundTrip() {
        return start(NUM_IO_THREADS, threadNum -> () -> {
            for (int i = 0; i < NUM_REPEATS; i++) {
                roundTrip(threadNum);
            }
            return null;
        });
    }

    private static TaskJoiner startSync() {
        return start(NUM_CPU_THREADS, threadNum -> () -> {
            while (!Thread.interrupted()) { // test and clear status!
                TVFS.sync(SYNC);
            }
            return null;
        });
    }

    private void roundTrip(final int threadNum) throws IOException {
        final TFile archive = newTempArchive();
        final TFile file = new TFile(archive, threadNum + getExtension() + "/" + threadNum);
        roundTrip(file);
        archive.rm_r();
    }

    private TFile newTempArchive() throws IOException {
        // TODO: Removing .getCanonicalFile() causes archive.rm_r() to fail in testCopyContainingOrSameFiles()
        //  - explain why!
        final File temp = File.createTempFile(TEMP_FILE_PREFIX, getExtension()).getCanonicalFile();
        TFile.rm(temp);
        return new TFile(temp);
    }

    private void roundTrip(final TFile outer) throws IOException {
        final TFile inner = new TFile(outer.getParentFile(), "inner" + getExtension() + "/" + outer.getName());
        // This particular sequence has been selected because of its increased likeliness to fail in case the sync logic
        // of the selective entry cache is not correct.
        create(inner);
        check(inner);
        inner.mv(outer);
        check(outer);
        outer.mv(inner);
        check(inner);
        inner.rm();
    }

    private void create(final TFile file) throws IOException {
        try (OutputStream out = new TFileOutputStream(file)) {
            out.write(getData());
        }
        assertEquals(getDataLength(), file.length());
    }

    private void check(final TFile file) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(getDataLength());
        try (OutputStream out = baos) {
            file.output(out);
        }
        assertArrayEquals(getData(), baos.toByteArray());
    }
}
