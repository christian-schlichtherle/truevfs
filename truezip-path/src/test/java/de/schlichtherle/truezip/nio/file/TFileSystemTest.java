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

import java.io.IOException;
import de.schlichtherle.truezip.fs.FsMountPoint;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URI;
import java.nio.file.FileSystems;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
public class TFileSystemTest extends MockTestBase {

    @Test
    public void testNewFileSystem() throws IOException {
        for (String[] params : new String[][] {
            // $uri, $mountPoint
            { "tpath:/", "file:/" },
            { "tpath:///", "file:/" },
            { "tpath:/foo", "file:/" },
            { "tpath:/foo/", "file:/" },
            { "tpath:/foo/bar", "file:/foo/" },
            { "tpath:/foo/bar/", "file:/foo/" },
            { "tpath:/foo/bar.mok/", "mok:file:/foo/bar.mok!/" },
            { "tpath:/foo.mok/bar", "mok:file:/foo.mok!/" },
            { "tpath:/foo.mok/bar.mok", "mok:mok:file:/foo.mok!/bar.mok!/" },
        }) {
            final URI uri = URI.create(params[0]);
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[1]));
            final TFileSystem fs = (TFileSystem) FileSystems.newFileSystem(uri, getEnvironment());
            fs.close();
            assertThat(fs.isOpen(), is(true));
            assertThat(fs.getMountPoint(), is(mountPoint));
        }
    }
}
