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

import static de.schlichtherle.truezip.io.fs.FSEntryName.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FSMountPointTest {

    private static final Logger logger
            = Logger.getLogger(FSMountPoint.class.getName());

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };

        for (final String[] params : new String[][] {
            { "zip:zip:file:/föö%20bär!/föö%20bär!/", },
            { "zip:file:/föö%20bär!/", },
            { "file:/föö%20bär/", },
        }) {
            final FSMountPoint original = FSMountPoint.create(params[0]);
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
            FSMountPoint.create((String) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint((String) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FSMountPoint.create((URI) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint((URI) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FSMountPoint.create((String) null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint((String) null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FSMountPoint.create((URI) null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint((URI) null, false);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FSMountPoint.create((String) null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint((String) null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FSMountPoint.create((URI) null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint((URI) null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FSMountPoint.create(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FSMountPoint(null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String param : new String[] {
            "foo:bar:baz:/!/!/",
            "foo",
            "foo/bar",
            "foo/bar/",
            "/foo",
            "/foo/bar",
            "/foo/bar/",
            "//foo",
            "/../foo",
            "foo:/bar#baz",
            "foo:/bar/#baz",
            "foo:/bar",
            "foo:/bar?baz",
            "foo:/bar//",
            "foo:/bar/.",
            "foo:/bar/./",
            "foo:/bar/..",
            "foo:/bar/../",
            "foo:bar:/baz!//",
            "foo:bar:/baz!/.",
            "foo:bar:/baz!/./",
            "foo:bar:/baz!/..",
            "foo:bar:/baz!/../",
            "foo:bar:/baz!/bang",
            "foo:bar:/baz!/#bang",
            "foo:bar:/baz/!/",
            "foo:bar:baz:/bang!/!/",
        }) {
            final URI uri = URI.create(param);

            try {
                FSMountPoint.create(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }

            try {
                new FSMountPoint(uri);
                fail(param);
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            //{ "foo", "bar:baz:/bang!/boom" },
            { "foo", "bar:baz:/bang!/" },
            { "foo", "bar:/baz/" },
        }) {
            final FSScheme scheme = FSScheme.create(params[0]);
            final FSPath path = FSPath.create(params[1]);
            try {
                new FSMountPoint(scheme, path);
                fail(params[0] + ":" + params[1] + "!/");
            } catch (URISyntaxException expected) {
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithValidUri() {
        for (final String param : new String[] {
            "foo:/bär/bäz/",
            "foo:/bär/",
            "foo:/",
            "foo:/bär/bäz/?bäng",
            "foo:/bär/?bäng",
            "foo:/?bäng",
        }) {
            final URI uri = URI.create(param);
            final FSMountPoint mountPoint = FSMountPoint.create(uri);
            assertThat(mountPoint.getUri(), sameInstance(uri));
            assertThat(mountPoint.getPath(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(FSMountPoint.create(mountPoint.getUri().toString())));
            assertThat(mountPoint.hashCode(), equalTo(FSMountPoint.create(mountPoint.getUri().toString()).hashCode()));
        }

        for (final String[] params : new String[][] {
            { "bar:baz:/ba%20ng!/", "bar", "baz:/ba%20ng" },
            { "foo:bar:baz:/ba%20ng!/bo%20om?plo%20nk!/", "foo", "bar:baz:/ba%20ng!/bo%20om?plo%20nk" },
            { "foo:bar:baz:/./bäng!/./bööm?plönk!/", "foo", "bar:baz:/bäng!/bööm?plönk" },
            { "foo:bar:baz:/./bang!/boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/./boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/boom?plonk!/", "foo", "bar:baz:/bang!/boom?plonk" },
            { "foo:bar:baz:/bang!/boom!/", "foo", "bar:baz:/bang!/boom" },
            { "foo:bar:/baz?bang!/", "foo", "bar:/baz?bang" },
            { "foo:bar:/baz!/", "foo", "bar:/baz" },
        }) {
            final FSMountPoint mountPoint = FSMountPoint.create(params[0], true);
            final FSScheme scheme = FSScheme.create(params[1]);
            final FSPath path = FSPath.create(params[2]);

            assertThat(mountPoint.getScheme(), equalTo(scheme));
            assertThat(mountPoint.getPath(), equalTo(path));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(FSMountPoint.create(mountPoint.toString()), equalTo(mountPoint));
            assertThat(FSMountPoint.create(mountPoint.getUri().getScheme() + ":" + mountPoint.getPath() + "!/"), equalTo(mountPoint));
            assertThat(FSMountPoint.create(mountPoint.getScheme(), mountPoint.getPath()), equalTo(mountPoint));
            assertThat(FSMountPoint.create(mountPoint.getUri().toString()), equalTo(mountPoint));
            assertThat(FSMountPoint.create(mountPoint.getUri().toString()).hashCode(), equalTo(mountPoint.hashCode()));
            //assertThat(FSMountPoint.create(mountPoint.getScheme(), new FSPath(mountPoint.getParent(), mountPoint.resolveParentEntryName(ROOT))), equalTo(mountPoint));
            assertThat(FSMountPoint.create(mountPoint.resolve(ROOT).getUri()), equalTo(mountPoint));
        }
    }

    @Test
    public void testResolve() {
        for (final String[] params : new String[][] {
            { "foo:bar:/baz?plonk!/", "", "baz", "foo:bar:/baz?plonk!/" },
            { "foo:bar:/bäz?bööm!/", "bäng?plönk", "bäz/bäng?plönk", "foo:bar:/bäz?bööm!/bäng?plönk" },
            { "foo:bar:/baz!/", "bang?boom", "baz/bang?boom", "foo:bar:/baz!/bang?boom" },
            { "foo:bar:/baz!/", "bang", "baz/bang", "foo:bar:/baz!/bang" },
            { "foo:bar:/baz!/", "", "baz", "foo:bar:/baz!/" },
            { "foo:bar:/baz?plonk!/", "bang?boom", "baz/bang?boom", "foo:bar:/baz?plonk!/bang?boom" },
            { "foo:bar:/baz?plonk!/", "bang", "baz/bang", "foo:bar:/baz?plonk!/bang" },
            { "foo:/bar/?boom", "baz?bang", null, "foo:/bar/baz?bang" },
            { "foo:/bar/?boom", "baz", null, "foo:/bar/baz" },
            { "foo:/bar/?boom", "", null, "foo:/bar/" },
            { "foo:/bar/", "baz", null, "foo:/bar/baz" },
        }) {
            final FSMountPoint mountPoint = FSMountPoint.create(params[0]);
            final FSEntryName entryName = FSEntryName.create(params[1]);
            final FSEntryName parentEntryName = null == params[2] ? null : FSEntryName.create(params[2]);
            final FSPath path = FSPath.create(params[3]);
            if (null != parentEntryName)
                assertThat(mountPoint.getPath().resolve(entryName).getEntryName(), equalTo(parentEntryName));
            assertThat(mountPoint.resolve(entryName), equalTo(path));
            assertThat(mountPoint.resolve(entryName).getUri().isAbsolute(), is(true));
        }
    }

    @Test
    public void testHierarchicalize() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/x/bö%20m?plö%20k!/bä%20g?zö%20k!/", "baz:/x/bö%20m/bä%20g/?zö%20k" },
            { "foo:bar:baz:/x/bööm?plönk!/bäng?zönk!/", "baz:/x/bööm/bäng/?zönk" },
            { "foo:bar:baz:/boom?plonk!/bang?zonk!/", "baz:/boom/bang/?zonk" },
            { "foo:bar:baz:/boom!/bang!/", "baz:/boom/bang/" },
            { "foo:bar:/baz?boom!/", "bar:/baz/?boom" },
            { "foo:bar:/baz!/", "bar:/baz/" },
            { "foo:/bar/?boom", "foo:/bar/?boom" },
            { "foo:/bar/", "foo:/bar/" },
        }) {
            final FSMountPoint mp = FSMountPoint.create(params[0]);
            final FSMountPoint hmp = mp.hierarchicalize();
            final FSPath p = FSPath.create(params[0]);
            final FSPath hp = p.hierarchicalize();
            assertThat(hmp.hierarchicalize(), sameInstance(hmp));
            assertThat(hmp.getUri(), equalTo(URI.create(params[1])));
            assertThat(hmp.getUri(), equalTo(hp.getUri()));
        }
    }
}
