/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.addr;

import static de.truezip.kernel.addr.FsUriModifier.CANONICALIZE;
import static de.truezip.kernel.addr.FsUriModifier.NULL;
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
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsPathTest {

    private static final Logger
            logger = Logger.getLogger(FsPathTest.class.getName());

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
            final FsPath original = FsPath.create(URI.create(params[0]));
            assertThat(original.toString(), equalTo(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(original);
                }

                logger.log(Level.FINEST, "Number of serialized bytes: {0}", bos.size());

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

                logger.log(Level.FINEST, bos.toString("UTF-8"));

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
            FsPath.create((URI) null, NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((URI) null, NULL);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            FsPath.create((URI) null, CANONICALIZE);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsPath((URI) null, CANONICALIZE);
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
            "foo#fragmentDefined",
            "foo/",
            "foo//",
            "foo/.",
            "foo/./",
            "foo/..",
            "foo/../",
            "foo:/bar/.",
            "foo:/bar/..",
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
            //{ $path, $mountPoint, $entryName },
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
            { "foo:bar:/baz?bang!/?plonk", "foo:bar:/baz?bang!/", "?plonk" },

            //{ "foo:bar:/baz!/bang//", "foo:bar:/baz!/", "bang/" },
            //{ "foo:bar:/baz!/bang/.", "foo:bar:/baz!/", "bang/" },
            //{ "foo:bar:/baz!/bang/./", "foo:bar:/baz!/", "bang/" },
            { "foo:bar:/baz!/bang/..", "foo:bar:/baz!/", "" },
            { "foo:bar:/baz!/bang/../", "foo:bar:/baz!/", "" },

            { "", null, "", },
            { "foo", null, "foo" },
            //{ "foo/", null, "foo/" },
            //{ "foo//", null, "foo/" },
            //{ "foo/.", null, "foo/" },
            //{ "foo/./", null, "foo/" },
            { "foo/..", null, "" },
            { "foo/../", null, "" },
            { "foo/bar", null, "foo/bar" },
            //{ "foo/bar/", null, "foo/bar/" },
            { "foo:/", "foo:/", "" },
            { "foo:/bar", "foo:/", "bar" },
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

            { "file:////host/share/file", "file://host/share/", "file" },
            { "file://host/share/file", "file://host/share/", "file" },

            { "file:///foo/c%3A//", "file:/foo/", "c%3A" },
            { "file:/foo/c%3A//", "file:/foo/", "c%3A" },
            //{ "file:////c://", "file:/c:/", "" },
            { "file:///c://", "file:/c:/", "" },
            { "file:/c://", "file:/c:/", "" },
            { "file:/c%3A", "file:/", "c%3A" },
            { "föö/", null, "föö" },
            { "/föö", null, "föö" },
            //{ "//föö", null, "föö" },
            { "///föö", null, "föö" },
            { "////föö", null, "föö" },
            { "/föö/", null, "föö" },
            { "/C%3A/", null, "C%3A" },
            { "C%3A/", null, "C%3A" },
        }) {
            FsPath path = FsPath.create(URI.create(params[0]), CANONICALIZE);
            final FsMountPoint mountPoint = null == params[1] ? null : FsMountPoint.create(URI.create(params[1]));
            final FsEntryName entryName = FsEntryName.create(URI.create(params[2]));
            assertPath(path, mountPoint, entryName);
            path = new FsPath(mountPoint, entryName);
            assertPath(path, mountPoint, entryName);
        }
    }

    private void assertPath(final FsPath path,
                            final FsMountPoint mountPoint,
                            final FsEntryName entryName) {
        if (null != mountPoint)
            assertThat(path.getMountPoint(), equalTo(mountPoint));
        else
            assertThat(path.getMountPoint(), nullValue());
        assertThat(path.getEntryName(), equalTo(entryName));
        assertThat(path.toString(), equalTo(path.toUri().toString()));
        assertThat(FsPath.create(path.toUri()), equalTo(path));
        assertThat(FsPath.create(path.toUri()).hashCode(), equalTo(path.hashCode()));
    }

    @Test
    public void testSpaces() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/%20!/%20/%20!/%20/%20", " ", " / ", " / ", },
            { "foo:bar:baz:/%20a%20!/%20b%20!/%20c%20", " a ", " b ", " c ", },
        }) {
            FsPath path = FsPath.create(URI.create(params[0]));
            for (int i = params.length; 0 < --i; ) {
                assertThat(path.getEntryName().getPath(), equalTo(params[i]));
                path = path.getMountPoint().getPath();
            }
        }
    }

    @Test
    public void testHierarchicalize() {
        for (final String[] params : new String[][] {
            { "foo:bar:baz:/x/bö%20m?plö%20k!/bä%20g?zö%20k!/", "baz:/x/bö%20m/bä%20g?zö%20k" },
            { "bar:baz:/x/bö%20m?plö%20k!/bä%20g?zö%20k", "baz:/x/bö%20m/bä%20g?zö%20k" },
            { "foo:bar:baz:/x/bööm?plönk!/bäng?zönk!/", "baz:/x/bööm/bäng?zönk" },
            { "bar:baz:/x/bööm?plönk!/bäng?zönk", "baz:/x/bööm/bäng?zönk" },
            { "foo:bar:baz:/boom?plonk!/bang?zonk!/", "baz:/boom/bang?zonk" },
            { "bar:baz:/boom?plonk!/bang?zonk", "baz:/boom/bang?zonk" },
            { "bar:baz:/boom?plonk!/?zonk", "baz:/boom/?zonk" },
            { "bar:baz:/boom?plonk!/bang", "baz:/boom/bang" },
            { "bar:baz:/boom?plonk!/", "baz:/boom?plonk" },
            { "foo:bar:baz:/boom!/bang!/", "baz:/boom/bang" },
            { "bar:baz:/boom!/bang", "baz:/boom/bang" },
            { "foo:bar:/baz?boom!/", "bar:/baz?boom" },
            { "bar:/baz?boom", "bar:/baz?boom" },
            { "foo:bar:/baz!/", "bar:/baz" },
            { "bar:/baz", "bar:/baz" },
            { "foo:/bar/?boom", "foo:/bar/?boom" },
            { "bar?boom", "bar?boom" },
            { "foo:/bar/", "foo:/bar/" },
            { "bar", "bar" },
        }) {
            final FsPath path = FsPath.create(URI.create(params[0]));
            final URI hierarchical = path.toHierarchicalUri();
            assertThat(hierarchical, equalTo(URI.create(params[1])));
        }
    }
}
