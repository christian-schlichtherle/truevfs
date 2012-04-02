/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.zip;

import de.truezip.driver.zip.io.ZipFile;
import de.truezip.driver.zip.io.ZipOutputStream;
import de.truezip.driver.zip.io.ZipTestSuite;
import de.truezip.driver.zip.raes.crypto.*;
import de.truezip.kernel.io.AbstractSink;
import de.truezip.kernel.rof.ReadOnlyFile;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Tests compression and encryption of data.
 * 
 * @author Christian Schlichtherle
 */
public final class RaesZipIT extends ZipTestSuite {

    /** Cipher text shorter than this gets authenticated. */
    private static int AUTHENTICATION_TRIGGER = 512 * 1024;
    
    // Must not be static to enable parallel testing!
    private final RaesParameters raesParameters = new MockType0RaesParameters();

    @Override
    protected ZipOutputStream newZipOutputStream(final OutputStream out)
    throws IOException {
        final OutputStream ros = new RaesSink(new InvalidSink(out),
                raesParameters).newStream();
        try {
            return new ZipOutputStream(ros);
        } catch (RuntimeException ex) {
            ros.close();
            throw ex;
        }
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            final OutputStream out, final Charset cs)
    throws IOException {
        final OutputStream ros = new RaesSink(new InvalidSink(out),
                raesParameters).newStream();
        try {
            return new ZipOutputStream(ros, cs);
        } catch (RuntimeException ex) {
            ros.close();
            throw ex;
        }
    }

    private static final class InvalidSink extends AbstractSink {
        private final OutputStream out;

        InvalidSink(final OutputStream out) {
            this.out = out;
        }

        @Override
        public OutputStream newStream() throws IOException {
            return out; // TODO: Upon the second call, this is an invalid stream!
        }
    } // InvalidSink

    @Override
    protected ZipFile newZipFile(final String name)
    throws IOException {
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(new File(name), raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof);
        } catch (final RuntimeException | IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(final String name, final Charset cs)
    throws IOException {
        if (null == cs)
            throw new NullPointerException();
        new String(new byte[0], cs); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(new File(name), raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof, cs);
        } catch (final RuntimeException | IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final File file)
    throws IOException {
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(file, raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof);
        } catch (final RuntimeException | IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(
            final File file, final Charset charset)
    throws IOException {
        if (null == charset)
            throw new NullPointerException();
        new String(new byte[0], charset); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyFile rof
                = RaesReadOnlyFile.getInstance(file, raesParameters);
        try {
            if (rof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rof.authenticate();
            return new ZipFile(rof, charset);
        } catch (final RuntimeException | IOException ex) {
            rof.close();
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final ReadOnlyFile rof)
    throws IOException {
        final RaesReadOnlyFile rrof
                = RaesReadOnlyFile.getInstance(rof, raesParameters);
        try {
            if (rrof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rrof.authenticate();
            return new ZipFile(rrof);
        } catch (final RuntimeException | IOException ex) {
            rrof.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(final ReadOnlyFile rof, final Charset cs)
    throws IOException {
        if (null == cs)
            throw new NullPointerException();
        new String(new byte[0], cs); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyFile rrof
                = RaesReadOnlyFile.getInstance(rof, raesParameters);
        try {
            if (rrof.length() < AUTHENTICATION_TRIGGER) // heuristic
                rrof.authenticate();
            return new ZipFile(rrof, cs);
        } catch (final RuntimeException | IOException ex) {
            rrof.close();
            throw ex;
        }
    }

    /**
     * Skipped because the test is specific to plain ZIP files.
     */
    @Override
    public void testBadGetCheckedInputStream() {
    }

    /**
     * Skipped because appending to RAES encrypted ZIP files is not possible
     * by design.
     */
    @Override
    public void testAppending() {
    }
}
