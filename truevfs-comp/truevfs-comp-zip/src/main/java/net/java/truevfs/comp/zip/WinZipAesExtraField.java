/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import javax.annotation.concurrent.NotThreadSafe;
import net.java.truecommons.io.ImmutableBuffer;
import net.java.truevfs.key.spec.param.AesKeyStrength;
import static net.java.truevfs.key.spec.param.AesKeyStrength.BITS_128;

/**
 * WinZip AES Extra Field.
 *
 * @see     <a href="http://www.winzip.com/win/en/aes_info.htm">AES Encryption Information: Encryption Specification AE-1 and AE-2 (WinZip Computing, S.L.)</a>
 * @see     <a href="http://www.winzip.com/win/en/aes_tips.htm">AES Coding Tips for Developers (WinZip Computing, S.L.)</a>
 * @see     RawZipOutputStream$WinZipAesOutputMethod
 * @author  Christian Schlichtherle
 */
@NotThreadSafe
final class WinZipAesExtraField extends BufferedExtraField {

    /** The Header Id for a WinZip AES extra field. */
    public static final int HEADER_ID = 0x9901;

    private static final int DATA_SIZE = 7;
    private static final int VENDOR_ID = 'A' | ('E' << 8);
    private static final AesKeyStrength[] KEY_STRENGTHS = AesKeyStrength.values();

    /**
     * Entries of this type <em>do</em> include the standard ZIP CRC-32
     * value.
     * For use with {@link #setVendorVersion(int)}/{@link #getVendorVersion()}.
     */
    public static final int VV_AE_1 = 1;

    /**
     * Entries of this type do <em>not</em> include the standard ZIP CRC-32
     * value.
     * For use with {@link #setVendorVersion(int)}/{@link #getVendorVersion()}.
     */
    public static final int VV_AE_2 = 2;

    WinZipAesExtraField(final ImmutableBuffer ib) {
        super(ib);
        requireHeaderId(HEADER_ID);
        requireDataSize(DATA_SIZE);
        validateVendorVersion(getVendorVersion());
        validateVendorId(getVendorId());
        validateEncryptionStrength(getEncryptionStrength());
        validateMethod(getMethod());
    }

    WinZipAesExtraField() {
        super(HEADER_ID, DATA_SIZE);
        setVendorVersion(VV_AE_1);
        setVendorId(VENDOR_ID);
        setKeyStrength(BITS_128);
    }

    /**
     * Returns the vendor version.
     *
     * @see #VV_AE_1
     * @see #VV_AE_2
     */
    public int getVendorVersion() { return mb.getUShort(4); }

    /**
     * Sets the vendor version.
     *
     * @see    #VV_AE_1
     * @see    #VV_AE_2
     * @param  vendorVersion the vendor version.
     * @throws IllegalArgumentException
     */
    public void setVendorVersion(int vendorVersion) {
        mb.putShort(4, (short) validateVendorVersion(vendorVersion));
    }

    private static int validateVendorVersion(final int vendorVersion) {
        validate(VV_AE_1 <= vendorVersion && vendorVersion <= VV_AE_2,
                "%d (invalid Vendor Version)", vendorVersion);
        return vendorVersion;
    }

    public int getVendorId() { return mb.getUShort(6); }

    private void setVendorId(int vendorId) {
        mb.putShort(6, (short) validateVendorId(vendorId));
    }

    private static int validateVendorId(final int vendorId) {
        validate(VENDOR_ID == vendorId, "%d (invalid Vendor Id)", vendorId);
        return vendorId;
    }

    private int getEncryptionStrength() { return mb.getUByte(8); }

    private static int validateEncryptionStrength(final int encryptionStrength) {
        keyStrength(encryptionStrength);
        return encryptionStrength;
    }

    public AesKeyStrength getKeyStrength() {
        return keyStrength(getEncryptionStrength());
    }

    public void setKeyStrength(AesKeyStrength keyStrength) {
        mb.put(8, (byte) encryptionStrength(keyStrength));
    }

    private static int encryptionStrength(AesKeyStrength keyStrength) {
        return keyStrength.ordinal() + 1;
    }

    private static AesKeyStrength keyStrength(final int encryptionStrength) {
        try {
            return KEY_STRENGTHS[encryptionStrength - 1];
        } catch (final IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(encryptionStrength + " (invalid Key Strength)", ex);
        }
    }

    public int getMethod() { return mb.getUShort(9); }

    public void setMethod(int method) {
        mb.putShort(9, (short) validateMethod(method));
    }

    private static int validateMethod(final int method) {
        UShort.check(method);
        return method;
    }

    @SuppressWarnings("PackageVisibleInnerClass")
    static final class Factory extends AbstractExtraFieldFactory {
        @Override
        protected ExtraField newExtraFieldUnchecked(ImmutableBuffer ib) {
            return new WinZipAesExtraField(ib);
        }
    } // Factory
}
