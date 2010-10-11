/*
 * Copyright (C) 2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.socket;

import de.schlichtherle.truezip.io.Files;
import java.io.File;
import java.io.IOException;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class TempFileCreator implements FileCreator {

    // Declared package private for unit testing purposes.
    static final String DEFAULT_PREFIX = "tzp-tfc";
    static final String DEFAULT_SUFFIX = null;

    private final String prefix;
    private final String suffix;
    private final File dir;

    public TempFileCreator() {
        this(null, null, null);
    }

    public TempFileCreator( final String prefix,
                            final String suffix,
                            final File dir) {
        this.prefix = null == prefix ? DEFAULT_PREFIX : prefix;
        this.suffix = null == suffix ? DEFAULT_SUFFIX : suffix;
        this.dir = dir;
    }

    @Override
    public File createFile() throws IOException {
        return Files.createTempFile(prefix, suffix, dir);
    }
}
