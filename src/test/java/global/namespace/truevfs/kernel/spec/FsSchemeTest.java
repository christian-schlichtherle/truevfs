/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.spec;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;
import java.util.Locale;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Schlichtherle
 */
public class FsSchemeTest {

    private static final Logger
            logger = LoggerFactory.getLogger(FsSchemeTest.class);

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
            final FsScheme copy = FsScheme.create(params[0].toUpperCase(Locale.ROOT));
            assertThat(original.toString(), equalTo(params[0]));
            assertThat(copy.toString(), not(equalTo(params[0])));
            assertThat(copy, equalTo(original));
            assertThat(copy.hashCode(), equalTo(original.hashCode()));
            assertThat(original.compareTo(copy), is(0));
        }
    }
}
