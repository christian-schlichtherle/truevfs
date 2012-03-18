/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.zip;

import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An output stream which updates a CRC-32 checksum.
 * <p>
 * Implementations cannot be thread-safe.
 *
 * @since  TrueZIP 7.3
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class Crc32OutputStream extends CheckedOutputStream {
    Crc32OutputStream(@CheckForNull OutputStream out) {
        super(out, new CRC32());
    }
}
