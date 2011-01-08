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
package de.schlichtherle.truezip.io.fs;

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
public class FsFilterManagerTest extends FsManagerTestCase {

    @Override
    protected FsManager newManager() {
        return new FsFilterManager(
                new FsFederationManager(),
                FsMountPoint.create("file:/"));
    }

    @Test
    public void testFiltering() {
        for (final String[][] params : new String[][][] {
            { { "tar:zip:file:/foo!/bar!/" }, { "zip:file:/foo!/" }, { } },
            { { "file:/foo/bar/" }, { "zip:file:/foo!/" }, { } },
            { { "tar:file:/foo!/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
            { { "zip:file:/foo!/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
            { { "file:/foo/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
            { { "file:/" }, { "zip:file:/foo!/" }, { "zip:file:/foo!/" } },
        }) {
            assert params[0].length == 1;

            final FsManager manager = new FsFederationManager(
                    STRONG);
            for (final String param : params[1])
                manager.getController(FsMountPoint.create(param), new FsDummyDriver());
            assertThat(manager.getSize(), is(params[1].length));

            final Set<FsMountPoint> set = new HashSet<FsMountPoint>();
            for (final String param : params[2])
                set.add(FsMountPoint.create(param));

            final FsManager filter = new FsFilterManager(
                    manager, FsMountPoint.create(params[0][0]));
            assertThat(filter.getSize(), is(params[2].length));
            for (final FsController<?> controller : filter)
                assertTrue(set.contains(controller.getModel().getMountPoint()));
        }
    }
}
