/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.access;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import javax.annotation.concurrent.Immutable;

/**
 * A replacement for the class {@link FileWriter} for writing plain old files
 * or entries in an archive file.
 * Mind that applications cannot write archive files directly - just their
 * entries!
 *
 * @see    TConfig#setLenient
 * @see    TFileReader
 * @author Christian Schlichtherle
 */
@Immutable
@CleanupObligation
public final class TFileWriter extends OutputStreamWriter {

    /**
     * Constructs a new {@code TFile} writer.
     * This writer uses the default character set for encoding characters
     * to bytes.
     * 
     * @param  file a file to write.
     * @throws FileNotFoundException on any I/O error.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING" )
    public TFileWriter(File file) throws FileNotFoundException {
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
     * @throws FileNotFoundException on any I/O error.
     */
    @CreatesObligation
    @edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING")
    public TFileWriter(File file, boolean append) throws FileNotFoundException {
        super(new TFileOutputStream(file, append));
    }

    /**
     * Constructs a new {@code TFile} writer.
     * 
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  charset a character set for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O error.
     */
    @CreatesObligation
    public TFileWriter(File file, boolean append, Charset charset)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), charset);
    }

    /**
     * Constructs a new {@code TFile} writer.
     * 
     * @param  file a file to write.
     * @param  append iff {@code true}, then this writer appends the data to the
     *         given file.
     * @param  encoder an encoder for encoding characters to bytes.
     * @throws FileNotFoundException on any I/O error.
     */
    @CreatesObligation
    public TFileWriter(File file, boolean append, CharsetEncoder encoder)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), encoder);
    }

    /*@Override
    @DischargesObligation
    public void close() throws IOException {
        super.close();
    }*/
}