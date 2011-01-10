/*
 * Copyright (C) 2006-2010 Schlichtherle IT Services
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

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 * A drop-in replacement for {@link java.io.FileReader} which provides
 * transparent read access to archive entries as if they were (virtual) files.
 * All file system operations in this class are
 * <a href="package-summary.html#atomicity">virtually atomic</a>.
 *
 * @see     <a href="package-summary.html#streams">Using Archive Entry Streams</a>
 * @see     File#cat
 * @see     File#umount
 * @see     File#update
 * @author  Christian Schlichtherle
 * @version $Id$
 */
public final class FileReader extends InputStreamReader {

    public FileReader(String path) throws FileNotFoundException {
	super(new FileInputStream(path));
    }

    public FileReader(File file) throws FileNotFoundException {
	super(new FileInputStream(file));
    }

    public FileReader(FileDescriptor fd) {
	super(new FileInputStream(fd));
    }
}
