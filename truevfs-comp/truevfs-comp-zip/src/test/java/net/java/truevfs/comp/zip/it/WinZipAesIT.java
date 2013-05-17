/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip.it;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import net.java.truecommons.key.spec.common.AesKeyStrength;
import net.java.truevfs.comp.zip.WinZipAesParameters;
import net.java.truevfs.comp.zip.ZipEntry;
import net.java.truevfs.comp.zip.ZipFile;
import net.java.truevfs.comp.zip.ZipKeyException;
import net.java.truevfs.comp.zip.ZipOutputStream;
import static org.junit.Assert.assertSame;

/**
 * @author Christian Schlichtherle
 */
public final class WinZipAesIT extends ZipITSuite {

    @Override
    public ZipEntry newEntry(String name) {
        final ZipEntry entry = new ZipEntry(name);
        entry.setEncrypted(true);
        return entry;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(OutputStream out)
    throws IOException {
        final ZipOutputStream zos = new ZipOutputStream(out);
        zos.setCryptoParameters(new WinZipAesCryptoParameters());
        return zos;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            OutputStream out, Charset charset)
    throws IOException {
        final ZipOutputStream zos = new ZipOutputStream(out, charset);
        zos.setCryptoParameters(new WinZipAesCryptoParameters());
        return zos;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            OutputStream out,
            ZipFile appendee)
    throws IOException {
        final ZipOutputStream zos = new ZipOutputStream(out, appendee);
        zos.setCryptoParameters(new WinZipAesCryptoParameters());
        return zos;
    }

    @Override
    protected ZipFile newZipFile(Path file)
    throws IOException {
        final ZipFile zf = new ZipFile(file);
        zf.setCryptoParameters(new WinZipAesCryptoParameters());
        return zf.recoverLostEntries();
    }

    @Override
    protected ZipFile newZipFile(
            Path file, Charset charset)
    throws IOException {
        final ZipFile zf = new ZipFile(file, charset);
        zf.setCryptoParameters(new WinZipAesCryptoParameters());
        return zf.recoverLostEntries();
    }

    @Override
    protected ZipFile newZipFile(SeekableByteChannel file)
    throws IOException {
        final ZipFile zf = new ZipFile(file);
        zf.setCryptoParameters(new WinZipAesCryptoParameters());
        return zf.recoverLostEntries();
    }

    @Override
    protected ZipFile newZipFile(
            SeekableByteChannel file, Charset charset)
    throws IOException {
        final ZipFile zf = new ZipFile(file, charset);
        zf.setCryptoParameters(new WinZipAesCryptoParameters());
        return zf.recoverLostEntries();
    }

    /**
     * Skipped because this test is specified to a plain ZIP file.
     */
    @Override
    public void testBadGetCheckedInputStream() { }

    private static final class WinZipAesCryptoParameters
    implements WinZipAesParameters {
        @Override
        public byte[] getWritePassword(String name) throws ZipKeyException {
            return "top secret".getBytes();
        }

        @Override
        public byte[] getReadPassword(final String name, final boolean invalid)
        throws ZipKeyException {
            if (invalid) throw new ZipKeyException("Unknown password.");
            return "top secret".getBytes();
        }

        @Override
        public AesKeyStrength getKeyStrength(String name)
        throws ZipKeyException {
            return AesKeyStrength.BITS_128;
        }

        @Override
        public void setKeyStrength(String name, AesKeyStrength keyStrength)
        throws ZipKeyException {
            assertSame(AesKeyStrength.BITS_128, keyStrength);
        }
    } // WinZipAesCryptoParameters
}
