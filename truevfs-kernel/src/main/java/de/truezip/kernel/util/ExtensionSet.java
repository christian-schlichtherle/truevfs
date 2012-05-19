/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * An ordered set of canonicalized extensions.
 * An {@code ExtensionSet} can be converted into a string representation by
 * calling {@link #toString()}.
 * <p>
 * An <i>extension</i> is the part of a file name string after the last dot.
 * It must not contain the character {@code '|'}.
 * An extension in canonical form (or <i>canonical extension</i> for short) is
 * a lowercase string which is not empty and does <em>not</em> start with a
 * dot ({@code '.'}).
 * <p>
 * For example, the extension {@code "zip"} is in canonical form,
 * while the extensions
 * {@code ""},
 * {@code "Zip"},
 * {@code "ZIP"},
 * {@code ".zip"},
 * {@code ".Zip"},
 * {@code ".ZIP"}, and
 * {@code "zip|Zip|ZIP|.zip|.Zip|.ZIP"} aren't.
 * <p>
 * An <i>extension list</i> is a string which consists of zero or more
 * extensions which are separated by the character {@code '|'}.
 * Note that in general, an extension list is just a sequence of extensions.
 * In particular, an extension list may be empty (but not {@code null}) and
 * its extensions don't have to be in canonical form, may be duplicated in the
 * list and may appear in arbitrary order.
 * <p>
 * Extension lists have a canonical form, too:
 * An extension list in canonical form (or <i>canonical extension list</i> for
 * short) is an extension list which contains only canonical extensions in
 * natural sort order and does not contain any duplicates (so it's actually a
 * set).
 * <p>
 * An extension list can be canonicalized using this class by calling
 * {@link #ExtensionSet(String) new ExtensionSet(list)}.{@link #toString toString()}.
 * <p>
 * Unless otherwise documented, all {@link java.util.Set} methods work on the
 * canonical form of the extensions in this set.
 * <p>
 * Null extensions are <em>not</em> permitted in this set.
 *
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class ExtensionSet extends CanonicalStringSet {

    /** A canonicalizer for file extensions. */
    private static class ExtensionMapper implements Canonicalizer {
        /**
         * Returns the canonical form of {@code extension} or {@code null}
         * if the given extension does not have a canonical form.
         * An example of the latter case is the empty string.
         */
        @Override
        public @CheckForNull String map(Object o) {
            String extension = o.toString();
            while (0 < extension.length() && extension.charAt(0) == PREFIX)
                extension = extension.substring(1);
            return extension.isEmpty() ? null : extension.toLowerCase(Locale.ENGLISH);
        }
    } // ExtensionMapper

    /** The separator for extensions in lists, which is {@value}. */
    public static final char SEPARATOR = '|';

    /** The optional prefix for extensions, which is {@value}. */
    public static final char PREFIX = '.';

    /** Constructs a new, empty extension set. */
    public ExtensionSet() {
        super(new ExtensionMapper(), SEPARATOR);
    }

    /**
     * Constructs a new extension set from the given extension list.
     *
     * @param extensions A list of extensions.
     */
    public ExtensionSet(final String extensions) {
        super(new ExtensionMapper(), SEPARATOR);
        super.addAll(extensions);
    }

    /**
     * Constructs a new extension set by adding the canonical form of all
     * extensions for all extension lists in the given collection.
     *
     * @param  c A collection of extension lists.
     */
    public ExtensionSet(final Collection<String> c) {
        super(new ExtensionMapper(), SEPARATOR);
        super.addAll(c);
    }

    /**
     * Returns a case insensitive regular expression to match (file) paths
     * against the extensions in this set.
     * If the regular expression matches, the matching extension is captured as
     * the first matching group.
     * If this extension set is empty, an unmatchable expression is returned.
     */
    public Pattern toPattern() {
        final Iterator<String> i = iterator();
        if (i.hasNext()) {
            final StringBuilder sb = new StringBuilder(".*\\.(?i)("); // NOI18N
            int c = 0;
            do {
                final String extension = i.next();
                if (0 < c++)
                    sb.append('|'); // not SEPARATOR !!!
                sb.append("\\Q").append(extension).append("\\E"); // NOI18N
            } while (i.hasNext());
            assert 0 < c;
            return Pattern.compile(
                    sb  .append(")[\\")
                        .append(File.separatorChar)
                        .append("/]*")
                        .toString());
        } else {
            return Pattern.compile("\\00"); // NOT "\00"! Effectively never matches anything. // NOI18N
        }
    }
}