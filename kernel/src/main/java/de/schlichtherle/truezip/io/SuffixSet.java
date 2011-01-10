/*
 * Copyright (C) 2005-2010 Schlichtherle IT Services
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
package de.schlichtherle.truezip.io;

import de.schlichtherle.truezip.util.CanonicalStringSet;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * An ordered set of canonicalized suffixes.
 * A {@code SuffixSet} can be converted into a string representation by
 * calling {@link #toString()}.
 * <p>
 * A <i>suffix</i> is the part of a file name string after the last dot.
 * It must not contain the character {@code '|'}.
 * A suffix in canonical form (or <i>canonical suffix</i> for short) is a
 * lowercase string which is not empty and does <em>not</em> start with a
 * dot ({@code '.'}).
 * <p>
 * For example, the suffix {@code "zip"} is in canonical form,
 * while the suffixes
 * {@code ""},
 * {@code "Zip"},
 * {@code "ZIP"},
 * {@code ".zip"},
 * {@code ".Zip"},
 * {@code ".ZIP"}, and
 * {@code "zip|Zip|ZIP|.zip|.Zip|.ZIP"} aren't.
 * <p>
 * A <i>suffix list</i> is a string which consists of zero or more suffixes
 * which are separated by the character {@code '|'}.
 * Note that in general, a suffix list is just a sequence of suffixes.
 * In particular, a suffix list may be empty (but not {@code null}) and
 * its suffixes don't have to be in canonical form, may be duplicated in the
 * list and may appear in arbitrary order.
 * <p>
 * Suffix lists have a canonical form, too:
 * A suffix list in canonical form (or <i>canonical suffix list</i> for short)
 * is a suffix list which contains only canonical suffixes in natural sort
 * order and does not contain any duplicates (so it's actually a set).
 * <p>
 * A suffix list can be canonicalized using this class by calling
 * {@code new {@link #SuffixSet(String) SuffixSet(list)}.{@link #toString()}}.
 * <p>
 * Unless otherwise documented, all {@link java.util.Set} methods work on the
 * canonical form of the suffixes in this set.
 * <p>
 * Null suffixes are not permitted in this set.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public final class SuffixSet extends CanonicalStringSet {

    /** The separator for suffixes in lists. */
    public static final char SEPARATOR = '|';

    /** The optional prefix for suffixes. */
    private static final char PREFIX = '.';

    /** Constructs a new, empty suffix set. */
    public SuffixSet() {
        super(new SuffixMapper(), SEPARATOR);
    }

    /**
     * Constructs a new suffix set from the given suffix list.
     *
     * @param suffixes A list of suffixes.
     */
    public SuffixSet(final @NonNull String suffixes) {
        super(new SuffixMapper(), SEPARATOR);
        super.addAll(suffixes);
    }

    /**
     * Constructs a new suffix set by adding the canonical form of all suffixes
     * for all suffix lists in the given collection.
     *
     * @param  c A collection of suffix lists.
     */
    public SuffixSet(final @NonNull Collection<String> c) {
        super(new SuffixMapper(), SEPARATOR);
        super.addAll(c);
    }

    private static class SuffixMapper implements Canonicalizer {
        /**
         * Returns the canonical form of {@code suffix} or {@code null}
         * if the given suffix does not have a canonical form.
         * An example of the latter case is the empty string.
         */
        @Override
        public String toString(Object o) {
            String suffix = o.toString();
            assert 0 > suffix.indexOf(SEPARATOR) : "illegal separator position in suffix";
            if (suffix.length() > 0 && suffix.charAt(0) == PREFIX)
                suffix = suffix.substring(1);
            return suffix.length() > 0
                    ? suffix.toLowerCase(Locale.ENGLISH)
                    : null;
        }
    }

    /**
     * Returns a case insensitive regular expression to match (file) paths
     * against the suffixes in this set.
     * If the regular expression matches, the matching suffix is captured as
     * the first matching group.
     * If this suffix set is empty, an unmatchable expression is returned.
     */
    public Pattern toPattern() {
        final Iterator<String> i = iterator();
        if (i.hasNext()) {
            final StringBuilder sb = new StringBuilder(".*\\.(?i)("); // NOI18N
            int c = 0;
            do {
                final String suffix = i.next();
                if (0 < c++)
                    sb.append('|'); // not SEPARATOR !!!
                sb.append("\\Q").append(suffix).append("\\E"); // NOI18N
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
