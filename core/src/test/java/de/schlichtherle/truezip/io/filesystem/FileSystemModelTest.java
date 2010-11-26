/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io.filesystem;

import java.net.URI;
import org.junit.Test;

import static org.junit.Assert.*;
import static de.schlichtherle.truezip.io.entry.Entry.ROOT;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemModelTest {

    @Test
    public void testMountPointConstructor() {
        FileSystemModel model;

        try {
            model = new FileSystemModel(null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String uri : new String[] {
            "foo:bar",
            "/bar",
        }) {
            final URI mountPoint = URI.create(uri);
            try {
                model = new FileSystemModel(mountPoint);
                fail();
            } catch (RuntimeException expected) {
            }
        }

        final URI expectedMountPoint = URI.create("foo:/bar/");
        for (final String uri : new String[] {
            "foo:/bar",
            "foo:/bar/",
            "foo:/bar//",
        }) {
            final URI mountPoint = URI.create(uri);
            model = new FileSystemModel(mountPoint);
            assert expectedMountPoint.equals(model.getMountPoint());
            assert null == model.getParent();
            try {
                model.parentPath(ROOT);
                fail();
            } catch (RuntimeException expected) {
            }
            assert !model.isTouched();
        }
    }

    @Test
    public void testMountPointAndParentConstructor() {
        FileSystemModel model;

        try {
            model = new FileSystemModel(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String[] uri : new String[][] {
            { "foo:bar", "foo:/" },
            { "/bar", "/" },
            { "foo:/bar", "foo:/baz" },
            { "foo:/bar/", "foo:/baz/" },
            { "foo:bar:a!/b/", "bar:a" },
        }) {
            final URI mountPoint = URI.create(uri[0]);
            final URI parentMountPoint = URI.create(uri[1]);
            try {
                model = new FileSystemModel(mountPoint, new FileSystemModel(parentMountPoint));
                fail();
            } catch (RuntimeException expected) {
            }
        }

        for (final String[] uri : new String[][] {
            { "foo:/bar/", "foo:/bar", "foo:/", "bar", "" },
            { "foo:/bar/", "foo:/bar/", "foo:/", "bar", "" },
            { "foo:/bar/", "foo:/bar//", "foo:/", "bar", "" },
            { "foo:/bar/", "foo:/bar//", "foo:/", "bar/baz", "baz" },
            { "foo:/bar/", "foo:/bar//", "foo:/", "bar/baz/", "baz/" },
        }) {
            final URI expectedMountPoint = URI.create(uri[0]);
            final URI mountPoint = URI.create(uri[1]);
            final URI parentMountPoint = URI.create(uri[2]);
            final String parentPath = uri[3];
            final String path = uri[4];
            model = new FileSystemModel(mountPoint, new FileSystemModel(parentMountPoint));
            assert expectedMountPoint.equals(model.getMountPoint());
            assert parentMountPoint.equals(model.getParent().getMountPoint());
            assert parentPath.equals(model.parentPath(path));
            assert !model.isTouched();
        }
    }
}
