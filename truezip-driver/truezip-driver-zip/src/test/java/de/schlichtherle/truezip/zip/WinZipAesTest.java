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
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.rof.ReadOnlyFile;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipException;
import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import static org.junit.Assert.*;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class WinZipAesTest extends ZipTestSuite {

    @Override
    public ZipEntry newEntry(String name) {
        ZipEntry entry = new ZipEntry(name);
        entry.setEncrypted(true);
        return entry;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(OutputStream out)
    throws IOException {
        ZipOutputStream res = new ZipOutputStream(out);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            OutputStream out, Charset charset)
    throws IOException {
        ZipOutputStream res = new ZipOutputStream(out, charset);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipOutputStream newZipOutputStream(
            OutputStream out,
            ZipFile appendee)
    throws ZipException {
        ZipOutputStream res = new ZipOutputStream(out, appendee);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipFile newZipFile(String name)
    throws IOException {
        ZipFile res = new ZipFile(name);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipFile newZipFile(
            String name, Charset charset)
    throws IOException {
        ZipFile res = new ZipFile(name, charset);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipFile newZipFile(File file)
    throws IOException {
        ZipFile res = new ZipFile(file);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipFile newZipFile(
            File file, Charset charset)
    throws IOException {
        ZipFile res = new ZipFile(file, charset);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipFile newZipFile(ReadOnlyFile file)
    throws IOException {
        ZipFile res = new ZipFile(file);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    @Override
    protected ZipFile newZipFile(
            ReadOnlyFile file, Charset charset)
    throws IOException {
        ZipFile res = new ZipFile(file, charset);
        res.setCryptoParameters(new WinZipAesCryptoParameters());
        return res;
    }

    /**
     * Skipped because this test is specified to a plain ZIP file.
     * 
     * @deprecated 
     */
    @Deprecated
    @Override
    public void testBadGetCheckedInputStream() {
    }

    private static final class WinZipAesCryptoParameters
    implements WinZipAesParameters {
        @Override
        public byte[] getWritePassword(String name) throws ZipKeyException {
            return "secret".getBytes();
        }

        @Override
        public byte[] getReadPassword(String name, boolean invalid) throws ZipKeyException {
            return "secret".getBytes();
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
