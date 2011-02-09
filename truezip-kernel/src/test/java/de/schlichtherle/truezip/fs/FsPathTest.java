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

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsPathTest {

    private static final Logger logger
            = Logger.getLogger(FsPathTest.class.getName());

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };

        for (final String[] params : new String[][] {
            { "zip:zip:file:/föö%20bär!/föö%20bär!/föö%20bär", },
            { "zip:file:/föö%20bär!/föö%20bär", },
            { "file:/föö%20bär/föö%20bär", },
            { "zip:file:/foo!/bar", },
            { "file:/foo/bar", },
            { "file:/foo/bar", },
            { "föö%20bär", },
            { "föö/bär", },
            { "föö", },
            { "föö?bär", },
            { "", },
        }) {
            final FsPath original = FsPath.create(params[0]);
            assertThat(original.toString(), equalTo(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(original);
                oos.close();

                logger.log(Level.FINE, "Number of serialized bytes: {0}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final ObjectInputStream ois = new ObjectInputStream(bis);
                final Object clone = ois.readObject();
                ois.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
                assertThat(clone.toString(), equalTo(params[0]));
            }

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final XMLEncoder enc = new XMLEncoder(bos);
                enc.setExceptionListener(listener);
                enc.writeObject(original);
                enc.close();

                logger.log(Level.FINE, bos.toString("UTF-8"));

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final XMLDecoder dec = new XMLDecoder(bis);
                final Object clone = dec.readObject();
                dec.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
                assertThat(clone.toString(), equalTo(params[0]));
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithInvalidUri() throws URISyntaxException {
        try {
            FsPath.create((String) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((String) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsPath.create((URI) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((URI) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsPath.create((String) null, FsUriModifier.NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((String) null, FsUriModifier.NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsPath.create((URI) null, FsUriModifier.NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((URI) null, FsUriModifier.NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsPath.create((String) null, FsUriModifier.CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((String) null, FsUriModifier.CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsPath.create((URI) null, FsUriModifier.CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((URI) null, FsUriModifier.CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((FsMountPoint) null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String param : new String[] {
            "/../foo#boo",
            "/../foo#",
            "/../foo",
            "/./foo",
            "//foo",
            "/foo",
            "/foo/bar",
            "/foo/bar/",
            "/",
            "foo/",
            "foo//",
            "foo/.",
            "foo/./",
            "foo/..",
            "foo/../",
            "foo:bar",
            "foo:bar:",
            "foo:bar:/",
            "foo:bar:/baz",
            "foo:bar:/baz!",
            "foo:bar:/baz/",
            "foo:bar:/baz!//",
            "foo:bar:/baz!/.",
            "foo:bar:/baz!/./",
            "foo:bar:/baz!/..",
            "foo:bar:/baz!/../",
            "foo:bar:/baz!/bang/.",
            "foo:bar:/baz!/bang/./",
            "foo:bar:/baz!/bang/..",
            "foo:bar:/baz!/bang/../",
            "foo:bar:baz:/bang",
            "foo:bar:baz:/bang!",
            "foo:bar:baz:/bang/",
            "foo:bar:baz:/bang!/",
            "foo:bar:baz:/bang!/boom",
            "foo:bar:/baz/.!/",
            "foo:bar:/baz/./!/",
            "foo:bar:/baz/..!/",
            "foo:bar:/baz/../!/",

            "foo:bar:/baz/../!/bang/",
            "foo:bar:/baz/..!/bang/",
            "foo:bar:/baz/./!/bang/",
            "foo:bar:/baz/.!/bang/",
            "foo:bar:/../baz/!/bang/",
            "foo:bar:/./baz/!/bang/",
            "foo:bar://baz/!/bang/", // baz is authority!
            "foo:bar://baz!/bang/", // baz is authority!

            "foo:bar:/!/bang/",

            "foo:bar:/baz/../!/bang",
            "foo:bar:/baz/..!/bang",
            "foo:bar:/baz/./!/bang",
            "foo:bar:/baz/.!/bang",
            "foo:bar:/../baz/!/bang",
            "foo:bar:/./baz/!/bang",
            "foo:bar://baz/!/bang", // baz is authority!
            "foo:bar://baz!/bang", // baz is authority!

            "foo:bar:/!/bang",

            "foo:bar:/baz/!/",
            "foo:bar:/baz/?bang!/?plonk",
            "foo:bar:/baz//!/",
            "foo:bar:/baz/./!/",
            "foo:bar:/baz/..!/",
            "foo:bar:/baz/../!/",

            "//authority/defined",
        }) {
            final URI uri = URI.create(param);

            try {
                FsPath.create(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }

            try {
                new FsPath(uri);
                fail(param);
            } catch (URISyntaxException expected) {
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithValidUri() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/bä%20ng!/bö%20öm!/plö%20nk", "foo:bar:baz:/bä%20ng!/bö%20öm!/", "plö%20nk" },
            { "foo:bar:baz:/bäng!/bööm!/plönk", "foo:bar:baz:/bäng!/bööm!/", "plönk" },
            { "foo:bar:baz:/bang!/boom!/plonk", "foo:bar:baz:/bang!/boom!/", "plonk" },
            { "foo:bar:baz:/bang!/boom!/", "foo:bar:baz:/bang!/boom!/", "" },

            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            //{ "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            //{ "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },

            //{ "foo:bar:/baz!/bang/", "foo:bar:/baz!/", "bang/" },

            { "foo:bar:/baz!/bang", "foo:bar:/baz!/", "bang" },

            { "foo:bar:/baz!/./", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/.", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/#", "foo:bar:/baz!/", "#", },
            { "foo:bar:/baz!/#bang", "foo:bar:/baz!/", "#bang", },
            { "foo:bar:/baz?bang!/?plonk", "foo:bar:/baz?bang!/", "?plonk" },
            { "foo:bar:/baz?bang!/?plonk#frag", "foo:bar:/baz?bang!/", "?plonk#frag" },

            //{ "foo:bar:/baz!/bang//", "foo:bar:/baz!/", "bang/" },
            //{ "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },
            //{ "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },

            { "", null, "", },
            { "#foo", null, "#foo", },
            { "foo", null, "foo" },
            { "foo#bar", null, "foo#bar", },
            //{ "foo/", null, "foo/" },
            //{ "foo//", null, "foo/" },
            //{ "foo/.", null, "foo/" },
            //{ "foo/./", null, "foo/" },
            { "foo/..", null, "" },
            { "foo/../", null, "" },
            { "foo/bar", null, "foo/bar" },
            //{ "foo/bar/", null, "foo/bar/" },
            { "foo:/", "foo:/", "" },
            { "foo:/#", "foo:/", "#" },
            { "foo:/#bar", "foo:/", "#bar" },
            { "foo:/bar", "foo:/", "bar" },
            { "foo:/bar#", "foo:/", "bar#" },
            { "foo:/bar#frag", "foo:/", "bar#frag" },
            { "foo:/bar/", "foo:/", "bar" },
            { "foo:/bar//", "foo:/", "bar" },
            { "foo:/bar/.", "foo:/", "bar" },
            { "foo:/bar/./", "foo:/", "bar" },
            { "foo:/bar/..", "foo:/", "" },
            { "foo:/bar/../", "foo:/", "" },
            { "foo:/bar/baz", "foo:/bar/", "baz" },
            { "foo:/bar/baz?bang", "foo:/bar/", "baz?bang" },
            { "foo:/bar/baz/", "foo:/bar/", "baz" },
            { "foo:/bar/baz/?bang", "foo:/bar/", "baz?bang" },
        }) {
            FsPath path = FsPath.create(params[0], FsUriModifier.CANONICALIZE);
            final FsMountPoint mountPoint = null == params[1] ? null : FsMountPoint.create(params[1]);
            final FsEntryName entryName = FsEntryName.create(params[2]);
            assertPath(path, mountPoint, entryName);

            path = new FsPath(mountPoint, entryName);
            assertPath(path, mountPoint, entryName);
        }
    }

    private void assertPath(final FsPath path,
                            final FsMountPoint mountPoint,
                            final FsEntryName entryName) {
        if (null != mountPoint) {
            assertThat(path.getUri(), equalTo(URI.create(
                    mountPoint.toString() + entryName)));
            assertThat(path.getMountPoint(), equalTo(mountPoint));
        } else {
            assertThat(path.getUri(), equalTo(entryName.getUri()));
            assertThat(path.getMountPoint(), nullValue());
        }
        assertThat(path.getEntryName().getUri(), equalTo(entryName.getUri()));
        assertThat(path.toString(), equalTo(path.getUri().toString()));
        assertThat(FsPath.create(path.getUri().toString()), equalTo(path));
        assertThat(FsPath.create(path.getUri().toString()).hashCode(), equalTo(path.hashCode()));
    }

    @Test
    public void testSpaces() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/%20!/%20/%20!/%20/%20", " ", " / ", " / ", },
            { "foo:bar:baz:/%20a%20!/%20b%20!/%20c%20", " a ", " b ", " c ", },
        }) {
            FsPath path = FsPath.create(params[0]);
            for (int i = params.length; 0 < --i; ) {
                assertThat(path.getEntryName().getPath(), equalTo(params[i]));
                path = path.getMountPoint().getPath();
            }
        }
    }

    @Test
    public void testHierarchicalize() {
        for (final String[] params : new String[][] {
            { "foo:bar:/baz!/", "bar:/baz/" },
            { "foo:bar:baz:/x/bö%20m?plö%20k!/bä%20g?zö%20k!/", "baz:/x/bö%20m/bä%20g/?zö%20k" },
            { "bar:baz:/x/bö%20m?plö%20k!/bä%20g?zö%20k", "baz:/x/bö%20m/bä%20g?zö%20k" },
            { "foo:bar:baz:/x/bööm?plönk!/bäng?zönk!/", "baz:/x/bööm/bäng/?zönk" },
            { "bar:baz:/x/bööm?plönk!/bäng?zönk", "baz:/x/bööm/bäng?zönk" },
            { "foo:bar:baz:/boom?plonk!/bang?zonk!/", "baz:/boom/bang/?zonk" },
            { "bar:baz:/boom?plonk!/bang?zonk", "baz:/boom/bang?zonk" },
            { "bar:baz:/boom?plonk!/?zonk", "baz:/boom/?zonk" },
            { "bar:baz:/boom?plonk!/bang", "baz:/boom/bang" },
            { "bar:baz:/boom?plonk!/", "baz:/boom/?plonk" },
            { "foo:bar:baz:/boom!/bang!/", "baz:/boom/bang/" },
            { "bar:baz:/boom!/bang", "baz:/boom/bang" },
            { "foo:bar:/baz?boom!/", "bar:/baz/?boom" },
            { "bar:/baz?boom", "bar:/baz?boom" },
            { "foo:bar:/baz!/", "bar:/baz/" },
            { "bar:/baz", "bar:/baz" },
            { "foo:/bar/?boom", "foo:/bar/?boom" },
            { "bar?boom", "bar?boom" },
            { "foo:/bar/", "foo:/bar/" },
            { "bar", "bar" },
        }) {
            final FsPath path = FsPath.create(params[0]);
            final FsPath hierarchical = path.hierarchicalize();
            assertThat(hierarchical.getUri(), equalTo(URI.create(params[1])));
            assertThat(hierarchical.hierarchicalize(), sameInstance(hierarchical));
        }
    }
}
