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

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import static de.schlichtherle.truezip.crypto.param.AesKeyStrength.*;
import static de.schlichtherle.truezip.zip.LittleEndian.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.NotThreadSafe;

/**
 * WinZip AES Extra Field.
 *
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class WinZipAesExtraField extends ExtraField {

    private static final int DATA_SIZE = 7;
    private static final int VENDOR_ID = 'A' | ('E' << 8);

    private short vendorVersion = 1;
    private AesKeyStrength encryptionStrength = BITS_256;
    private short compressionMethod;
    
    /**
     * Constructs a new WinZip AES Extra Field.
     */
    WinZipAesExtraField() {
    }

    @Override
    int getHeaderId() {
        return WINZIP_AES_ID;
    }

    @Override
    int getDataSize() {
        return DATA_SIZE;
    }

    int getVendorVersion() {
        return vendorVersion & UShort.MAX_VALUE;
    }

    void setVendorVersion(final int vendorVersion) {
        assert UShort.check(vendorVersion);
        this.vendorVersion = (short) vendorVersion;
    }

    int getVendorId() {
        return VENDOR_ID;
    }

    AesKeyStrength getEncryptionStrength() {
        return encryptionStrength;
    }

    void setKeyStrength(final AesKeyStrength encryptionStrength) {
        assert null != encryptionStrength;
        this.encryptionStrength = encryptionStrength;
    }

    int getCompressionMethod() {
        return compressionMethod & UShort.MAX_VALUE;
    }

    void setMethod(final int compressionMethod) {
        assert UShort.check(compressionMethod);
        this.compressionMethod = (short) compressionMethod;
    }

    @Override
    void readFrom(final byte[] src, int off, final int size) {
        if (DATA_SIZE != size)
            throw new IllegalArgumentException();
        vendorVersion = (short) readUShort(src, off);
        off += 2;
        final int vendorId = (short) readUShort(src, off);
        off += 2;
        if (VENDOR_ID != vendorId)
            throw new IllegalArgumentException();
        encryptionStrength = AesKeyStrength.values()[src[off] & UByte.MAX_VALUE];
        off += 1;
        compressionMethod = (short) readUShort(src, off);
        // off += 2;
    }

    @Override
    void writeTo(byte[] dst, int off) {
        writeShort(vendorVersion, dst, off);
        off += 2;
        writeShort(VENDOR_ID, dst, off);
        off += 2;
        dst[off] = (byte) encryptionStrength.ordinal();
        off += 1;
        writeShort(compressionMethod, dst, off);
        // off += 2;
    }
}