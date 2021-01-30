/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;

/**
 * An immutable collection of at most one item.
 * As with any collection, the most idiomatic way to use it is as follows:
 * <pre>{@code
 * Option<String> option = Option.apply("Hello world!"); // or Option.apply(null)
 * for (String string : option) System.out.println(string);
 * }</pre>
 * <p>
 * A less idiomatic way is the following:
 * <pre>{@code
 * Option<String> option = Option.some("Hello world!"); // or Option.none()
 * if (!option.isEmpty()) System.out.println(option.get());
 * }</pre>
 * <p>
 * If you use this class in these ways, your code clearly expresses the
 * intention that its prepared to deal with the absence of an object in a
 * collection and won't throw a {@link NullPointerException} in this case.
 * Here's a more complex example with composed options:
 * <p>
 * <pre>{@code
 * class Container {
 *     Option<String> getMessage() { return Option.some("Hello world!"); }
 * }
 *
 * Option<Container> option = Option.some(new Container()); // or Option.none()
 * for (Container c : option)
 *     for (String s : c.getMessage())
 *         System.out.println(s);
 * }</pre>
 * <p>
 * This class is inspired by the Scala Library and checked with Google's Guava
 * Library.
 * <p>
 * A noteable difference to Guava's {@code Optional} class is that this class
 * is a collection while {@code Optional} is not, so you can't use a for-loop
 * with the latter.
 * <p>
 * A noteable difference to both libraries is that this class doesn't support
 * a generic Function interface.
 * This is because without support for closures in Java 7, using a generic
 * functional interface in Java is not as convenient as the for-loop.
 * <p>
 * This class is immutable. The same constraint must apply to its subclasses.
 *
 * @param  <E> The type of the optional item in this container.
 * @since  TrueCommons 2.4
 * @author Christian Schlichtherle
 */
public abstract class Option<E>
extends AbstractCollection<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    Option() { }

    /**
     * Returns an option for the given nullable item.
     *
     * @param <T> The type of the nullable item.
     * @param item the nullable item.
     * @return An option for the given nullable item.
     */
    public static <T> Option<T> apply(T item) {
        return null != item ? some(item) : Option.<T>none();
    }

    /**
     * Returns an option with the given nullable item.
     *
     * @param <T> the type of the item.
     * @param item the nullable item in this option.
     * @return An option with the given nullable item.
     */
    public static <T> Option<T> some(T item) { return new Some<>(item); }

    /**
     * Returns an option with no item.
     *
     * @param <T> the type of the absent item.
     * @return An option with no item.
     */
    @SuppressWarnings("unchecked")
    public static <T> Option<T> none() { return (Option<T>) None.SINGLETON; }

    /**
     * If present, returns the single item contained in this collection,
     * or otherwise throws an exception.
     *
     * @return The single item in this collection.
     * @throws NoSuchElementException if no item is present in this collection.
     */
    public abstract E get() throws NoSuchElementException;

    /**
     * If present, returns the single item contained in this collection,
     * or otherwise the given alternative.
     *
     * @param alternative the alternative item.
     */
    public abstract E getOrElse(E alternative);

    /**
     * Equivalent to {@link #getOrElse getOrElse(null)}, but probably more
     * efficient.
     */
    public abstract E orNull();

    /**
     * If an item is present in this collection, then this collection is
     * returned, or otherwise the given alternative.
     *
     * @param alternative the alternative option.
     */
    public Option<E> orElse(Option<E> alternative) {
        return isEmpty() ? alternative : this;
    }

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract int hashCode();
}

