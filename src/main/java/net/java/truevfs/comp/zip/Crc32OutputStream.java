/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip;

import javax.annotation.CheckForNull;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

/**
 * An output stream which updates a CRC-32 checksum.
 *
 * @author Christian Schlichtherle
 */
final class Crc32OutputStream extends CheckedOutputStream {
    Crc32OutputStream(@CheckForNull OutputStream out) {
        super(out, new CRC32());
    }
}
