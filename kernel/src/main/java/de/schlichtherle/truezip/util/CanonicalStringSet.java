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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * A set of the canonical string representation of objects in natural sort
 * order.
 * An object is canonicalized by the idempotent function
 * {@link Canonicalizer#map}.
 * <p>
 * Canonical string sets can be converted from and to string lists by using
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
@DefaultAnnotation(NonNull.class)
public class CanonicalStringSet extends AbstractSet<String> {

    /** Maps an object to its canonical string representation. */
    public interface Canonicalizer extends IdemPotence<String> {

        /**
         * Returns the canonical string representation of {@code o} or
         * {@code null} if the canonical string representation is undefined.
         *
         * @param  o The Object to map to its canonical string representation.
         * @return The canonical string representation of {@code o} or
         *         {@code null} if the canonical string representation is
         *         undefined.
         */
        @Override
        @CheckForNull String map(@Nullable Object o);
    } // interface Canonicalizer

    /** The canonicalizer for strings. */
    private final Canonicalizer canonicalizer;

    /** The separator for string lists. */
    private final char separator;

    /** The sorted map which implements the behaviour of this class. */
    private final Set<String> set = new TreeSet<String>();

    /**
     * Constructs a new, empty set of canonical strings.
     *
     * @param canonicalizer the idempotent function to use for canonicalizing
     *        strings.
     * @param separator The separator character to use for string lists.
     */
    public CanonicalStringSet(  final Canonicalizer canonicalizer,
                                final char separator) {
        if (null == canonicalizer)
            throw new NullPointerException();
        this.canonicalizer = canonicalizer;
        this.separator = separator;
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Iterator<String> iterator() {
        return set.iterator();
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return set.toArray(array);
    }

    /**
     * Returns the string list in canonical form for this canonical string set.
     * If this canonical string set is empty, an empty string is returned.
     */
    @Override
    public String toString() {
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

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link CanonicalStringSet} first
     * canonicalizes the given parameter before the operation is continued.
     */
    @Override
    public boolean contains(@Nullable Object o) {
        return set.contains(canonicalizer.map(o));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link CanonicalStringSet} first
     * canonicalizes the given parameter before the operation is continued.
     */
    @Override
    public boolean add(@Nullable String s) {
        return set.add(canonicalizer.map(s));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link CanonicalStringSet} first
     * canonicalizes the given parameter before the operation is continued.
     */
    @Override
    public boolean remove(@Nullable Object o) {
        return set.remove(canonicalizer.map(o));
    }

    @Override
    public void clear() {
        set.clear();
    }

    /**
     * Tests if all canonical strings in the given set are contained in this
     * set.
     * An empty set is considered to be a true subset of this set.
     *
     * @param set A non-null set of canonical strings.
     * @return {@code true} Iff all strings in the given set are contained
     *         in this set.
     */
    public boolean containsAll(final CanonicalStringSet set) {
        return this.set.containsAll(set.set);
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
     * @param  list a non-null string list.
     * @return {@code true} Iff the canonical form of all strings in the
     *         given string list is contained in this set.
     */
    public boolean containsAll(final String list) {
        Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            if (!set.contains(i.next()))
                return false;
        return true;
    }

    /**
     * Adds all canonical strings in the given set to this set after they have
     * been canonicalized by this set again.
     *
     * @param  set a non-null set of canonical strings.
     * @return {@code true} Iff this set of canonicalized strings has
     *         changed as a result of the call.
     */
    public boolean addAll(final CanonicalStringSet set) {
        boolean changed = false;
        for (String s : set.set)
            changed |= add(s);
        return changed;
    }

    /**
     * Adds the canonical form of all strings in the given list to this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param  list a non-null string list.
     * @return {@code true} Iff this set of canonicalized strings has
     *         changed as a result of the call.
     */
    public boolean addAll(final String list) {
        boolean changed = false;
        Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            changed |= set.add(i.next());
        return changed;
    }

    /**
     * Retains all canonical strings in the given set in this set.
     *
     * @param  set a non-null set of canonical strings.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean retainAll(CanonicalStringSet set) {
        return this.set.retainAll(set.set);
    }

    /**
     * Retains the canonical form of all strings in the given list in this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param  list a non-null string list.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean retainAll(final String list) {
        CanonicalStringSet set = new CanonicalStringSet(canonicalizer, separator);
        set.addAll(list);
        return set.retainAll(set);
    }

    /**
     * Removes all canonical strings in the given set from this set.
     *
     * @param  set a non-null set of strings.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean removeAll(CanonicalStringSet set) {
        return this.set.removeAll(set.set);
    }

    /**
     * Removes the canonical form of all strings in the given list from this set.
     * If a string in the list does not have a canonical form, it's skipped.
     *
     * @param  list a non-null string list.
     * @return {@code true} Iff this set changed as a result of the call.
     */
    public boolean removeAll(final String list) {
        boolean changed = false;
        Iterator<String> i = new CanonicalStringIterator(list);
        while (i.hasNext())
            changed |= set.remove(i.next());
        return changed;
    }

    private class CanonicalStringIterator implements Iterator<String> {
        private final StringTokenizer i;
        private @CheckForNull String canonical;

        private CanonicalStringIterator(final String list) {
            i = new StringTokenizer(list, "" + separator); // NOI18N
            advance();
        }

        @Override
        public boolean hasNext() {
            return canonical != null;
        }

        @Override
        public String next() {
            if (null == canonical)
                throw new NoSuchElementException();
            final String c = canonical;
            advance();
            return c;
        }

        private void advance() {
            while (i.hasMoreTokens()) {
                canonical = canonicalizer.map(i.nextToken());
                if (null != canonical)
                    return;
            }
            canonical = null; // no such element
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    } // class CanonicalStringIterator
}
