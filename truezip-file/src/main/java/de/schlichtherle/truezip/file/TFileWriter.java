/*
 * Copyright (C) 2006-2011 Schlichtherle IT Services
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Note that applications cannot write archive <em>files</em> directly using
 * this class - just their entries.
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
