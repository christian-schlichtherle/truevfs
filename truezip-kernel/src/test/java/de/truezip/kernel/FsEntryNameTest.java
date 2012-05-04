/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel;

import de.truezip.kernel.FsEntryName;
import static de.truezip.kernel.FsUriModifier.CANONICALIZE;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsEntryNameTest {

    private static final Logger
            logger = Logger.getLogger(FsEntryNameTest.class.getName());

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
            final FsEntryName original = FsEntryName.create(URI.create(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(original);
                }

                logger.log(Level.FINEST, "Number of serialized bytes: {0}", bos.size());

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

                logger.log(Level.FINEST, bos.toString("UTF-8"));

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
                FsEntryName.create(uri);
                fail(param);
            } catch (IllegalArgumentException ex) {
            }

            try {
                new FsEntryName(uri);
                fail(param);
            } catch (URISyntaxException ex) {
            }
        }
    }

    @Test
    public void testConstructorWithValidUri() {
        for (final String[] params : new String[][] {
            // { $parent, $member, $result },
            { "foo%3Abar", "baz", "foo%3Abar/baz" },
            { "foo", "bar%3Abaz", "foo/bar%3Abaz" },
            { "foo", "", "foo", },
            { "", "", "", },
            { "föö", "?bär", "föö?bär" },
            { "föö?bär", "", "föö" },
            { "föö?bär", "?täscht", "föö?täscht" },
            { "föö", "", "föö" },
            { "", "föö", "föö" },
            { "föö", "bär", "föö/bär" },
        }) {
            final FsEntryName parent = FsEntryName.create(URI.create(params[0]));
            final FsEntryName member = FsEntryName.create(URI.create(params[1]));
            final FsEntryName result = new FsEntryName(parent, member);
            assertThat(result.toUri(), equalTo(URI.create(params[2])));
            assertThat(FsEntryName.create(result.toUri()), equalTo(result));
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
            final FsEntryName name = FsEntryName.create(uri, CANONICALIZE);
            final URI result = name.toUri();
            assertThat(result, equalTo(expected));
        }
    }

    @Test
    public void testIsRoot() {
        for (final Object params[] : new Object[][] {
            { "", true },
            { "?", false, },
        }) {
            assertThat(FsEntryName.create(URI.create(params[0].toString())).isRoot(), is(params[1]));
        }
    }
}