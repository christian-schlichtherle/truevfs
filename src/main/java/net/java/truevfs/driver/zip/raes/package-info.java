/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * Provides a file system driver for accessing the RAES encrypted ZIP file
 * format, alias ZIP.RAES or TZP.
 * ZIP.RAES files are regular ZIP files which use UTF-8 as their character set
 * (like JARs) and have been wrapped in a RAES file format envelope.
 * The RAES File Format enables transparent, random access to AES encrypted
 * data (the payload) in the file as if the application were reading decrypted
 * data from a {@link java.io.RandomAccessFile}.
 * Note that RAES is not specific to ZIP files - any kind of content may get
 * encrypted.
 */
@javax.annotation.Nonnull @javax.annotation.ParametersAreNonnullByDefault
package net.java.truevfs.driver.zip.raes;
