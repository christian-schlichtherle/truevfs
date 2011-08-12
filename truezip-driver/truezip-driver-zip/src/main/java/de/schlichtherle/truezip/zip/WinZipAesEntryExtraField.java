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
 * @since   TrueZIP 7.3
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@DefaultAnnotation(NonNull.class)
final class WinZipAesEntryExtraField extends ExtraField {

    private static final int DATA_SIZE = 7;
    private static final int VENDOR_ID = 'A' | ('E' << 8);
    private static final AesKeyStrength[] KEY_STRENGTHS = AesKeyStrength.values();

    /**
     * Entries of this type <em>do</em> include the standard ZIP CRC-32
     * value.
     * For use with {@link #setVendorVersion(int)}/{@link #getVendorVersion()}.
     */
    static final int VV_AE_1 = 1;

    /**
     * Entries of this type do <em>not</em> include the standard ZIP CRC-32
     * value.
     * For use with {@link #setVendorVersion(int)}/{@link #getVendorVersion()}.
     */
    static final int VV_AE_2 = 2;

    private short vendorVersion = VV_AE_1;
    private byte encryptionStrength = encryptionStrength(BITS_128);
    private short method;

    /**
     * Constructs a new WinZip AES Extra Field.
     */
    WinZipAesEntryExtraField() {
    }

    private static byte encryptionStrength(AesKeyStrength keyStrength) {
        return (byte) (keyStrength.ordinal() + 1);
    }

    private static AesKeyStrength keyStrength(int encryptionStrength) {
        return KEY_STRENGTHS[(encryptionStrength - 1) & UByte.MAX_VALUE];
    }

    @Override
    int getHeaderId() {
        return WINZIP_AES_ID;
    }

    @Override
    int getDataSize() {
        return DATA_SIZE;
    }

    /**
     * Returns the vendor version.
     * 
     * @see #VV_AE_1
     * @see #VV_AE_2
     */
    int getVendorVersion() {
        return vendorVersion & UShort.MAX_VALUE;
    }

    /**
     * Sets the vendor version.
     * 
     * @see    #VV_AE_1
     * @see    #VV_AE_2
     * @param  vendorVersion the vendor version.
     * @throws IllegalArgumentException
     */
    void setVendorVersion(final int vendorVersion) {
        if (vendorVersion < VV_AE_1 || VV_AE_2 < vendorVersion)
            throw new IllegalArgumentException("" + vendorVersion);
        this.vendorVersion = (short) vendorVersion;
    }

    int getVendorId() {
        return VENDOR_ID;
    }

    AesKeyStrength getKeyStrength() {
        return keyStrength(this.encryptionStrength);
    }

    void setKeyStrength(final AesKeyStrength keyStrength) {
        this.encryptionStrength = encryptionStrength(keyStrength);
    }

    int getMethod() {
        return method & UShort.MAX_VALUE;
    }

    void setMethod(final int compressionMethod) {
        assert UShort.check(compressionMethod);
        this.method = (short) compressionMethod;
    }

    @Override
    void readFrom(final byte[] src, int off, final int size) {
        if (DATA_SIZE != size)
            throw new IllegalArgumentException();
        setVendorVersion(readUShort(src, off));
        off += 2;
        final int vendorId = (short) readUShort(src, off);
        off += 2;
        if (VENDOR_ID != vendorId)
            throw new IllegalArgumentException();
        setKeyStrength(keyStrength(src[off])); // checked
        off += 1;
        setMethod(readUShort(src, off));
        // off += 2;
    }

    @Override
    void writeTo(byte[] dst, int off) {
        writeShort(this.vendorVersion, dst, off);
        off += 2;
        writeShort(VENDOR_ID, dst, off);
        off += 2;
        dst[off] = this.encryptionStrength;
        off += 1;
        writeShort(this.method, dst, off);
        // off += 2;
    }
}