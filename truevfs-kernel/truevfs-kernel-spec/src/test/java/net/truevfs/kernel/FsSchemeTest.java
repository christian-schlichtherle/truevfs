/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel;

import net.truevfs.kernel.FsScheme;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Christian Schlichtherle
 */
public class FsSchemeTest {

    private static final Logger
            logger = Logger.getLogger(FsSchemeTest.class.getName());

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };

        for (final String[] params : new String[][] {
            { "foo", },
            { "foo+bar", },
            { "foo-bar", },
            { "foo.bar", },
            { "tar.bz2", },
        }) {
            final FsScheme original = FsScheme.create(params[0]);
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
            FsScheme.create(null);
            fail();
        } catch (NullPointerException expected) {
        }

        try {
            new FsScheme(null);
            fail();
        } catch (NullPointerException expected) {
        }

        for (final String param : new String[] {
            "",
            "+",
            "-",
            ".",
        }) {
            try {
                FsScheme.create(param);
                fail(param);
            } catch (IllegalArgumentException expected) {
            }

            try {
                new FsScheme(param);
                fail(param);
            } catch (URISyntaxException expected) {
            }
        }
    }

    @Test
    public void testEquality() {
        for (final String[] params : new String[][] {
            { "foo", },
            { "foo+bar", },
            { "foo-bar", },
            { "foo.bar", },
            { "tar.bz2", },
        }) {
            final FsScheme original = FsScheme.create(params[0]);
            final FsScheme copy = FsScheme.create(params[0].toUpperCase(Locale.ENGLISH));
            assertThat(original.toString(), equalTo(params[0]));
            assertThat(copy.toString(), not(equalTo(params[0])));
            assertThat(copy, equalTo(original));
            assertThat(copy.hashCode(), equalTo(original.hashCode()));
            assertThat(original.compareTo(copy), is(0));
        }
    }
}
