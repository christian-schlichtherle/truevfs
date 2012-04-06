/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.io;

import de.truezip.key.param.AesKeyStrength;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.util.zip.ZipException;
import static org.junit.Assert.assertSame;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAes2IT extends Zip2TestSuite {

    @Override
    public ZipEntry newEntry(String name) {
        final ZipEntry r = new ZipEntry(name);
        r.setEncrypted(true);
        return r;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(OutputStream out)
    throws IOException {
        final ZipOutputStream r = new ZipOutputStream(out);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            OutputStream out, Charset charset)
    throws IOException {
        final ZipOutputStream r = new ZipOutputStream(out, charset);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            OutputStream out,
            ZipReadOnlyChannel appendee)
    throws ZipException {
        final ZipOutputStream r = new ZipOutputStream(out, appendee);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipReadOnlyChannel newZipFile(String name)
    throws IOException {
        final ZipReadOnlyChannel r = new ZipReadOnlyChannel(name);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipReadOnlyChannel newZipFile(
            String name, Charset charset)
    throws IOException {
        final ZipReadOnlyChannel r = new ZipReadOnlyChannel(name, charset);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipReadOnlyChannel newZipFile(File file)
    throws IOException {
        final ZipReadOnlyChannel r = new ZipReadOnlyChannel(file);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipReadOnlyChannel newZipFile(
            File file, Charset charset)
    throws IOException {
        final ZipReadOnlyChannel r = new ZipReadOnlyChannel(file, charset);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipReadOnlyChannel newZipFile(SeekableByteChannel file)
    throws IOException {
        final ZipReadOnlyChannel r = new ZipReadOnlyChannel(file);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    @Override
    protected ZipReadOnlyChannel newZipFile(
            SeekableByteChannel file, Charset charset)
    throws IOException {
        final ZipReadOnlyChannel r = new ZipReadOnlyChannel(file, charset);
        r.setCryptoParameters(new WinZipAesCryptoParameters());
        return r;
    }

    /**
     * Skipped because this test is specified to a plain ZIP file.
     */
    @Override
    public void testBadGetCheckedInputStream() {
    }

    private static final class WinZipAesCryptoParameters
    implements WinZipAesParameters {
        @Override
        public byte[] getWritePassword(String name) throws ZipKeyException {
            return "top secret".getBytes();
        }

        @Override
        public byte[] getReadPassword(String name, boolean invalid) throws ZipKeyException {
            return "top secret".getBytes();
        }

        @Override
        public AesKeyStrength getKeyStrength(String name) throws ZipKeyException {
            return AesKeyStrength.BITS_128;
        }

        @Override
        public void setKeyStrength(String name, AesKeyStrength keyStrength) throws ZipKeyException {
            assertSame(AesKeyStrength.BITS_128, keyStrength);
        }
    } // WinZipAesCryptoParameters
}
