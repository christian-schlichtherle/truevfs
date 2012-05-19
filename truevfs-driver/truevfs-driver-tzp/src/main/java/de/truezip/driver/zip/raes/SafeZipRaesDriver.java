/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.driver.zip.raes;

import java.io.IOException;
import javax.annotation.concurrent.Immutable;

/**
 * A safe archive driver for RAES encrypted ZIP files (ZIP.RAES).
 * For input archive files up to 512 KB, the cipher text gets authenticated
 * using the RAES provided Message Authentication Code (MAC) <em>before</em>
 * the archive can be accessed by a client application.
 * For larger input archive files, the MAC is not used, but instead the
 * CRC-32 value of the decrypted and deflated archive entries is checked
 * when the archive entry stream is <em>closed</em> by the client application,
 * resulting in some {@link IOException}.
 * <p>
 * This operation mode is considered to be safe:
 * Although a formal prove is missing, it should be computationally
 * infeasible to modify an archive file so that <em>after</em> decryption
 * of the archive and <em>after</em> inflation (decompression) of an
 * entry's data its CRC-32 value still matches!
 * This should hold true even though CRC-32 is not at all a good cryptographic
 * hash function because of its frequent collisions, its linear sink and
 * small sink size.
 * It's the ZIP inflation algorithm which actually comes to our rescue!
 * <p>
 * Subclasses must be thread-safe and should be immutable!
 * 
 * @see     ParanoidZipRaesDriver
 * @author  Christian Schlichtherle
 */
@Immutable
public class SafeZipRaesDriver extends ZipRaesDriver {

    private static final long AUTHENTICATION_TRIGGER = 512 * 1024;

    @Override
    public final long getAuthenticationTrigger() {
        return AUTHENTICATION_TRIGGER;
    }
}