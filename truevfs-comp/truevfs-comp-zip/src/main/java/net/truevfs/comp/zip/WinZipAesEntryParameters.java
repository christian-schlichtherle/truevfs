/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.comp.zip;

import net.truevfs.keymgr.spec.param.AesKeyStrength;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The parameters of this interface are used with WinZip AES encrypted entries.
 *
 * @author  Christian Schlichtherle
 */
@ThreadSafe
final class WinZipAesEntryParameters {

    private final WinZipAesParameters param;
    private final ZipEntry entry;

    WinZipAesEntryParameters(
            final WinZipAesParameters param,
            final ZipEntry entry) {
        assert null != param;
        assert null != entry;
        this.param = param;
        this.entry = entry;
    }

    ZipEntry getEntry() {
        return entry;
    }

    AesKeyStrength getKeyStrength() throws ZipKeyException {
        return param.getKeyStrength(entry.getName());
    }

    void setKeyStrength(AesKeyStrength keyStrength) throws ZipKeyException {
        param.setKeyStrength(entry.getName(), keyStrength);
    }

    byte[] getWritePassword() throws ZipKeyException {
        return param.getWritePassword(entry.getName());
    }

    byte[] getReadPassword(boolean invalid) throws ZipKeyException {
        return param.getReadPassword(entry.getName(), invalid);
    }
}