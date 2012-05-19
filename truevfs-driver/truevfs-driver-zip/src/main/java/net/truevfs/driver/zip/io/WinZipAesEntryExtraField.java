/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.driver.zip.io;

import net.truevfs.key.param.AesKeyStrength;
import static net.truevfs.key.param.AesKeyStrength.BITS_128;
import static net.truevfs.driver.zip.io.LittleEndian.readUShort;
import static net.truevfs.driver.zip.io.LittleEndian.writeShort;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * WinZip AES Extra Field.
 *
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
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