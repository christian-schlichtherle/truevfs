/*
 * Copyright (C) 2007-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import static java.util.logging.Level.*;
import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class UriBuilderTest {

    private static final Logger
            logger = Logger.getLogger(UriBuilderTest.class.getName());
    
    private UriBuilder builder;

    @Before
    public void setUp() {
        builder = new UriBuilder();
    }

    @Test
    public void testDefaults() {
        assertNull(builder.getScheme());
        assertNull(builder.getAuthority());
        assertNull(builder.getPath());
        assertNull(builder.getQuery());
        assertNull(builder.getFragment());
        assertEquals("", builder.toString());
        assertEquals(URI.create(""), builder.toUri());
    }

    @Test
    public void testClear1() {
        builder .uri(URI.create("scheme://authority/path?query#fragment"))
                .clear();
        testDefaults();
    }

    @Test
    public void testClear2() {
        builder .uri(URI.create("scheme://authority/path?query#fragment"))
                .scheme(null)
                .authority(null)
                .path(null)
                .query(null)
                .fragment(null);
        testDefaults();
    }

    @Test
    public void testRoundTrip() {
        for (final String[] test : new String[][] {
            // { URI, scheme, authority, path, query, fragment }
            { "scheme://authority/path?query#fragment", "scheme", "authority", "/path", "query", "fragment" },
            { "foo%3Abar", null, null, "foo:bar", null, null },
            { "/foo:bar", null, null, "/foo:bar", null, null },
            { "//foo:bar", null, "foo:bar", "", null, null },
            { "//foo:bar/", null, "foo:bar", "/", null, null },
            { "foo:bar", "foo", null, "bar", null, null },
            { "", null, null, "", null, null },
            { "?query", null, null, "", "query", null },
            { "#foo?bar%23baz", null, null, "", null, "foo?bar#baz" },
            { "?foo?bar%23baz#foo?bar%23baz", null, null, "", "foo?bar#baz", "foo?bar#baz" },
            { "0", null, null, "0", null, null },
            { "0%3A", null, null, "0:", null, null },
            { "scheme:scheme-specific-part#fragment", "scheme", null, "scheme-specific-part", null, "fragment" },
            { "scheme:scheme-specific-part?noquery#fragment", "scheme", null, "scheme-specific-part?noquery", null, "fragment" },
            { "scheme:?#", "scheme", null, "?", null, "" },
            { "scheme:?", "scheme", null, "?", null, null },
        }) {
            final URI u = URI.create(test[0]);

            // Test parsing.
            builder.setUri(u);
            assertEquals(test[1], builder.getScheme());
            assertEquals(test[2], builder.getAuthority());
            assertEquals(test[3], builder.getPath());
            assertEquals(test[4], builder.getQuery());
            assertEquals(test[5], builder.getFragment());
            assertEquals(test[0], builder.toString());

            // Test composition.
            builder .clear()
                    .scheme(test[1])
                    .authority(test[2])
                    .path(test[3])
                    .query(test[4])
                    .fragment(test[5]);
            assertEquals(test[0], builder.toString());
            
            // Test identities.
            assertEquals(u, builder.toUri());
            assertEquals(u, builder.uri(u).toUri());
        }
    }

    @Test
    public void testIllegalState() {
        for (final String[] test : new String[][] {
            // { scheme, authority, path, query, fragment }

            // Illegal scheme.
            { "", null, null, null, null },
            { "0", null, null, null, null },
            { "+", null, null, null, null },
            { "-", null, null, null, null },
            { ".", null, null, null, null },
            { "a$", null, null, null, null },
            
            // Empty scheme specific part in absolute URI.
            { "scheme", null, null, null, null },
            { "scheme", null, null, null, "fragment" },
            { "scheme", null, "", null, null },
            { "scheme", null, "", null, "fragment" },

            // Relative path with empty authority.
            { null, "", "path", null, null },
            { null, "", "path", null, "fragment" },
            { null, "", "path", "query", null },
            { null, "", "path", "query", "fragment" },
            { "scheme", "", "path", "query", "fragment" },

            // Relative path with non-empty authority.
            { null, "authority", "path", null, null },
            { null, "authority", "path", null, "fragment" },
            { null, "authority", "path", "query", null },
            { null, "authority", "path", "query", "fragment" },
            { "scheme", "authority", "path", "query", "fragment" },

            // Query in opaque URI.
            { "scheme", null, null, "query", null },
            { "scheme", null, null, "query", "fragment" },
            { "scheme", null, "path", "query", null },
            { "scheme", null, "path", "query", "fragment" },
        }) {
            // Set to illegal state and assert failure.
            builder .clear()
                    .scheme(test[0])
                    .authority(test[1])
                    .path(test[2])
                    .query(test[3])
                    .fragment(test[4]);
            try {
                builder.getString();
                fail();
            } catch (URISyntaxException expected) {
                logger.log(FINE, expected.toString(), expected);
            }
            try {
                builder.toString();
                fail();
            } catch (IllegalStateException expected) {
            }
            try {
                builder.getUri();
                fail();
            } catch (URISyntaxException expected) {
            }
            try {
                builder.toUri();
                fail();
            } catch (IllegalStateException expected) {
            }

            // Recover to legal state and assert success.
            builder.uri(URI.create(""));
            assertEquals("", builder.toString());
            assertEquals(URI.create(""), builder.toUri());
        }
    }
}
