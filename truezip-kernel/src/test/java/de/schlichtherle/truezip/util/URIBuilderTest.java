/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
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

package de.schlichtherle.truezip.util;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 * @author Christian Schlichtherle
 * @version @version@
 */
public class URIBuilderTest extends TestCase {

    private URIBuilder builder;

    public URIBuilderTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        builder = new URIBuilder();
    }

    protected void tearDown() throws Exception {
    }

    public void testDefaults() throws URISyntaxException {
        assertNull(builder.getScheme());
        assertNull(builder.getUserInfo());
        assertNull(builder.getHost());
        assertEquals(-1, builder.getPort());
        assertNull(builder.getPath());
        assertNotNull(builder.getParameters());
        assertNotNull(builder.getParameterValues("any"));
        assertFalse(builder.isParameterSorting());
        assertNull(builder.getQuery());
        assertNull(builder.getFragment());
        assertEquals("", builder.toString());
        assertNotNull(builder.getURI());
    }

    public void testReset1() throws URISyntaxException {
        builder.setURI("http://java.sun.com");
        builder.setParameterSorting(true);

        builder.setURI((URI) null);
        testDefaults();
    }

    public void testReset2() throws URISyntaxException {
        builder.setURI("http://java.sun.com");
        builder.setParameterSorting(true);

        builder.setURI((String) null);
        testDefaults();
    }

    public void testReset3() throws URISyntaxException {
        builder.setURI("http://java.sun.com");
        builder.setParameterSorting(true);

        final String nil = null;
        builder.setScheme(nil);
        builder.setUserInfo(nil);
        builder.setHost(nil);
        builder.setPort(-1);
        builder.setPath(nil);
        builder.setQuery(nil);
        builder.setParameterSorting(false);
        builder.setFragment(nil);
        testDefaults();
    }

    public void testReset4() throws URISyntaxException {
        builder.setURI("http://java.sun.com");
        builder.setParameterSorting(true);

        final URI nil = null;
        builder.setScheme(nil);
        builder.setUserInfo(nil);
        builder.setHost(nil);
        builder.setPort(-1);
        builder.setPath(nil);
        builder.setQuery(nil);
        builder.setParameterSorting(false);
        builder.setFragment(nil);
        testDefaults();
    }

    public void testParsingAndComposition() throws URISyntaxException {
        final String[][] TEST_URIS = {
            // { URI, scheme, userInfo, host, port, path, query, fragment }
            { "http://java.sun.com", "http", null, "java.sun.com", "-1", "", null, null },
            { "http://java.sun.com/", "http", null, "java.sun.com", "-1", "/", null, null },
            { "http://java.sun.com/a", "http", null, "java.sun.com", "-1", "/a", null, null },
            { "http://java.sun.com/a/", "http", null, "java.sun.com", "-1", "/a/", null, null },
            { "http://java.sun.com/a/b", "http", null, "java.sun.com", "-1", "/a/b", null, null },
            { "http://java.sun.com/a/b/", "http", null, "java.sun.com", "-1", "/a/b/", null, null },
            { "http://java.sun.com?", "http", null, "java.sun.com", "-1", "", "", null },
            { "http://java.sun.com?&", "http", null, "java.sun.com", "-1", "", "&", null },
            { "http://java.sun.com?=", "http", null, "java.sun.com", "-1", "", "=", null },
            { "http://java.sun.com?q", "http", null, "java.sun.com", "-1", "", "q", null },
            { "http://java.sun.com?&&", "http", null, "java.sun.com", "-1", "", "&&", null },
            { "http://java.sun.com?&=", "http", null, "java.sun.com", "-1", "", "&=", null },
            { "http://java.sun.com?&q", "http", null, "java.sun.com", "-1", "", "&q", null },
            { "http://java.sun.com?=&", "http", null, "java.sun.com", "-1", "", "=&", null },
            { "http://java.sun.com?==", "http", null, "java.sun.com", "-1", "", "==", null },
            { "http://java.sun.com?=q", "http", null, "java.sun.com", "-1", "", "=q", null },
            { "http://java.sun.com?q&", "http", null, "java.sun.com", "-1", "", "q&", null },
            { "http://java.sun.com?q=", "http", null, "java.sun.com", "-1", "", "q=", null },
            { "http://java.sun.com?qq", "http", null, "java.sun.com", "-1", "", "qq", null },
            { "http://java.sun.com?q=d&q=c&r=b&r=a", "http", null, "java.sun.com", "-1", "", "q=d&q=c&r=b&r=a", null },
            { "http://java.sun.com?q=a&r=b&q=c&r=d", "http", null, "java.sun.com", "-1", "", "q=a&r=b&q=c&r=d", null },
            { "mailto:christian@schlichtherle.de", "mailto", null, null, "-1", "christian@schlichtherle.de", null, null },
            { "mailto:christian@schlichtherle.de?foo", "mailto", null, null, "-1", "christian@schlichtherle.de", "foo", null },
            { "mailto:christian@schlichtherle.de?foo&bar", "mailto", null, null, "-1", "christian@schlichtherle.de", "foo&bar", null },
        };

        final URIBuilder builder = new URIBuilder();
        for (int i = 0; i < TEST_URIS.length; i++) {
            final String[] TEST_URI = TEST_URIS[i];

            //final URIBuilder builder = new URIBuilder();
            builder.setURI(TEST_URI[0]); // reset URI

            // Test parsing.
            assertEquals(TEST_URI[1], builder.getScheme());
            assertEquals(TEST_URI[2], builder.getUserInfo());
            assertEquals(TEST_URI[3], builder.getHost());
            assertEquals(TEST_URI[4], Integer.toString(builder.getPort()));
            assertEquals(TEST_URI[5], builder.getPath());
            assertEquals(TEST_URI[6], builder.getQuery());
            assertEquals(TEST_URI[7], builder.getFragment());

            // Test composition.
            assertEquals(        TEST_URI[0] ,  builder.toString());
            assertEquals(new URI(TEST_URI[0]),  builder.getURI());
        }
    }

    public void testGetParameters() throws URISyntaxException {
        assertTrue(builder.getParameters().isEmpty());
        assertEquals(0, builder.getParameters().size());

        try {
            builder.getParameters().add(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        try {
            builder.getParameters().add("a&b");
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        assertTrue(builder.getParameters().isEmpty());

        final List<String> parameters = Collections.unmodifiableList(
                Arrays.asList(new String[] { "", "=", "==", "a", "a=", "a==", "a=1" }));

        for (String parameter : parameters)
            assertTrue(builder.getParameters().add(parameter));
        assertFalse(builder.getParameters().isEmpty());

        final List<String> test = new ArrayList<String>(builder.getParameters());
        assertEquals(parameters, test);

        assertEquals("&=&==&a&a=&a==&a=1", builder.getURI().getQuery());

        for (String parameter : parameters)
            assertTrue(builder.getParameters().remove(parameter));

        assertTrue(builder.getParameters().isEmpty());
        assertEquals(0, builder.getParameters().size());
        assertEquals(null, builder.getURI().getQuery());
    }

    public void testGetParameterNames() throws URISyntaxException {
        try {
            builder.getParameterNames().add(null);
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException expected) {
        }

        try {
            builder.getParameterNames().add("");
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException expected) {
        }

        try {
            builder.getParameterNames().add("a");
            fail("Expected UnsupportedOperationException!");
        } catch (UnsupportedOperationException expected) {
        }

        builder.setQuery("a=1&b=2&c=3&a=4&b=5&c=6");
        assertEquals(   new HashSet<String>(Arrays.asList("a", "b", "c")),
                        builder.getParameterNames());

        assertTrue(builder.getParameterNames().remove("b"));
        assertEquals(   new HashSet<String>(Arrays.asList("a", "c")),
                        builder.getParameterNames());
        assertEquals("a=1&c=3&a=4&c=6", builder.getURI().getQuery());
    }

    public void testGetParameterValues() throws URISyntaxException {
        try {
            builder.getParameterValues(null);
            fail("Expected NullPointerException!");
        } catch (NullPointerException expected) {
        }

        try {
            builder.getParameterValues("a=1");
            //fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        try {
            builder.getParameterValues("a&b");
            //fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }

        /*try {
            builder.getParameterValues("a").add("1&2");
            fail("Expected IllegalArgumentException!");
        } catch (IllegalArgumentException expected) {
        }*/

        assertTrue(builder.getParameterValues("a").add("1"));
        assertEquals("a=1", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("b").add("2"));
        assertEquals("a=1&b=2", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("b").add("2"));
        assertEquals("a=1&b=2&b=2", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("b").remove("2"));
        assertEquals("a=1&b=2", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("c").add("3"));
        assertEquals("a=1&b=2&c=3", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("b").add("4"));
        assertEquals("a=1&b=2&c=3&b=4", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("b").remove("2"));
        assertEquals("a=1&c=3&b=4", builder.getURI().getQuery());

        assertFalse(builder.getParameterValues("b").remove("2"));
        assertEquals("a=1&c=3&b=4", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("c").add("5"));
        assertEquals("a=1&c=3&b=4&c=5", builder.getURI().getQuery());
        assertEquals(0, builder.getParameterValues("").size());
        assertEquals(1, builder.getParameterValues("a").size());
        assertEquals(1, builder.getParameterValues("b").size());
        assertEquals(2, builder.getParameterValues("c").size());

        builder.getParameterValues("c").clear();
        assertEquals("a=1&b=4", builder.getURI().getQuery());

        builder.getParameterValues("a").clear();
        assertEquals("b=4", builder.getURI().getQuery());

        builder.getParameterValues("b").clear();
        assertEquals(null, builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("").add(null));
        assertEquals("", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("").add(""));
        assertEquals("&=", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("").add("="));
        assertEquals("&=&==", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("").add("=="));
        assertEquals("&=&==&===", builder.getURI().getQuery());
    }

    public void testMixedParameterAccess() throws URISyntaxException {
        assertEquals(null, builder.getURI().getQuery());

        assertTrue(builder.getParameters().add("a=1"));
        assertEquals("a=1", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("a").add("2"));
        assertEquals("a=1&a=2", builder.getURI().getQuery());

        assertTrue(builder.getParameters().add("a=3"));
        assertEquals("a=1&a=2&a=3", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("a").add("4"));
        assertEquals("a=1&a=2&a=3&a=4", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("a").remove("2"));
        assertEquals("a=1&a=3&a=4", builder.getURI().getQuery());

        assertTrue(builder.getParameterValues("a").remove("3"));
        assertEquals("a=1&a=4", builder.getURI().getQuery());

        assertTrue(builder.getParameters().remove("a=4"));
        assertEquals("a=1", builder.getURI().getQuery());

        assertFalse(builder.getParameters().remove("a=4"));
        assertEquals("a=1", builder.getURI().getQuery());
    }

    public void testParameterSorting() throws URISyntaxException {
        builder.setURI("http://java.sun.com?b=b&b=a&a=b&a=a");
        assertEquals("http://java.sun.com?b=b&b=a&a=b&a=a", builder.toString());

        builder.setParameterSorting(true);
        assertEquals("http://java.sun.com?a=a&a=b&b=a&b=b", builder.toString());

        builder.setParameterSorting(false);
        assertEquals("http://java.sun.com?b=b&b=a&a=b&a=a", builder.toString());
    }
}
