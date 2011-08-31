/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs.archive.zip.raes;

import de.schlichtherle.truezip.key.KeyManagerProvider;
import de.schlichtherle.truezip.socket.IOPoolProvider;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import net.jcip.annotations.Immutable;

/**
 * A safe archive driver which builds RAES encrypted ZIP files (ZIP.RAES).
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
 * hash function because of its frequent collisions, its linear output and
 * small output size.
 * It's the ZIP inflation algorithm which actually comes to our rescue!
 * 
 * @see     ParanoidZipRaesDriver
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
@DefaultAnnotation(NonNull.class)
public class SafeZipRaesDriver extends ZipRaesDriver {

    public SafeZipRaesDriver(   IOPoolProvider ioPoolProvider,
                                KeyManagerProvider keyManagerProvider) {
        super(ioPoolProvider, keyManagerProvider);
    }

    private static final long AUTHENTICATION_TRIGGER = 512 * 1024;

    @Override
    public final long getAuthenticationTrigger() {
        return AUTHENTICATION_TRIGGER;
    }
}
