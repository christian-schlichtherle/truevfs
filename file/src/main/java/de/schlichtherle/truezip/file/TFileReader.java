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
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import net.jcip.annotations.Immutable;

/**
 * A replacement for {@link FileReader} which provides transparent read access
 * to archive entries as if they were (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 *
 * @see     <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see     TFileWriter
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
public final class TFileReader extends InputStreamReader {

    public TFileReader(String path) throws FileNotFoundException {
	super(new TFileInputStream(path));
    }

    public TFileReader(TFile file) throws FileNotFoundException {
	super(new TFileInputStream(file));
    }

    public TFileReader(FileDescriptor fd) {
	super(new TFileInputStream(fd));
    }
}
