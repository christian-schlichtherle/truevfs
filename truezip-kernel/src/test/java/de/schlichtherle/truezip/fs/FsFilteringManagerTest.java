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
package de.schlichtherle.truezip.fs;

import de.schlichtherle.truezip.fs.spi.DummyDriverService;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static de.schlichtherle.truezip.util.Link.Type.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsFilteringManagerTest extends FsManagerTestSuite {

    @Override
    protected FsManager newManager() {
        return new FsFilteringManager(
                new FsDefaultManager(),
                FsMountPoint.create(URI.create("file:/")));
    }

    @Test
    public void testFiltering() {
        final FsCompositeDriver driver = new FsDefaultDriver(
                new DummyDriverService("file|tar|tar.gz|zip"));
        for (final String[][] params : new String[][][] {
            // { { /* filter */ }, { /* test set */ }, { /* result set */ } },
            { { "file:/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" } },
            { { "tar:file:/bar.tar!/" }, { "tar:file:/bar.tar!/", "tar.gz:file:/bar.tar.gz!/" }, { "tar:file:/bar.tar!/" } },
            { { "tar:zip:file:/foo.zip!/bar.tar!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/foo.zip/bar.tar/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "tar:file:/foo!/" }, { "zip:file:/foo!/", "tar:file:/bar!/" }, { "zip:file:/foo!/" } },
            { { "zip:file:/foo.zip!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/" } },
            { { "file:/foo.zip/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { } },
            { { "file:/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" }, { "zip:file:/foo.zip!/", "tar:file:/bar.tar!/" } },
        }) {
            assert params[0].length == 1;

            final FsManager manager = new FsDefaultManager(STRONG);
            for (String param : params[1])
                manager.getController(  FsMountPoint.create(URI.create(param)),
                                        driver);
            assertThat(manager.getSize(), is(params[1].length));

            final Set<FsMountPoint> set = new HashSet<FsMountPoint>();
            for (String param : params[2])
                set.add(FsMountPoint.create(URI.create(param)));

            final FsManager filter = new FsFilteringManager(
                    manager, FsMountPoint.create(URI.create(params[0][0])));
            assertThat(filter.getSize(), is(params[2].length));
            for (FsController<?> controller : filter)
                assertTrue(set.contains(controller.getModel().getMountPoint()));
        }
    }
}
