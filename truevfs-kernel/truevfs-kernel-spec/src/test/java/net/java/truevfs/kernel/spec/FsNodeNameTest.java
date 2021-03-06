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

import static net.java.truevfs.kernel.spec.FsUriModifier.CANONICALIZE;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Christian Schlichtherle
 */
public class FsNodeNameTest {

    private static final Logger
            logger = LoggerFactory.getLogger(FsNodeNameTest.class);

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };


        for (final String[] params : new String[][] {
            { "föö%20bär", },
            { "föö/bär", },
            { "föö", },
            { "föö?bär", },
            { "", },
        }) {
            final FsNodeName original = FsNodeName.create(URI.create(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(original);
                }

                logger.trace("Number of serialized bytes: {}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final ObjectInputStream ois = new ObjectInputStream(bis);
                final Object clone = ois.readObject();
                ois.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
            }

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (final XMLEncoder enc = new XMLEncoder(bos)) {
                    enc.setExceptionListener(listener);
                    enc.writeObject(original);
                }

                logger.trace("XML String: {}", bos.toString("UTF-8"));

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final XMLDecoder dec = new XMLDecoder(bis);
                final Object clone = dec.readObject();
                dec.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
            }
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testConstructorWithInvalidUri() {
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
            "foo#fragmentDefined",
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
            "foo:bar:/baz!/#",
            "foo:bar:/baz!/#bang",
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
                FsNodeName.create(uri);
                fail(param);
            } catch (IllegalArgumentException ignored) {
            }

            try {
                new FsNodeName(uri);
                fail(param);
            } catch (URISyntaxException ignored) {
            }
        }
    }

    @Test
    public void testConstructorWithValidUri() {
        for (final Object[] params : new Object[][] {
            // { $parent, $member, $result },
            { "", "", "",  true, "", null},
            { "foo%3Abar", "baz", "foo%3Abar/baz", false, "foo:bar/baz", null },
            { "foo", "bar%3Abaz", "foo/bar%3Abaz", false, "foo/bar:baz", null },
            { "foo", "?bar%3Abaz", "foo?bar%3Abaz", false, "foo", "bar:baz" },
            { "foo", "", "foo", false, "foo", null },
            { "föö", "?bär", "föö?bär", false, "föö", "bär" },
            { "föö?bär", "", "föö", false, "föö", null },
            { "föö?bär", "?täscht", "föö?täscht", false, "föö", "täscht" },
            { "föö", "", "föö", false, "föö", null },
            { "", "föö", "föö", false, "föö", null },
            { "föö", "bär", "föö/bär", false, "föö/bär", null },
        }) {
            final FsNodeName parent = FsNodeName.create(URI.create((String) params[0]));
            final FsNodeName member = FsNodeName.create(URI.create((String) params[1]));
            final FsNodeName result = new FsNodeName(parent, member);
            assertThat(result.getUri(), equalTo(URI.create((String) params[2])));
            assertThat(result.isRoot(), equalTo(params[3]));
            assertThat(result.getPath(), equalTo(params[4]));
            assertThat(result.getQuery(), equalTo(params[5]));
            assertThat(FsNodeName.create(result.getUri()), equalTo(result));
        }
    }

    @Test
    public void testCanonicalization() {
        for (final String[] params : new String[][] {
            // { $uri, $expected },
            { "föö/", "föö" },
            { "/föö", "föö" },
            { "/föö/", "föö" },
            { "/C:/", "C%3A" },
            { "C%3A/", "C%3A" },
        }) {
            final URI uri = URI.create(params[0]);
            final URI expected = URI.create(params[1]);
            final FsNodeName name = FsNodeName.create(uri, CANONICALIZE);
            final URI result = name.getUri();
            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void testIsRoot() {
        for (final Object params[] : new Object[][] {
            { "", true },
            { "?", false, },
        }) {
            assertThat(FsNodeName.create(URI.create(params[0].toString())).isRoot(), is(params[1]));
        }
    }
}
