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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Locale;
import net.jcip.annotations.Immutable;

/**
 * Represents a {@link java.net.URI} scheme according to the syntax constraints
 * defined in <a href="http://www.ietf.org/rfc/rfc2396.txt""><i>RFC&nbsp;2396:
 * Uniform Resource Identifiers (URI): Generic Syntax</i></a>.
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@Immutable
public final class FSScheme implements Serializable, Comparable<FSScheme> {

    private static final long serialVersionUID = 2765230379628276648L;

    private final String scheme;

    /**
     * Constructs a new URI scheme by parsing the given string.
     * This static factory method calls
     * {@link #FSScheme(String) new FSScheme(scheme)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the URI scheme.
     * @throws IllegalArgumentException if {@code scheme} does not conform to
     *         the syntax constraints for URI schemes.
     * @return A new scheme.
     */
    public static @NonNull FSScheme create(String scheme) {
        try {
            return new FSScheme(scheme);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Constructs a new URI scheme by parsing the given string.
     *
     * @param  scheme the URI scheme.
     * @throws URISyntaxException if {@code scheme} does not conform to the
     *         syntax constraints for URI schemes.
     */
    public FSScheme(final @NonNull String scheme) throws URISyntaxException {
        int i = scheme.length();
        if (0 >= i) {
            throw new URISyntaxException(quote(scheme), "Empty URI scheme");
        }
        char c;
        c = scheme.charAt(0);
        // TODO: Character class is no help here - consider table lookup!
        if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c)) {
            throw new URISyntaxException(quote(scheme), "Illegal character in URI scheme", 0);
        }
        while (--i >= 1) {
            c = scheme.charAt(i);
            if ((c < 'a' || 'z' < c) && (c < 'A' || 'Z' < c)
                    && (c < '0' || '9' < c)
                    && c != '+' && c != '-' && c != '.') {
                throw new URISyntaxException(quote(scheme), "Illegal character in URI scheme", i);
            }
        }
        this.scheme = scheme;
    }

    private static String quote(Object s) {
        return "\"" + s + "\"";
    }

    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof FSScheme
                    && this.scheme.equalsIgnoreCase(((FSScheme) that).scheme);
    }

    @Override
    public int compareTo(FSScheme that) {
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
