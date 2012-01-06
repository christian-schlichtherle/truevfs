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

    public TFileWriter(TFile file) throws FileNotFoundException {
        super(new TFileOutputStream(file));
    }

    public TFileWriter(TFile file, boolean append) throws FileNotFoundException {
        super(new TFileOutputStream(file, append));
    }

    public TFileWriter(TFile file, boolean append, CharsetEncoder encoder)
    throws FileNotFoundException {
        super(new TFileOutputStream(file, append), encoder);
    }
}
