/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/**
 * A drop-in replacement for the package {@link java.util.zip} for reading and
 * writing ZIP files.
 * <p>
 * The classes in this package read and write ZIP files according to a subset
 * of PKWARE's
 * <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">
 * ZIP File Format Specification</a>,
 * Version 6.3.2 from September 28, 2007.
 * The subset has been selected in order to achieve the following objectives:
 * <ul>
 * <li>Full interoperability with ZIP files read or written by the package
 *     {@link java.util.zip} of Oracle's Java SE&nbsp;7 implementation.
 * <li>Support for
 *     {@link de.schlichtherle.truezip.zip.ZipOutputStream#ZipOutputStream(java.io.OutputStream,de.schlichtherle.truezip.zip.ZipFile) appending}
 *     to existing ZIP files.
 * <li>Support for
 *     {@link de.schlichtherle.truezip.zip.ZipFile#recoverLostEntries() recovering}
 *     lost entries when reading a ZIP file.
 * <li>Support for
 *     {@link de.schlichtherle.truezip.zip.ZipEntry#isEncrypted() reading}
 *     and
 *     {@link de.schlichtherle.truezip.zip.ZipEntry#setEncrypted(boolean) writing}
 *     encrypted or authenticated ZIP entries.
 *     Currently, only the
 *     <a href="http://www.winzip.com/win/en/aes_info.htm">WinZip AES Specification</a>
 *     is supported.
 *     You need to implement the interface
 *     {@link de.schlichtherle.truezip.zip.WinZipAesParameters}
 *     and inject it for reading and writing to the
 *     {@link de.schlichtherle.truezip.zip.ZipFile#setCryptoParameters(de.schlichtherle.truezip.zip.ZipCryptoParameters) ZipFile.setCryptoParameters(ZipCryptoParameters)}
 *     and
 *     {@link de.schlichtherle.truezip.zip.ZipOutputStream#setCryptoParameters(de.schlichtherle.truezip.zip.ZipCryptoParameters) ZipOutputStream.setCryptoParameters(ZipCryptoParameters)}
 *     classes.
 * <li>Support for
 *     {@link de.schlichtherle.truezip.zip.ZipEntry#getMethod() reading}
 *     and
 *     {@link de.schlichtherle.truezip.zip.ZipEntry#setMethod(int) writing}
 *     BZIP2 compressed ZIP entries.
 * <li>Support for selectable character sets, in particular IBM Code Page 437
 *     (alias IBM PC) for PKZIP compatibility and UTF-8 for Java Archive (JAR)
 *     compatibility.
 *     Note that using any other character set except CP437 or UTF-8 is
 *     strongly discouraged because it will result in interoperability issues
 *     with third party tools, especially when sharing archive files between
 *     different locales!
 * <li>Support for
 *     {@link de.schlichtherle.truezip.zip.ZipEntry#getExternalAttributes() reading}
 *     and
 *     {@link de.schlichtherle.truezip.zip.ZipEntry#setExternalAttributes(long) writing}
 *     external file attributes.
 * <li>Support for reading and writing ZIP64 extensions with the following
 *     restrictions:
 *     <ol>
 *     <li>The maximum file size is {@link java.lang.Long#MAX_VALUE}.
 *     <li>The maximum number of entries in the Central Directory is
 *         {@link java.lang.Integer#MAX_VALUE}.
 *     <li>The offsets in ZIP64 files must respect a preamble if present, i.e.
 *         they must be exact.
 *         This is in contrast to ZIP32 files where the offsets do not need to
 *         respect a preamble if present.
 *         Preambles are primarily used to contain self extracting (SFX)
 *         executable code.
 *     </ol>
 * <li>{@link de.schlichtherle.truezip.zip.ZipFile} supports reading archive
 *     data from the random access read only interface
 *     {@link de.schlichtherle.truezip.rof.ReadOnlyFile}, which allows to read
 *     archive data from composite data sources like e.g. RAES encrypted ZIP
 *     files directly without the need to decrypt them to a temporary file
 *     first.
 * </ul>
 */
@edu.umd.cs.findbugs.annotations.DefaultAnnotation(edu.umd.cs.findbugs.annotations.NonNull.class)
package de.schlichtherle.truezip.zip;
