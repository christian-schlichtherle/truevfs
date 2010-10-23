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

package de.schlichtherle.truezip.io.file;

import java.io.FileNotFoundException;
import java.net.URI;

/**
 * Implements the {@link FileFactory} part of the {@link ArchiveDetector}
 * interface.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 * @since TrueZIP 6.0
 */
public abstract class AbstractArchiveDetector implements ArchiveDetector {

    @Override
    public File newFile(java.io.File template) {
        return new File(template, this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public File newFile(java.io.File delegate, File innerArchive) {
        return new File(delegate, innerArchive, this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public File newFile(
            File template,
            java.io.File delegate,
            File enclArchive) {
        return new File(template, delegate, enclArchive);
    }

    @Override
    public File newFile(java.io.File parent, String child) {
        return new File(parent, child, this);
    }

    @Override
    public File newFile(String pathName) {
        return new File(pathName, this);
    }

    @Override
    public File newFile(String parent, String child) {
        return new File(parent, child, this);
    }

    @Override
    public File newFile(URI uri) {
        return new File(uri, this);
    }

    @Override
    public FileInputStream newFileInputStream(java.io.File file)
    throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public FileOutputStream newFileOutputStream(
            java.io.File file,
            boolean append)
    throws FileNotFoundException {
        return new FileOutputStream(file, append);
    }
}
