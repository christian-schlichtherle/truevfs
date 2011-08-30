/*
 * Copyright (C) 2011 Schlichtherle IT Services
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

import java.io.File;
import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;

/**
 * Compares two files by their status and path name so that directories
 * are always ordered <em>before</em> other files.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class TFileComparator implements Comparator<File>, Serializable {

    private static final long serialVersionUID = 1234567890123456789L;

    /**
     * A collator for file names which considers case according to the
     * platform's standard.
     */
    private static final Collator COLLATOR = Collator.getInstance();
    static {
        // Set minimum requirements for maximum performance.
        COLLATOR.setDecomposition(Collator.NO_DECOMPOSITION);
        COLLATOR.setStrength(File.separatorChar == '\\'
                ? Collator.SECONDARY
                : Collator.TERTIARY);
    }

    @Override
    public int compare(File f1, File f2) {
        return f1.isDirectory()
            ? f2.isDirectory()
                    ? COLLATOR.compare(f1.getName(), f2.getName())
                    : -1
            : f2.isDirectory()
                    ? 1
                    : COLLATOR.compare(f1.getName(), f2.getName());
    }
}
