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
import java.net.URISyntaxException;
import java.util.Locale;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FsSchemeTest {

    private static final Logger logger
            = Logger.getLogger(FsSchemeTest.class.getName());

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
