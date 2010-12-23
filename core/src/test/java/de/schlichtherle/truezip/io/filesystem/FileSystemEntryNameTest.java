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
package de.schlichtherle.truezip.io.filesystem;

import de.schlichtherle.truezip.io.entry.EntryNameTest;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class FileSystemEntryNameTest {

    private static final Logger LOGGER
            = Logger.getLogger(FileSystemEntryNameTest.class.getName());

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        final ExceptionListener listener = new ExceptionListener() {
            @Override
            public void exceptionThrown(Exception ex) {
                throw new UndeclaredThrowableException(ex);
            }
        };


        for (final String[] params : new String[][] {
            { "föö bär", null },
            { "föö/bär", },
            { "föö", },
            { "föö?bär", },
            { "", },
        }) {
            final FileSystemEntryName original = 1 < params.length
                    ? FileSystemEntryName.create(params[0], params[1])
                    : FileSystemEntryName.create(params[0]);

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(original);
                oos.close();

                LOGGER.log(Level.FINE, "Number of serialized bytes: {0}", bos.size());

                final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                final ObjectInputStream ois = new ObjectInputStream(bis);
                final Object clone = ois.readObject();
                ois.close();

                assertThat(clone, not(sameInstance((Object) original)));
                assertThat(clone, equalTo((Object) original));
            }

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final XMLEncoder enc = new XMLEncoder(bos);
                enc.setExceptionListener(listener);
                enc.writeObject(original);
                enc.close();

                LOGGER.log(Level.FINE, bos.toString("UTF-8"));

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
            "foo#bar",
            "#foo",
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
                FileSystemEntryName.create(uri);
                fail(param);
            } catch (IllegalArgumentException ex) {
            }

            try {
                new FileSystemEntryName(uri);
                fail(param);
            } catch (URISyntaxException ex) {
            }
        }
    }

    @Test
    public void testConstructorWithValidUri() {
        for (final String[] params : new String[][] {
            { "föö", "?bär", "föö?bär" },
            { "föö?bär", "", "föö" },
            { "föö?bär", "?tüü", "föö?tüü" },
            { "föö", "", "föö" },
            { "", "föö", "föö" },
            { "föö", "bär", "föö/bär" },
        }) {
            final FileSystemEntryName parent = FileSystemEntryName.create(URI.create(params[0]));
            final FileSystemEntryName member = FileSystemEntryName.create(URI.create(params[1]));
            final FileSystemEntryName result = new FileSystemEntryName(parent, member);
            assertThat(result.getUri(), equalTo(URI.create(params[2])));
            assertThat(FileSystemEntryName.create(result.getUri()), equalTo(result));
        }
    }
}
