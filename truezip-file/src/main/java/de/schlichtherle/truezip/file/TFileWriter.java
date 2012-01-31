/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.file;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import net.jcip.annotations.Immutable;

/**
 * A replacement for the class {@link FileWriter} for writing plain old files
 * or entries in an archive file.
 * Mind that applications cannot write archive files directly - just their
 * entries!
 *
 * @see     TConfig#setLenient
 * @see     TFileReader
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
public final class TFileWriter extends OutputStreamWriter {

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     * 
     * @param  file a file to write.
     * @throws FileNotFoundException on any I/O failure.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING")
    public TFileWriter(TFile file) throws FileNotFoundException {
        super(new TFileOutputStream(file));
    }

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     * 
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @throws FileNotFoundException on any I/O failure.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING")
    public TFileWriter(TFile file, boolean append) throws FileNotFoundException {
        super(new TFileOutputStream(file, append));
    }

    /**
     * Constructs a new {@code TFile} writer.
     * 
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  charset a character set for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O failure.
     * @since  TrueZIP 7.5
     */
    public TFileWriter(TFile file, boolean append, Charset charset)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), charset);
    }

    /**
     * Constructs a new {@code TFile} writer.
     * 
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  encode an encoder for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileWriter(TFile file, boolean append, CharsetEncoder encoder)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), encoder);
    }
}
