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
package de.schlichtherle.truezip.nio.file;

import de.schlichtherle.truezip.fs.FsMountPoint;
import de.schlichtherle.truezip.fs.archive.FsArchiveDriver;
import java.io.File;
import static de.schlichtherle.truezip.nio.file.TFileSystemProvider.Parameter.*;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public abstract class TestBase<D extends FsArchiveDriver<?>>
extends de.schlichtherle.truezip.file.TestBase<D> {

    protected static final FsMountPoint
            ROOT_DIRECTORY = FsMountPoint.create(URI.create("file:/"));
    protected static final FsMountPoint
            CURRENT_DIRECTORY = FsMountPoint.create(new File("").toURI());
    protected static final String[] NO_MORE = new String[0];

    private @Nullable Map<String, ?> environment;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(ARCHIVE_DETECTOR, super.getArchiveDetector());
        this.environment = map;
    }

    protected final @Nullable Map<String, ?> getEnvironment() {
        return environment;
    }
}
