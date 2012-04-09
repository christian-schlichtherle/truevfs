/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes.zip;

import de.truezip.driver.zip.io.ZipFile;
import de.truezip.driver.zip.io.ZipOutputStream;
import de.truezip.driver.zip.io.ZipTestSuite;
import de.truezip.driver.zip.raes.crypto.MockType0RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesParameters;
import de.truezip.driver.zip.raes.crypto.RaesReadOnlyChannel;
import de.truezip.driver.zip.raes.crypto.RaesSink;
import de.truezip.kernel.io.AbstractSink;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        final RaesReadOnlyChannel channel
                = RaesReadOnlyChannel.getInstance(Paths.get(name), raesParameters);
        try {
            if (channel.size() < AUTHENTICATION_TRIGGER) // heuristic
                channel.authenticate();
            return new ZipFile(channel);
        } catch (final RuntimeException | IOException ex) {
            channel.close();
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
        final RaesReadOnlyChannel channel
                = RaesReadOnlyChannel.getInstance(Paths.get(name), raesParameters);
        try {
            if (channel.size() < AUTHENTICATION_TRIGGER) // heuristic
                channel.authenticate();
            return new ZipFile(channel, cs);
        } catch (final RuntimeException | IOException ex) {
            channel.close();
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final Path file)
    throws IOException {
        final RaesReadOnlyChannel channel
                = RaesReadOnlyChannel.getInstance(file, raesParameters);
        try {
            if (channel.size() < AUTHENTICATION_TRIGGER) // heuristic
                channel.authenticate();
            return new ZipFile(channel);
        } catch (final RuntimeException | IOException ex) {
            channel.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(
            final Path file, final Charset charset)
    throws IOException {
        if (null == charset)
            throw new NullPointerException();
        new String(new byte[0], charset); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyChannel channel
                = RaesReadOnlyChannel.getInstance(file, raesParameters);
        try {
            if (channel.size() < AUTHENTICATION_TRIGGER) // heuristic
                channel.authenticate();
            return new ZipFile(channel, charset);
        } catch (final RuntimeException | IOException ex) {
            channel.close();
            throw ex;
        }
    }

    @Override
    protected ZipFile newZipFile(final SeekableByteChannel channel)
    throws IOException {
        final RaesReadOnlyChannel rrof
                = RaesReadOnlyChannel.getInstance(channel, raesParameters);
        try {
            if (rrof.size() < AUTHENTICATION_TRIGGER) // heuristic
                rrof.authenticate();
            return new ZipFile(rrof);
        } catch (final RuntimeException | IOException ex) {
            rrof.close();
            throw ex;
        }
    }

    @Override
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    protected ZipFile newZipFile(final SeekableByteChannel channel, final Charset cs)
    throws IOException {
        if (null == cs)
            throw new NullPointerException();
        new String(new byte[0], cs); // may throw UnsupportedEncodingExceoption!
        final RaesReadOnlyChannel rrof
                = RaesReadOnlyChannel.getInstance(channel, raesParameters);
        try {
            if (rrof.size() < AUTHENTICATION_TRIGGER) // heuristic
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
