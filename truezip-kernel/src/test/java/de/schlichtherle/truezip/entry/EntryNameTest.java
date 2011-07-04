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
package de.schlichtherle.truezip.entry;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import org.junit.Test;

import static java.util.logging.Level.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class EntryNameTest {

    private static final Logger logger
            = Logger.getLogger(EntryNameTest.class.getName());

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
            { "föö/bär/", },
            { "föö/bär", },
            { "föö/", },
            { "föö", },
            { "föö?bär", },
            //{ "föö#bär", },
            //{ "#bär", },
            { "", },
            { "/", },
            { "/föö", },
            { ".", },
            { "./", },
            { "..", },
            { "../", },
            { "/.", },
            { "/./", },
            { "/..", },
            { "/../", },
        }) {
            final EntryName original = EntryName.create(URI.create(params[0]));

            {
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                final ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(original);
                oos.close();

                logger.log(FINE, "Number of serialized bytes: {0}", bos.size());

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

                logger.log(FINE, bos.toString("UTF-8"));

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
            "scheme:defined",
            "//authority/defined/",
            "fragment#defined",
        }) {
            final URI uri = URI.create(param);

            try {
                EntryName.create(uri);
                fail(param);
            } catch (IllegalArgumentException ex) {
            }

            try {
                new EntryName(uri);
                fail(param);
            } catch (URISyntaxException ex) {
            }
        }
    }

    @Test
    public void testConstructorWithValidUri() {
        for (final String[] params : new String[][] {
            //{ $parent, $member, $result },
            { "foo%3Abar", "baz", "foo%3Abar/baz" },
            { "foo", "bar%3Abaz", "foo/bar%3Abaz" },
            //{ "/../foo", "#bar", "/../foo#bar", },
            //{ "/../foo", "#", "/../foo#", },
            //{ "foo", "#bar", "foo#bar", },
            //{ "foo", "#", "foo#", },
            //{ "", "#foo", "#foo", },
            //{ "", "#", "#", },
            { "föö/", "?bär", "föö/?bär" },
            { "föö", "?bär", "föö?bär" },
            { "föö/?bär", "", "föö/" },
            { "föö?bär", "", "föö" },
            { "föö/?bär", "?tüü", "föö/?tüü" },
            { "föö?bär", "?tüü", "föö?tüü" },
            { "föö", "", "föö" },
            { "/", "föö", "/föö" },
            { "", "föö", "föö" },
            { "föö/", "bär", "föö/bär" },
            { "föö", "bär", "föö/bär" },
        }) {
            final EntryName parent = EntryName.create(URI.create(params[0]));
            final EntryName member = EntryName.create(URI.create(params[1]));
            final EntryName result = new EntryName(parent, member);
            assertThat(result.toUri(), equalTo(URI.create(params[2])));
            assertThat(EntryName.create(result.toUri()), equalTo(result));
        }
    }
}
