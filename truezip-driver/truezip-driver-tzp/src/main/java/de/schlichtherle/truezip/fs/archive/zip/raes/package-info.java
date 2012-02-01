/*
 * Copyright (C) 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
@DefaultAnnotation(NonNull.class)
package de.schlichtherle.truezip.fs.archive.zip.raes;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
