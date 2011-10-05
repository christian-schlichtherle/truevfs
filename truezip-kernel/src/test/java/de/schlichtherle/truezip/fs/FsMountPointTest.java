/*
 * Copyright (C) 2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.fs;

import static de.schlichtherle.truezip.fs.FsUriModifier.*;
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

import static de.schlichtherle.truezip.fs.FsEntryName.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsMountPointTest {

    private static final Logger logger
            = Logger.getLogger(FsMountPoint.class.getName());

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
            final FsMountPoint original = FsMountPoint.create(URI.create(params[0]));
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
            FsMountPoint.create((URI) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsMountPoint((URI) null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsMountPoint.create((URI) null, NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsMountPoint((URI) null, NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsMountPoint.create((URI) null, CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsMountPoint((URI) null, CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsMountPoint.create((FsScheme) null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsMountPoint((FsScheme) null, null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String param : new String[] {
            "foo:/?queryDefined",
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
            "foo:bar!/",
            "foo:bar:baz!/",
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
                FsMountPoint.create(uri);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }

            try {
                new FsMountPoint(uri);
                fail(param);
            } catch (URISyntaxException expected) {
            }
        }

        for (final String[] params : new String[][] {
            //{ "foo", "bar:baz:/bang!/boom" },
            { "foo", "bar:baz:/bang!/" },
            { "foo", "bar:/baz/" },
        }) {
            final FsScheme scheme = FsScheme.create(params[0]);
            final FsPath path = FsPath.create(URI.create(params[1]));
            try {
                new FsMountPoint(scheme, path);
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
        }) {
            final URI uri = URI.create(param);
            final FsMountPoint mountPoint = FsMountPoint.create(uri);
            assertThat(mountPoint.toUri(), sameInstance(uri));
            assertThat(mountPoint.getPath(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.toUri().toString()));
            assertThat(mountPoint, equalTo(FsMountPoint.create(mountPoint.toUri())));
            assertThat(mountPoint.hashCode(), equalTo(FsMountPoint.create(mountPoint.toUri()).hashCode()));
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
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]), FsUriModifier.CANONICALIZE);
            final FsScheme scheme = FsScheme.create(params[1]);
            final FsPath path = FsPath.create(URI.create(params[2]));

            assertThat(mountPoint.getScheme(), equalTo(scheme));
            assertThat(mountPoint.getPath(), equalTo(path));
            assertThat(mountPoint.toString(), equalTo(mountPoint.toUri().toString()));
            assertThat(FsMountPoint.create(mountPoint.toUri()), equalTo(mountPoint));
            assertThat(FsMountPoint.create(URI.create(mountPoint.toUri().getScheme() + ":" + mountPoint.getPath() + "!/")), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.getScheme(), mountPoint.getPath()), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.toUri()), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.toUri()).hashCode(), equalTo(mountPoint.hashCode()));
            //assertThat(FsMountPoint.create(mountPoint.getScheme(), new FsPath(mountPoint.getParent(), mountPoint.resolveParentEntryName(ROOT))), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.resolve(ROOT).toUri()), equalTo(mountPoint));
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
            { "foo:/bar/", "baz?bang", null, "foo:/bar/baz?bang" },
            { "foo:/bar/", "baz", null, "foo:/bar/baz" },
            { "foo:/bar/", "", null, "foo:/bar/" },
            { "foo:/bar/", "baz", null, "foo:/bar/baz" },
        }) {
            final FsMountPoint mountPoint = FsMountPoint.create(URI.create(params[0]));
            final FsEntryName entryName = FsEntryName.create(URI.create(params[1]));
            final FsEntryName parentEntryName = null == params[2] ? null : FsEntryName.create(URI.create(params[2]));
            final FsPath path = FsPath.create(URI.create(params[3]));
            if (null != parentEntryName)
                assertThat(mountPoint.getPath().resolve(entryName).getEntryName(), equalTo(parentEntryName));
            assertThat(mountPoint.resolve(entryName), equalTo(path));
            assertThat(mountPoint.resolve(entryName).toUri().isAbsolute(), is(true));
        }
    }

    @Test
    public void testHierarchicalize() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/x/bö%20m?plö%20k!/bä%20g?zö%20k!/", "baz:/x/bö%20m/bä%20g?zö%20k" },
            { "foo:bar:baz:/x/bööm?plönk!/bäng?zönk!/", "baz:/x/bööm/bäng?zönk" },
            { "foo:bar:baz:/boom?plonk!/bang?zonk!/", "baz:/boom/bang?zonk" },
            { "foo:bar:baz:/boom!/bang!/", "baz:/boom/bang" },
            { "foo:bar:/baz?boom!/", "bar:/baz?boom" },
            { "foo:bar:/baz!/", "bar:/baz" },
            { "foo:/bar/", "foo:/bar/" },
        }) {
            final FsMountPoint mp = FsMountPoint.create(URI.create(params[0]));
            final URI hmp = mp.toHierarchicalUri();
            final FsPath p = FsPath.create(URI.create(params[0]));
            final URI hp = p.toHierarchicalUri();
            assertThat(hmp, equalTo(URI.create(params[1])));
            assertThat(hmp, equalTo(hp));
        }
    }
}
