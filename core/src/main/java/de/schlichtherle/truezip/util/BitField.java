/*
 * Copyright 2007-2010 Schlichtherle IT Services
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

import java.util.EnumSet;
import java.util.Iterator;

/**
 * A type-safe, immutable set of enums which emulates the concept of a bit
 * field, i.e. a set of predefined bits.
 * As an immutable class, it's inherently thread-safe.
 * All modifying methods return a modified clone of this instance.
 * <p>
 * In general, passing {@code null} as a method parameter results in a
 * {@link NullPointerException}.
 * <p>
 * Subclasses could override the {@link #set(Enum, boolean)} and
 * {@link #clear()} methods in order to cache frequently used results, such
 * as a null bit field, single bit fields etc.
 * <p>
 * <b>TODO:</b> Add more modifying methods.
 *
 * @param <E> The type of {@link Enum} objects contained in this set.
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class BitField<E extends Enum<E>> implements Iterable<E>, Cloneable {

    /**
     * Returns a bit field which can contain the given element type and is
     * initially empty.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.noneOf(Option.class);
     * }</pre>
     * where {@code Option} is an arbitrary enum type.
     */
    public static <E extends Enum<E>>
    BitField<E> noneOf(Class<E> elementType) {
        return new BitField<E>(elementType, false);
    }

    /**
     * Returns a bit field which contains all enums of the given element type.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.allOf(Option.class);
     * }</pre>
     * where {@code Option} is an arbitrary enum type.
     */
    public static <E extends Enum<E>>
    BitField<E> allOf(Class<E> elementType) {
        return new BitField<E>(elementType, true);
    }

    /**
     * Returns a bit field which contains the given bit.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.of(Option.ONE);
     * }</pre>
     * where {@code Option.ONE} is an arbitrary enum.
     */
    public static <E extends Enum<E>>
    BitField<E> of(E bit) {
        return new BitField<E>(bit);
    }

    /**
     * Returns a bit field which contains the given bits.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.of(Option.ONE, Option.TWO);
     * }</pre>
     * where {@code Option.ONE} and {@code Option.TWO} are arbitrary enums.
     */
    public static <E extends Enum<E>>
    BitField<E> of(E bit, E... bits) {
        return new BitField<E>(bit, bits);
    }

    private EnumSet<E> bits;

    /**
     * Constructs a new bit field which contains all or none of the enums
     * of the given element type.
     */
    protected BitField(final Class<E> elementType, final boolean allOf) {
        this.bits = allOf   ? EnumSet.allOf (elementType)
                            : EnumSet.noneOf(elementType);
    }

    /** Constructs a new bit field which contains the given bit. */
    protected BitField(final E bit) {
        this.bits = EnumSet.of(bit);
    }

    /** Constructs a new bit field which contains the given bits. */
    protected BitField(final E bit, final E... bits) {
        this.bits = EnumSet.of(bit, bits);
    }

    /** Returns a clone of this bit field. */
    @Override
    public BitField<E> clone() {
        try {
            final BitField<E> clone = (BitField<E>) super.clone();
            clone.bits = bits.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Returns {@code true} if and only if all bits are cleared in this bit
     * field.
     */
    public boolean isEmpty() {
        return bits.isEmpty();
    }

    /** Returns the number of bits set in this field. */
    public int cardinality() {
        return bits.size();
    }

    /**
     * Returns {@code true} if and only if the given bit is set.
     *
     * @param bit The bit to test.
     */
    public boolean get(E bit) {
        return bits.contains(bit);
    }

    /** Equivalent to {@link #get(Enum) get(bit)}. */
    public final boolean is(E bit) {
        return get(bit);
    }

    /**
     * Sets or clears the given bit.
     * <p>
     * Subclasses could override this method in order to cache frequently used
     * results, such as a null bit field, a single bit field etc.
     *
     * @param bit The bit to set or clear.
     * @param set Whether the bit shall get set or cleared.
     */
    public BitField<E> set(final E bit, final boolean set) {
        final BitField<E> clone;
        if (set) {
            if (bits.contains(bit))
                return (BitField<E>) this;
            clone = clone();
            clone.bits.add(bit);
        } else {
            if (!bits.contains(bit))
                return (BitField<E>) this;
            clone = clone();
            clone.bits.remove(bit);
        }
        return clone;
    }

    /**
     * Clears all bits.
     * <p>
     * Subclasses could override this method in order to return a cached null
     * bit field.
     */
    public BitField<E> clear() {
        final BitField<E> clone;
        clone = clone();
        clone.bits.clear();
        return clone;
    }

    /** Sets the given bit. */
    public final BitField<E> set(E bit) {
        return set(bit, true);
    }

    /** Clears the given bit. */
    public final BitField<E> clear(E bit) {
        return set(bit, false);
    }

    /** Returns a read-only iterator for the bits in this field. */
    @Override
    public Iterator<E> iterator() {
        return new BitFieldIterator();
    }

    /**
     * Returns a concatenation of the names of the bits in this field,
     * separated by a &quot;|&quot;.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final E bit : bits) {
            if (sb.length() > 0)
                sb.append('|');
            sb.append(bit);
        }
        return sb.toString();
    }

    /**
     * Returns {@code true} if and only if the given object is another
     * {@code BitField} and contains the same bits.
     */
    @Override
    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BitField))
            return false;
        return bits.equals(((BitField) o).bits);
    }

    /** Returns a hash code which is consistent with {@link #equals}. */
    @Override
    public final int hashCode() {
        return bits.hashCode();
    }

    private final class BitFieldIterator implements Iterator<E> {
        private final Iterator<E> i = bits.iterator();

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public E next() {
            return i.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("BitField class is immutable");
        }
    }
}
