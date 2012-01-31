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
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import net.jcip.annotations.Immutable;

/**
 * A replacement for the class {@link FileReader} for reading plain old files
 * or entries in an archive file.
 * Mind that applications cannot read archive files directly - just their
 * entries!
 *
 * @see     TFileWriter
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
public final class TFileReader extends InputStreamReader {

    /**
     * Constructs a new {@code TFile} reader.
     * This reader uses the default character set for decoding bytes to
     * characters.
     * 
     * @param  file a file to read.
     * @throws FileNotFoundException on any I/O failure.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING")
    public TFileReader(TFile file) throws FileNotFoundException {
	super(new TFileInputStream(file));
    }

    /**
     * Constructs a new {@code TFile} reader.
     * 
     * @param  file a file to read.
     * @param  charset a character set for decoding bytes to characters.
     * @throws FileNotFoundException on any I/O failure.
     * @since  TrueZIP 7.5
     */
    public TFileReader(TFile file, Charset charset)
    throws FileNotFoundException {
	super(new TFileInputStream(file), charset);
    }

    /**
     * Constructs a new {@code TFile} reader.
     * 
     * @param  file a file to read.
     * @param  decoder a decoder for decoding bytes to characters.
     * @throws FileNotFoundException on any I/O failure.
     */
    public TFileReader(TFile file, CharsetDecoder decoder)
    throws FileNotFoundException {
	super(new TFileInputStream(file), decoder);
    }
}
