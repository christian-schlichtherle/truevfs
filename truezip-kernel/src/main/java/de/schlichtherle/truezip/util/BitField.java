/*
 * Copyright 2007-2011 Schlichtherle IT Services
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
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import net.jcip.annotations.Immutable;

/**
 * A type-safe, immutable set of enums which emulates the concept of a bit
 * field, i.e. a set of predefined bits.
 * As an immutable class, it's inherently thread-safe.
 * All modifying methods return a modified clone of this instance.
 * <p>
 * In general, passing {@code null} as a method parameter results in a
 * {@link NullPointerException} and methods return non-{@code null}.
 * <p>
 * <b>TODO:</b> Add more modifying methods.
 *
 * @param   <E> The type of {@link Enum} objects contained in this set.
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@DefaultAnnotation(NonNull.class)
@Immutable
public final class BitField<E extends Enum<E>>
implements Iterable<E>, Serializable {

    private static final long serialVersionUID = 3203876204846746524L;

    private final EnumSet<E> bits;

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
    public static <E extends Enum<E>> BitField<E>
    noneOf(Class<E> elementType) {
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
    public static <E extends Enum<E>> BitField<E>
    allOf(Class<E> elementType) {
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
    public static <E extends Enum<E>> BitField<E>
    of(E bit) {
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
    public static <E extends Enum<E>> BitField<E>
    of(E bit, E... bits) {
        return new BitField<E>(bit, bits);
    }

    /**
     * Returns a bit field which contains the same bits as the given set of
     * enums.
     * <p>
     * This could be used like this:
     * <pre>{@code
     *  BitField<Option> field = BitField.of(bits);
     * }</pre>
     * where {@code bits} is an {@code EnumSet<Option>}.
     */
    public static <E extends Enum<E>> BitField<E>
    of(EnumSet<E> bits) {
        return new BitField<E>(bits.clone());
    }

    public static <E extends Enum<E>> BitField<E>
    of(final Class<E> elementType, final String list) {
        final EnumSet<E> bits = EnumSet.noneOf(elementType);
        for (final String bit : list.split("\\|"))
            bits.add(Enum.valueOf(elementType, bit));
        return new BitField<E>(bits);
    }

    /**
     * Constructs a new bit field which contains all or none of the enums
     * of the given element type.
     */
    private BitField(Class<E> elementType, boolean allOf) {
        this.bits = allOf   ? EnumSet.allOf (elementType)
                            : EnumSet.noneOf(elementType);
    }

    /** Constructs a new bit field which contains the given bit. */
    private BitField(E bit) {
        this.bits = EnumSet.of(bit);
    }

    /** Constructs a new bit field which contains the given bits. */
    private BitField(E bit, E... bits) {
        this.bits = EnumSet.of(bit, bits);
    }

    /**
     * Constructs a new bit field by sharing the given set of enums.
     * Note that this constructor does NOT make a protective copy
     * - use with care!
     *
     * @param bits the set of enums to share with this instance.
     */
    private BitField(final EnumSet<E> bits) {
        this.bits = bits;
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
    public boolean is(E bit) {
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
        final EnumSet<E> bits;
        if (set) {
            if (this.bits.contains(bit))
                return this;
            bits = this.bits.clone();
            bits.add(bit);
        } else {
            if (!this.bits.contains(bit))
                return this;
            bits = this.bits.clone();
            bits.remove(bit);
        }
        return new BitField<E>(bits);
    }

    /** Sets the given bit. */
    public BitField<E> set(E bit) {
        return set(bit, true);
    }

    /** Clears the given bit. */
    public BitField<E> clear(E bit) {
        return set(bit, false);
    }

    public BitField<E> not() {
        return new BitField<E>(EnumSet.complementOf(bits));
    }

    public BitField<E> and(BitField<E> that) {
        final EnumSet<E> bits = this.bits.clone();
        return bits.retainAll(that.bits) ? new BitField<E>(bits) : this;
    }

    public BitField<E> or(BitField<E> that) {
        final EnumSet<E> bits = this.bits.clone();
        return bits.addAll(that.bits) ? new BitField<E>(bits) : this;
    }

    /** Returns a read-only iterator for the bits in this field. */
    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableSet(bits).iterator();
    }

    /**
     * Returns a new set of enums containing the same bits as this instance.
     * The following boolean expression is always true for any
     * non-{@code null} bit field {@code bits}:
     * {@code bits.equals(BitField.of(bits.toEnumSet()))}.
     *
     * @return a new set of enums containing the same bits as this instance.
     */
    public EnumSet<E> toEnumSet() {
        return bits.clone();
    }

    /**
     * Returns a concatenation of the names of the bits in this field,
     * separated by {@code "|"}.
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
    public boolean equals(@CheckForNull Object that) {
        return this == that
                || that instanceof BitField<?>
                    && bits.equals(((BitField<?>) that).bits);
    }

    /** Returns a hash code which is consistent with {@link #equals}. */
    @Override
    public int hashCode() {
        return bits.hashCode();
    }
}
