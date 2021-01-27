/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.spec;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;

import static net.java.truevfs.kernel.spec.FsNodeName.ROOT;
import static net.java.truevfs.kernel.spec.FsUriModifier.CANONICALIZE;
import static net.java.truevfs.kernel.spec.FsUriModifier.NULL;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Christian Schlichtherle
 */
public class FsMountPointTest {

    private static final Logger
            logger = LoggerFactory.getLogger(FsMountPoint.class);

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
                try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(original);
                }

                logger.trace("Number of serialized bytes: {}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final Object clone;
                try (final ObjectInputStream ois = new ObjectInputStream(bis)) {
                    clone = ois.readObject();
                }

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
                assertThat(clone.toString(), equalTo(params[0]));
            }

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (final XMLEncoder enc = new XMLEncoder(bos)) {
                    enc.setExceptionListener(listener);
                    enc.writeObject(original);
                }

                logger.trace("XML String: {}", bos.toString("UTF-8"));

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final Object clone;
                try (final XMLDecoder dec = new XMLDecoder(bis)) {
                    clone = dec.readObject();
                }

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
            final FsNodePath path = FsNodePath.create(URI.create(params[1]));
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
            assertThat(mountPoint.getUri(), sameInstance(uri));
            assertThat(mountPoint.getPath(), nullValue());
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(mountPoint, equalTo(FsMountPoint.create(mountPoint.getUri())));
            assertThat(mountPoint.hashCode(), equalTo(FsMountPoint.create(mountPoint.getUri()).hashCode()));
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
            final FsNodePath path = FsNodePath.create(URI.create(params[2]));

            assertThat(mountPoint.getScheme(), equalTo(scheme));
            assertThat(mountPoint.getPath(), equalTo(path));
            assertThat(mountPoint.toString(), equalTo(mountPoint.getUri().toString()));
            assertThat(FsMountPoint.create(mountPoint.getUri()), equalTo(mountPoint));
            assertThat(FsMountPoint.create(URI.create(mountPoint.getUri().getScheme() + ":" + mountPoint.getPath() + "!/")), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.getScheme(), mountPoint.getPath()), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.getUri()), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.getUri()).hashCode(), equalTo(mountPoint.hashCode()));
            //assertThat(FsMountPoint.create(mountPoint.getScheme(), new FsNodePath(mountPoint.getParent(), mountPoint.resolveParentEntryName(ROOT))), equalTo(mountPoint));
            assertThat(FsMountPoint.create(mountPoint.resolve(ROOT).getUri()), equalTo(mountPoint));
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
            final FsNodeName entryName = FsNodeName.create(URI.create(params[1]));
            final FsNodeName parentEntryName = null == params[2] ? null : FsNodeName.create(URI.create(params[2]));
            final FsNodePath path = FsNodePath.create(URI.create(params[3]));
            if (null != parentEntryName)
                assertThat(mountPoint.getPath().resolve(entryName).getNodeName(), equalTo(parentEntryName));
            assertThat(mountPoint.resolve(entryName), equalTo(path));
            assertThat(mountPoint.resolve(entryName).getUri().isAbsolute(), is(true));
        }
    }

    @Test
    public void testToHierarchicalUri() {
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
            final FsNodePath p = FsNodePath.create(URI.create(params[0]));
            final URI hp = p.toHierarchicalUri();
            assertThat(hmp, equalTo(URI.create(params[1])));
            assertThat(hmp, equalTo(hp));
        }
    }
}
