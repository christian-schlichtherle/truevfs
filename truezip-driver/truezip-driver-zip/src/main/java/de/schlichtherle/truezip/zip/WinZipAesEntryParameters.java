/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.zip;

import de.schlichtherle.truezip.crypto.param.AesKeyStrength;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * The parameters of this interface are used with WinZip AES encrypted entries.
 *
 * @since   TrueZIP 7.3
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@DefaultAnnotation(NonNull.class)
final class WinZipAesEntryParameters {

    private final WinZipAesParameters param;
    private final ZipEntry entry;

    public WinZipAesEntryParameters(
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
