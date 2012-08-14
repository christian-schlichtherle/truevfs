/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truezip.util;

import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An inheritable thread local stack of elements.
 * This class works pretty much like a {@link java.util.Stack}, except that
 * each call is forwarded to a private {@link InheritableThreadLocal} object.
 * <p>
 * Whenever a child thread gets started, it will share the <em>same</em>
 * top level element of the parent's inheritable thread local stack unless the
 * parent's inheritable thread local stack is empty.
 * 
 * @param  <T> The type of the elements in the inheritable thread local stack.
 * @since  TrueZIP 7.5
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class InheritableThreadLocalStack<T> {
    private final Stacks<T> stacks = new Stacks<T>();

    /**
     * Returns {@code true} if this stack is empty.
     * 
     * @return {@code true} if this stack is empty.
     */
    public boolean isEmpty() {
        return stacks.probe().isEmpty();
    }

    /**
     * Returns the top element on this stack or {@code null} if it's empty.
     * 
     * @return The top element of this stack or {@code null} if it's empty.
     */
    public @Nullable T peek() {
        return stacks.probe().peek();
    }

    /**
     * Returns the nullable top element on this stack unless it's empty,
     * in which case {@code elze} gets returned.
     * 
     * @param  elze the nullable default element.
     * @return The nullable top element on this stack unless it's empty,
     *         in which case {@code elze} gets returned.
     */
    public @Nullable T peekOrElse(final @CheckForNull T elze) {
        final Deque<T> stack = stacks.probe();
        return stack.isEmpty() ? elze : stack.peek();
    }

    /**
     * Pushes the given element onto this stack.
     * 
     * @param  element the nullable element to push onto this stack.
     * @return {@code element} - for fluent programming.
     */
    public @Nullable T push(final @CheckForNull T element) {
        stacks.get().push(element);
        return element;
    }

    /**
     * Removes and returns the nullable top element on this stack.
     * 
     * @return The (then no more) nullable top element on this stack.
     * @throws NoSuchElementException if this stack is empty.
     */
    public @Nullable T pop() {
        final Deque<T> stack = stacks.probe(); // NOT stacks.get()!
        final T element = stack.pop();
        if (stack.isEmpty()) stacks.remove();
        return element;
    }

    /** @deprecated Since TrueZIP 7.6.1, use {@link #popIff} instead.*/
    @Deprecated
    public void popIf(final @CheckForNull T expected) { popIff(expected); }

    /**
     * Removes and returns the nullable top element on this stack
     * if and only if its identical to the given element.
     * 
     * @param  expected The expected top element on this stack.
     * @throws IllegalStateException If the given element is not the top
     *         element on this stack.
     */
    public void popIff(final @CheckForNull T expected) {
        try {
            final @CheckForNull T got = pop();
            if (got != expected) {
                push(got);
                throw new IllegalStateException(got + " (expected " + expected + " as the top element of the inheritable thread local stack)");
            }
        } catch (NoSuchElementException ex) {
            throw new IllegalStateException("The inheritable thread local stack is empty!", ex);
        }
    }

    private static final class Stacks<T>
    extends InheritableThreadLocal<Deque<T>> {

        @Override
        protected Deque<T> initialValue() {
            return new LinkedList<T>();
        }

        @Override
        protected Deque<T> childValue(final Deque<T> parent) {
            final Deque<T> child = initialValue();
            if (!parent.isEmpty()) child.push(parent.peek());
            return child;
        }

        Deque<T> probe() {
            final Deque<T> stack = super.get();
            if (stack.isEmpty()) super.remove();
            return stack;
        }
    } // Stacks
}
