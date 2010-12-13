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

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Represents a {@link java.net.URI} scheme according to the syntax constraints
 * defined in <a href="http://www.ietf.org/rfc/rfc2396.txt""><i>RFC&nbsp;2396:
 * Uniform Resource Identifiers (URI): Generic Syntax</i></a>.
 */
public final class Scheme implements Serializable, Comparable<Scheme> {

    private static final long serialVersionUID = 2765230379628276648L;

    /** Represents the "file" URI scheme. */
    public static final Scheme FILE = Scheme.create("file");

    private final String scheme;

    /**
     * Constructs a new URI scheme by parsing the given string.
     * This static factory method calls
     * {@link #Scheme(String) new Scheme(scheme)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the non-{@code null} URI scheme.
     * @throws NullPointerException if {@code scheme} is {@code null}.
     * @throws IllegalArgumentException if {@code scheme} does not conform to
     *         the syntax constraints for URI schemes.
     * @return A non-{@code null} path.
     */
    public static Scheme create(String scheme) {
        try {
            return new Scheme(scheme);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new URI scheme by parsing the given string.
     *
     * @param  scheme the non-{@code null} URI scheme.
     * @throws NullPointerException if {@code scheme} is {@code null}.
     * @throws URISyntaxException if {@code scheme} does not conform to the
     *         syntax constraints for URI schemes.
     */
    public Scheme(final String scheme) throws URISyntaxException {
        int i = scheme.length();
        if (0 >= i) {
            throw new URISyntaxException(scheme, "Empty URI scheme");
        }
        char c;
        c = scheme.charAt(0);
        // TODO: Character class is no help here - consider table lookup!
        if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c)) {
            throw new URISyntaxException(scheme, "Illegal character in URI scheme", 0);
        }
        while (--i >= 1) {
            c = scheme.charAt(i);
            if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c) && c != '+' && c != '-' && c != '.') {
                throw new URISyntaxException(scheme, "Illegal character in URI scheme", i);
            }
        }
        this.scheme = scheme;
    }

    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof Scheme
                    && this.scheme.equalsIgnoreCase(((Scheme) that).scheme);
    }

    @Override
    public int compareTo(Scheme that) {
        return this.scheme.compareToIgnoreCase(that.scheme);
    }

    @Override
    public int hashCode() {
        return scheme.toLowerCase(Locale.ENGLISH).hashCode();
    }

    @Override
    public String toString() {
        return scheme;
    }
}
