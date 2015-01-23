/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import javax.annotation.concurrent.Immutable;

/**
 * A replacement for the class {@link FileReader} for reading plain old files
 * or entries in an archive file.
 * Mind that applications cannot read archive files directly - just their
 * entries!
 *
 * @see    TFileWriter
 * @author Christian Schlichtherle
 */
@Immutable
@CleanupObligation
public final class TFileReader extends InputStreamReader {

    /**
     * Constructs a new {@code TFile} reader.
     * This reader uses the default character set for decoding bytes to
     * characters.
     * 
     * @param  file a file to read.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public TFileReader(File file) throws IOException {
	super(new TFileInputStream(file));
    }

    /**
     * Constructs a new {@code TFile} reader.
     * 
     * @param  file a file to read.
     * @param  charset a character set for decoding bytes to characters.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileReader(File file, Charset charset) throws IOException {
	super(new TFileInputStream(file), charset);
    }

    /**
     * Constructs a new {@code TFile} reader.
     * 
     * @param  file a file to read.
     * @param  decoder a decoder for decoding bytes to characters.
     * @throws IOException on any I/O error.
     */
    @CreatesObligation
    public TFileReader(File file, CharsetDecoder decoder) throws IOException {
	super(new TFileInputStream(file), decoder);
    }
}
