/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
/**
 * The archive driver family for RAES encrypted ZIP files, alias TZP files.
 * TZP files are regular ZIP files which use UTF-8 as their character set (like
 * JARs) and have been wrapped in a RAES file format envelope.
 * The RAES File Format enables transparent, random access to AES encrypted
 * data (the payload) in the file as if the application were reading decrypted
 * data from a {@link java.io.RandomAccessFile}.
 * Note that RAES is not specific to ZIP files - any kind of content may get
 * encrypted.
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.truezip.driver.zip.raes;