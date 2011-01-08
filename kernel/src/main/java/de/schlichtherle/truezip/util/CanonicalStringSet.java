/*
 * Copyright (C) 2007-2010 Schlichtherle IT Services
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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * A set of canonicalized strings in natural sort order.
 * A string is canonicalized by the function {@link Canonicalizer#canonicalize}.
 * <p>
 * String sets can be converted from and to string lists by using
 * {@link #addAll(String)} and {@link #toString()}.
 * A <i>string list</i> is a string which consists of zero or more elements
 * which are separated by the <i>separator character</i> provided to the
 * constructor.
 * Note that in general, a string list is just a sequence of strings elements.
 * In particular, a string list may be empty (but not {@code null}) and
 * its elements don't have to be in canonical form, may be duplicated in the
 * list and may be listed in arbitrary order.
 * However, string lists have a canonical form, too:
 * A string list in canonical form (or <i>canonical string list</i> for short)
 * is a string list which contains only canonical strings in natural sort order
 * and does not contain any duplicates (so it's actually a set).
 * <p>
 * Unless otherwise documented, all {@link java.util.Set} methods work on the
 * canonical form of the string elements in this set.
 * <p>
 * Null elements are not permitted in this set.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class CanonicalStringSet extends AbstractSet<String> {

    /** Maps a string to its canonical form. */
    public interface Canonicalizer {
        /**
         * Returns the canonical form of {@code s} or {@code null} if the
         * given string does not have a canonical form.
         *
         * @param s The string to get canonicalized.
         * @return The canonical form of {@code s} or {@code null} if
         *         {@code s} does not have a canonical form.
         */
        @CheckForNull String canonicalize(@NonNull String s);
    } // interface Canonicalizer

    private final Canonicalizer canonicalizer;

    /** The separator for string lists. */
    private final char separator;

    /** The sorted map which implements the behaviour of this class. */
    private final SortedMap<String, String> map = new TreeMap<String, String>();

    /**
     * Constructs a new, empty set of canonical strings.
     *
     * @param separator The separator character to use in string lists.
     */
    public CanonicalStringSet(final @NonNull Canonicalizer mapper, final char separator) {
        if (null == mapper)
            throw new NullPointerException();
        this.canonicalizer = mapper;
        this.separator = separator;
    }

    /**
     * Constructs a new set of canonical strings from the given set of
     * canonical strings.
     *
     * @param separator The separator character to use in string lists.
     * @param set A set of canonical strings to canonicalize and add to this set.
     */
    public CanonicalStringSet(  final @NonNull Canonicalizer mapper,
                                final char separator,
                                final @NonNull CanonicalStringSet set) {
        if (null == mapper)
            throw new NullPointerException();
        this.canonicalizer = mapper;
        this.separator = separator;
        addAll(set);
    }

    @Override
    public final boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public final int size() {
        return map.size();
    }

    /**
     * Tests if the canonical form of all strings in the given string list
     * is contained in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * This implies that if the list is empty or entirely consists of strings
     * which do not have a canonical form, {@code true} is returned.
     * In other words, an empty set is considered to be a true subset of this
     * set.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff the canonical form of all strings in the
     *         given string list is contained in this set.
     * @throws NullPointerException If {@code list} is {@code null}.
     * @throws ClassCastException If {@code list} is not a {@code String}.
     */
    @Override
    public final boolean contains(Object list) {
        return containsAll((String) list);
    }

    /**
     * Returns a new iterator for all canonical string elements in this set.
     *
     * @return A new iterator for all canonical string elements.
     */
    @Override
    public final Iterator<String> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns a new iterator for all original string elements in this set.
     * Note that strings which don't have a canonical form cannot get added
     * to this class and hence cannot get returned by the iterator.
     *
     * @return A new iterator for all original string elements.
     */
    public final Iterator<String> originalIterator() {
        return map.values().iterator();
    }

    @Override
    public final Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public final <T> T[] toArray(T[] array) {
        return map.keySet().toArray(array);
    }

    //
    // Modification operations.
    //

    /**
     * Adds the canonical form of all strings in the given list to this set.
     * If a string in the list does not have a canonical form or its canonical
     * form is already contained in this set, it's ignored.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff this set changed as a result of the call.
     * @throws NullPointerException If {@code list} is {@code null}.
     * @throws ClassCastException If {@code list} is not a {@code String}.
     */
    @Override
    public final boolean add(String list) {
        return addAll(list);
    }

    /**
     * Removes the canonical form of all strings in the given list from this set.
     * If a string in the list does not have a canonical form, it's ignored.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff this set changed as a result of the call.
     * @throws NullPointerException If {@code list} is {@code null}.
     * @throws ClassCastException If {@code list} is not a {@code String}.
     */
    @Override
    public final boolean remove(Object list) {
        return removeAll((String) list);
    }

    //
    // Bulk operations.
    //

    /**
     * Tests if all canonical strings in the given set are contained in this
     * set.
     * An empty set is considered to be a true subset of this set.
     *
     * @param set A non-null set of canonical strings.
     * @return {@code true} Iff all strings in the given set are contained
     *         in this set.
     * @throws NullPointerException If {@code set} is {@code null}.
     */
    public final boolean containsAll(final CanonicalStringSet set) {
        return map.keySet().containsAll(set.map.keySet());
    }

    /**
     * Tests if the canonical form of all strings in the given string list
     * is contained in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     * This implies that if the list is empty or entirely consists of strings
     * which do not have a canonical form, {@code true} is returned.
     * In other words, an empty set is considered to be a true subset of this
     * set.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff the canonical form of all strings in the
     *         given string list is contained in this set.
     * @throws NullPointerException If {@code list} is {@code null}.
     */
    public final boolean containsAll(final String list) {
        final Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            if (!map.containsKey(i.next()))
                return false;
        return true;
    }

    /**
     * Adds all canonical strings in the given set to this set after they have
     * been canonicalized by this set again.
     *
     * @param set A non-null set of canonical strings.
     * @return {@code true} Iff this set of canonicalized strings has
     *         changed as a result of the call.
     * @throws NullPointerException If {@code set} is {@code null}.
     */
    public final boolean addAll(final CanonicalStringSet set) {
        boolean changed = false;
        for (String s : set.map.values())
            changed |= add(s);
        return changed;
    }

    /**
     * Adds the canonical form of all strings in the given list to this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff this set of canonicalized strings has
     *         changed as a result of the call.
     * @throws NullPointerException If {@code list} is {@code null}.
     */
    public final boolean addAll(final String list) {
        boolean changed = false;
        for (final Iterator<String> i = new StringIterator(list); i.hasNext(); ) {
            final String element = i.next();
            final String canonical = canonicalizer.canonicalize(element);
            if (null != canonical)
                changed |= null == map.put(canonical, element);
        }
        return changed;
    }

    /**
     * Retains all canonical strings in the given set in this set.
     *
     * @param set A non-null set of canonical strings.
     * @return {@code true} Iff this set changed as a result of the call.
     * @throws NullPointerException If {@code set} is {@code null}.
     */
    public final boolean retainAll(CanonicalStringSet set) {
        return map.keySet().retainAll(set.map.keySet());
    }

    /**
     * Retains the canonical form of all strings in the given list in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff this set changed as a result of the call.
     * @throws NullPointerException If {@code list} is {@code null}.
     */
    public final boolean retainAll(final String list) {
        final CanonicalStringSet set = new CanonicalStringSet(canonicalizer, separator);
        set.addAll(list);
        return map.keySet().retainAll(set);
    }

    /**
     * Removes all canonical strings in the given set from this set.
     *
     * @param set A non-null set of strings.
     * @return {@code true} Iff this set changed as a result of the call.
     * @throws NullPointerException If {@code set} is {@code null}.
     */
    public final boolean removeAll(CanonicalStringSet set) {
        return map.keySet().removeAll(set.map.keySet());
    }

    /**
     * Removes the canonical form of all strings in the given list from this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param list A non-null string list.
     * @return {@code true} Iff this set changed as a result of the call.
     * @throws NullPointerException If {@code list} is {@code null}.
     */
    public final boolean removeAll(final String list) {
        boolean changed = false;
        for (final Iterator<String> i = new CanonicalStringIterator(list); i.hasNext(); )
            changed |= map.remove(i.next()) != null;
        return changed;
    }

    @Override
    public final void clear() {
        map.clear();
    }

    //
    // Miscellaneous.
    //

    /**
     * Returns the string representation of this canonical string set.
     * If this canonical string set is empty, an empty string is returned.
     */
    @Override
    public final String toString() {
        final Iterator<String> i = iterator();
        if (i.hasNext()) {
            final StringBuilder sb = new StringBuilder();
            int c = 0;
            do {
                final String string = i.next();
                if (c++ > 0)
                    sb.append(separator);
                sb.append(string);
            } while (i.hasNext());
            return sb.toString();
        } else {
            return "";
        }
    }

    //
    // Inner classes.
    //

    private class CanonicalStringIterator implements Iterator<String> {
        private final Iterator<String> i;
        private String canonical;

        private CanonicalStringIterator(final String list) {
            i = new StringIterator(list);
            advance();
        }

        @Override
		public boolean hasNext() {
            return canonical != null;
        }

        @Override
		public String next() {
            if (canonical == null)
                throw new NoSuchElementException();
            final String c = canonical;
            advance();
            return c;
        }

        private void advance() {
            while (i.hasNext()) {
                canonical = canonicalizer.canonicalize(i.next());
                if (null != canonical)
                    return;
            }
            canonical = null; // no such element
        }

        @Override
		public void remove() {
            throw new UnsupportedOperationException();
        }
    } // class CanonicalSuffixIterator

    private class StringIterator implements Iterator<String> {
        private final StringTokenizer i;

        private StringIterator(final String list) {
            i = new StringTokenizer(list, "" + separator); // NOI18N
        }

        @Override
		public boolean hasNext() {
            return i.hasMoreTokens();
        }

        @Override
		public String next() {
            return i.nextToken();
        }

        @Override
		public void remove() {
            throw new UnsupportedOperationException();
        }
    } // class StringIterator
}
