/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

/**
 * @author Christian Schlichtherle
 */
public class UriBuilderTest {

    private UriBuilder builder;

    @Before
    public void setUp() {
        builder = new UriBuilder();
    }

    @Test
    public void testDefaults() {
        assertEquals("", builder.toStringUnchecked());
        assertEquals(URI.create(""), builder.toUriUnchecked());
    }

    @Test
    public void testClear() {
        assertSame(builder,
                builder.uri(URI.create("scheme://authority/path?query#fragment"))
                        .scheme(null)
                        .authority(null)
                        .path(null)
                        .query(null)
                        .fragment(null));
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
            { "föö%20bär", null, null, "föö bär", null, null },
            { "foo:bär", "foo", null, "bär", null, null },
            // See http://java.net/jira/browse/TRUEZIP-180
            { "Dichtheitsprüfung%3A%C2%A0Moenikes%20lässt%20Dampf%20ab", null, null, "Dichtheitsprüfung:\u00a0Moenikes lässt Dampf ab", null, null },
        }) {
            final URI u = URI.create(test[0]);

            // Test parsing.
            builder = new UriBuilder();
            builder.uri(u);
            assertEquals(test[0], builder.toStringUnchecked());

            // Test composition.
            builder = new UriBuilder();
            builder .scheme(test[1])
                    .authority(test[2])
                    .path(test[3])
                    .query(test[4])
                    .fragment(test[5]);
            assertEquals(test[0], builder.toStringUnchecked());
            
            // Test identities.
            assertEquals(u, builder.toUriUnchecked());
            assertEquals(u, builder.uri(u).toUriUnchecked());
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
            builder = new UriBuilder();
            builder .scheme(test[0])
                    .authority(test[1])
                    .path(test[2])
                    .query(test[3])
                    .fragment(test[4]);
            try {
                builder.toStringChecked();
                fail();
            } catch (URISyntaxException expected) {
            }
            try {
                builder.toStringUnchecked();
                fail();
            } catch (IllegalStateException expected) {
            }
            try {
                builder.toUriChecked();
                fail();
            } catch (URISyntaxException expected) {
            }
            try {
                builder.toUriUnchecked();
                fail();
            } catch (IllegalStateException expected) {
            }

            // Recover to legal state and assert success.
            builder.uri(URI.create(""));
            assertEquals("", builder.toStringUnchecked());
            assertEquals(URI.create(""), builder.toUriUnchecked());
        }
    }
}
