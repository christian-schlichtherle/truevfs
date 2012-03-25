/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

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
public final class InheritableThreadLocalStack<T> {
    private final Stacks<T> stacks = new Stacks<T>();

    public boolean isEmpty() {
        return stacks.get().isEmpty();
    }

    public @Nullable T peek() {
        return stacks.get().peek();
    }

    /**
     * Returns the top element on this stack unless it's {@code null}, in which
     * case {@code elze} gets returned.
     * 
     * @param  elze The default element.
     * @return Returns the top element on this stack unless it's {@code null},
     *         in which case {@code elze} gets returned.
     */
    public @Nullable T peekOrElse(final @Nullable T elze) {
        final T element = peek();
        return null != element ? element : elze;
    }

    /**
     * Pushes the given element onto this stack.
     * 
     * @param  element The element to push onto this stack.
     * @return {@code element}
     */
    public @Nullable T push(final @Nullable T element) {
        stacks.get().push(element);
        return element;
    }

    public @Nullable T pop() {
        final Deque<T> stack = stacks.get();
        final T element = stack.pop();
        if (stack.isEmpty())
            stacks.remove();
        return element;
    }

    /**
     * Pops the top element off this stack if its identical to the given
     * element.
     * 
     * @param  expected The expected top element on this stack.
     * @throws IllegalStateException If the given element is not the top
     *         element on this stack.
     */
    public void popIf(final @Nullable T expected) {
        try {
            final @Nullable T got = pop();
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
            final Deque<T> child = new LinkedList<T>();
            final T element = parent.peek();
            if (null != element) {
                child.push(element);
            }
            return child;
        }
    } // Stacks
}
