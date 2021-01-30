/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.shed;

import java.lang.ref.WeakReference;
import java.util.NoSuchElementException;

/**
 * An inheritable thread local stack of items.
 * This class works pretty much like a {@link java.util.Stack}, except that
 * each call is forwarded to a private {@link InheritableThreadLocal} object.
 * <p>
 * Whenever a child thread gets started, it inherits the top level item
 * of the parent's thread local stack unless the parent's thread local stack
 * is empty.
 * However, it's not possible to {@link #pop} this inherited top level item
 * off the stack - any attempt to do so will result in a
 * {@link NoSuchElementException}.
 * <p>
 * <b>Disclaimer</b>: Although this class internally uses an
 * {@link InheritableThreadLocal}, it does not leak memory in multi class
 * loader environments when used appropriately.
 * <p>
 * This class is thread-safe.
 *
 * @param  <T> The type of the items in the inheritable thread local stack.
 * @author Christian Schlichtherle
 */
@SuppressWarnings("LoopStatementThatDoesntLoop")
public final class InheritableThreadLocalStack<T> {

    private final InheritableThreadLocal<Option<Node<T>>> nodes
            = new InheritableThreadLocal<Option<Node<T>>>() {
        @Override
        protected Option<Node<T>> initialValue() { return Option.none(); }
    };

    /**
     * Returns {@code true} if this stack is empty.
     *
     * @return {@code true} if this stack is empty.
     */
    public boolean isEmpty() {
        return nodes.get().isEmpty();
    }

    /**
     * Returns the top item on this stack or {@code null} if it's empty.
     *
     * @return The top item of this stack or {@code null} if it's empty.
     */
    public T peek() { return peekOrElse(null); }

    /**
     * Returns the nullable top item on this stack unless it's empty,
     * in which case {@code elze} gets returned.
     *
     * @param  elze the nullable default item.
     * @return The nullable top item on this stack unless it's empty,
     *         in which case {@code elze} gets returned.
     */
    public T peekOrElse(final T elze) {
        for (Node<T> n : nodes.get())
            return n.item;
        return elze;
    }

    /**
     * Pushes the given item onto this stack.
     *
     * @param  item the nullable item to push onto this stack.
     * @return {@code item} - for fluent programming.
     */
    public T push(final T item) {
        nodes.set(Option.some(new Node<>(nodes.get(), item)));
        return item;
    }

    /**
     * Removes and returns the nullable top item on this stack.
     *
     * @return The (then no more) nullable top item on this stack.
     * @throws NoSuchElementException if this stack is empty.
     */
    public T pop() {
        for (final Node<T> n : nodes.get()) {
            if (!Thread.currentThread().equals(n.get()))
                throw new NoSuchElementException();
            nodes.set(n.previous); // may be Option.none()
            return n.item;
        }
        throw new NoSuchElementException();
    }

    /**
     * Removes and returns the nullable top item on this stack
     * if it's identical to the given item.
     *
     * @param  expected The expected top item on this stack.
     * @throws IllegalStateException If the given item is not the top
     *         item on this stack.
     */
    public void popIf(final T expected) {
        try {
            final T got = pop();
            if (got != expected) {
                push(got);
                throw new IllegalStateException(got + " (expected " + expected + " as the top item of the inheritable thread local stack)");
            }
        } catch (NoSuchElementException ex) {
            throw new IllegalStateException("The inheritable thread local stack is empty!", ex);
        }
    }

    private static final class Node<T> extends WeakReference<Thread> {

        final Option<Node<T>> previous;
        T item;

        /**
         *
         * @param previous the optional previous node.
         * @param item the nullable item.
         */
        Node(final Option<Node<T>> previous, final T item) {
            super(Thread.currentThread());
            this.previous = previous;
            this.item = item;
        }
    }
}
