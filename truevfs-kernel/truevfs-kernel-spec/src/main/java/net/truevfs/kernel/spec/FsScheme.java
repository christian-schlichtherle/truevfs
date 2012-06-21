/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.spec;

import net.truevfs.kernel.spec.util.UriBuilder;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Locale;
import javax.annotation.concurrent.Immutable;

/**
 * Addresses a file system scheme.
 * This is simply a {@link java.net.URI} scheme according to the syntax
 * constraints defined in
 * <a href="http://www.ietf.org/rfc/rfc2396.txt""><i>RFC&nbsp;2396: Uniform Resource Identifiers (URI): Generic Syntax</i></a>.
 * 
 * <h3><a name="serialization"/>Serialization</h3>
 * <p>
 * This class supports serialization with both
 * {@link java.io.ObjectOutputStream} and {@link java.beans.XMLEncoder}.
 *
 * @see    FsPath
 * @see    FsMountPoint
 * @see    FsEntryName
 * @author Christian Schlichtherle
 */
@Immutable
public final class FsScheme implements Serializable, Comparable<FsScheme> {

    private static final long serialVersionUID = 2765230379628276648L;

    private final String scheme;

    /**
     * Constructs a new URI scheme by parsing the given string.
     * This static factory method calls
     * {@link #FsScheme(String) new FsScheme(scheme)}
     * and wraps any thrown {@link URISyntaxException} in an
     * {@link IllegalArgumentException}.
     *
     * @param  scheme the URI scheme.
     * @throws IllegalArgumentException if {@code scheme} does not conform to
     *         the syntax constraints for URI schemes.
     * @return A new scheme.
     */
    public static FsScheme create(String scheme) {
        try {
            return new FsScheme(scheme);
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
    public FsScheme(final String scheme) throws URISyntaxException {
        UriBuilder.validateScheme(scheme);
        this.scheme = scheme;
    }

    @Override
    public boolean equals(Object that) {
        return this == that
                || that instanceof FsScheme
                    && this.scheme.equalsIgnoreCase(((FsScheme) that).scheme);
    }

    @Override
    public int compareTo(FsScheme that) {
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