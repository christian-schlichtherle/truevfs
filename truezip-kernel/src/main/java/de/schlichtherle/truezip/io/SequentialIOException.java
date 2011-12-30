/*
 * Copyright (C) 2005-2011 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.io;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Comparator;

/**
 * Represents a chain of subsequently occured {@code IOException}s which have
 * <em>not</em> caused each other.
 * <p>
 * This class supports chaining I/O exceptions for reasons other than
 * causes (which is a functionality already provided by J2SE 1.4 and later).
 * A chainable I/O exception can be used to implement an algorithm which must
 * be able to continue with some work although one or more I/O exceptions have
 * occured.
 * <p>
 * For example, when looping through a list of files, an algorithm might
 * encounter an I/O exception when processing a file element in the list.
 * However, it may still be required to process the remaining files in the list
 * before actually throwing the corresponding I/O exception.
 * Hence, whenever this algorithm encounters an I/O exception, it would catch
 * the I/O exception, create a chainable I/O exception for it and continue
 * processing the remainder of the list.
 * Finally, at the end of the algorithm, if any I/O exceptions
 * have occured, the chainable I/O exception would get sorted according to
 * priority (see {@link #getPriority()} and {@link #sortPriority()}) and
 * finally thrown.
 * This would allow a client application to filter the I/O exceptions by
 * priority with a simple try-catch statement, ensuring that no other
 * exception of higher priority is in the catched exception chain.
 * <p>
 * This class is thread-safe.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class SequentialIOException extends IOException implements Cloneable {

    private static final long serialVersionUID = 2203967634187324928L;

    private static int maxPrintExceptions = 3;

    /**
     * Compares two chainable I/O exceptions in descending order of their
     * priority.
     * If the priority is equal, the elements are compared in descending
     * order of their appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator<SequentialIOException> PRIORITY_COMP
            = new Comparator<SequentialIOException>() {
        @Override
		public int compare(SequentialIOException l, SequentialIOException r) {
            final int cmp = l.priority - r.priority;
            return cmp != 0 ? cmp : INDEX_COMP.compare(l, r);
        }
    };

    /**
     * Compares two chainable I/O exceptions in descending order of their
     * appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator<SequentialIOException> INDEX_COMP
            = new Comparator<SequentialIOException>() {
        @Override
		public int compare(SequentialIOException l, SequentialIOException r) {
            return l.index - r.index;
        }
    };

    /**
     * The tail of this exception chain.
     * Maybe {@code this} if the predecessor hasn't been
     * {@link #initPredecessor(SequentialIOException)} initialized yet or
     * {@code null} if there are no more exceptions in this chain.
     */
    private SequentialIOException predecessor = this;

    private final int priority;

    private int index; // effectively final

    // Note: Not private for unit testing purposes only!
    int maxIndex; // effectively final

    public SequentialIOException() {
        priority = 0;
    }

    public SequentialIOException(String message) {
        super(message);
        priority = 0;
    }

    public SequentialIOException(Throwable cause) {
        super(null == cause ? null : cause.toString());
        super.initCause(cause);
        priority = 0;
    }

    public SequentialIOException(String message, Throwable cause) {
        super(message);
        super.initCause(cause);
        priority = 0;
    }

    public SequentialIOException(int priority) {
        this.priority = priority;
    }

    public SequentialIOException(String message, int priority) {
        super(message);
        this.priority = priority;
    }

    public SequentialIOException(Throwable cause, int priority) {
        super(cause);
        this.priority = priority;
    }

    /**
     * Constructs a new chainable I/O exception with the given
     * {@code message}, {@code cause} and {@code priority}.
     * The predecessor of this exception remains unknown until initiliazed
     * by calling the method {@link #initPredecessor(SequentialIOException)}.
     *
     * @param message The message for this exception.
     * @param cause The cause exception.
     *         A {@code null} value is permitted, and indicates that the cause
     *         for this exception is nonexistent.
     * @param priority The priority of this exception to be used for
     *        {@link #sortPriority() priority sorting}.
     */
    public SequentialIOException(String message, Throwable cause, int priority) {
        super(message, cause);
        this.priority = priority;
    }

    /** Returns a <em>shallow</em> clone of this exception. */
    @Override
    public SequentialIOException clone() {
        try {
            return (SequentialIOException) super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    @Override
    public SequentialIOException initCause(final Throwable cause) {
        super.initCause(cause);
        return this;
    }

    /**
     * Initializes the <i>predecessor</i> of this chainable exception to
     * the given object. This method can be called at most once unless the
     * given predecessor is the same as the previously initialized predecessor.
     *
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception!
     *         Must be {@code null} to indicate that a predecessor does not
     *         exist.
     * @return A reference to this object.
     * @throws IllegalStateException If the predecessor has already been set.
     * @throws IllegalArgumentException If the given {@code predecessor} is
     *         this instance or has not been initialized with a predecessor
     *         itself.
     */
    public final synchronized SequentialIOException initPredecessor(
            SequentialIOException predecessor) {
        setPredecessor(predecessor);
        predecessor = getPredecessor();
        if (predecessor != null)
            index = maxIndex = predecessor.maxIndex + 1;
        return this;
    }

    private void setPredecessor(
            final SequentialIOException predecessor) {
        if (this.predecessor != this) {
            if (this.predecessor == predecessor)
                return;
            throw new IllegalStateException("Can't overwrite predecessor!");
        }
        if (predecessor == this)
            throw new IllegalArgumentException("Can't be predecessor of myself!");
        if (predecessor != null && predecessor.predecessor == predecessor)
            throw new IllegalArgumentException("The predecessor's predecessor must be initialized in order to inhibit loops!");
        this.predecessor = predecessor;
    }

    /**
     * Returns the exception chain represented by the predecessing exception,
     * or {@code null} if no predecessing exception exists or this property
     * hasn't been
     * {@link #initPredecessor(SequentialIOException) initialized} yet.
     */
    public final synchronized SequentialIOException getPredecessor() {
        return this == predecessor ? null : predecessor;
    }

    /** Returns the priority of this exception. */
    public final int getPriority() {
        return priority;
    }

    /**
     * Sorts the elements of this exception chain in descending order
     * of their priority.
     * If the priority of two elements is equal, they are sorted in descending
     * order of their appearance.
     *
     * @return The sorted exception chain, consisting of cloned elements where
     *         required to enforce the immutability of this class.
     *         This is a non-destructive sort, i.e. elements already in order
     *         are guaranteed not to get rearranged.
     *         If and only if all elements are in order, this exception chain
     *         is returned and no elements are cloned.
     */
    public SequentialIOException sortPriority() {
        return sort(PRIORITY_COMP);
    }
    
    /**
     * Sorts the elements of this exception chain in descending order
     * of their appearance.
     *
     * @return The sorted exception chain, consisting of cloned elements where
     *         required to enforce the immutability of this class.
     *         This is a non-destructive sort, i.e. elements already in order
     *         are guaranteed <em>not</em> to get rearranged.
     *         If and only if all elements are in order, this exception chain
     *         is returned and no elements are cloned.
     */
    public SequentialIOException sortAppearance() {
        return sort(INDEX_COMP);
    }

    private SequentialIOException sort(
            final Comparator<SequentialIOException> cmp) {
        final SequentialIOException predecessor = getPredecessor();
        if (predecessor == null)
            return this;
        final SequentialIOException tail = predecessor.sort(cmp);
        if (tail == predecessor && cmp.compare(this, tail) >= 0)
            return this;
        else
            return tail.insert(clone(), cmp);
    }

    private SequentialIOException insert(
            final SequentialIOException element,
            final Comparator<SequentialIOException> cmp) {
        if (cmp.compare(element, this) >= 0) {
            // Prepend to chain.
            element.predecessor = this;
            element.maxIndex = Math.max(element.index, maxIndex);
            return element;
        } else {
            // Insert element in the predecessor exception chain.
            final SequentialIOException predecessor = this.predecessor;
            assert predecessor != this;
            final SequentialIOException clone = clone();
            if (predecessor != null) {
                clone.predecessor = predecessor.insert(element, cmp);
                clone.maxIndex = Math.max(clone.index, clone.predecessor.maxIndex);
            } else {
                element.predecessor = null;
                clone.predecessor = element;
                clone.maxIndex = element.maxIndex;
            }
            return clone;
        }
    }

    /**
     * Prints up to {@link #getMaxPrintExceptions()} exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    @Override
    public void printStackTrace(PrintStream s) {
        printStackTrace(s, getMaxPrintExceptions());
    }

    /**
     * Prints up to {@code maxExceptions()} exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    public void printStackTrace(final PrintStream s, int maxExceptions) {
        maxExceptions--;
        final SequentialIOException predecessor = getPredecessor();
        if (null != predecessor) {
            if (maxExceptions > 0) {
                predecessor.printStackTrace(s, maxExceptions);
                s.println("\nFollowed, but not caused by:");
            } else {
                s.println("\nOmitting " + predecessor.getNumExceptions() + " more exception(s) at the start of this list!");
            }
        }
        super.printStackTrace(s);
    }

    private int getNumExceptions() {
        final SequentialIOException predecessor = getPredecessor();
        return null != predecessor ? predecessor.getNumExceptions() + 1 : 1;
    }

    /**
     * Prints up to {@link #getMaxPrintExceptions()} exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    @Override
    public void printStackTrace(PrintWriter s) {
        printStackTrace(s, getMaxPrintExceptions());
    }

    /**
     * Prints up to {@code maxExceptions()} exceptions in this
     * chain to the provided {@link PrintStream}.
     * <p>
     * Exceptions are printed in ascending order of this chain.
     * If this chain has not been sorted, this results in the exceptions being
     * printed in order of their appearance.
     * <p>
     * If more exceptions are in this chain than are allowed to be printed,
     * then the printed message starts with a line indicating the number of
     * exceptions which have been omitted from the beginning of this chain.
     * Thus, this exception is always printed as the last exception in the
     * list.
     */
    public void printStackTrace(final PrintWriter s, int maxExceptions) {
        maxExceptions--;

        final SequentialIOException predecessor = getPredecessor();
        if (predecessor != null) {
            if (maxExceptions > 0) {
                predecessor.printStackTrace(s, maxExceptions);
                s.println("\nFollowed, but not caused by:");
            } else {
                s.println("\nOmitting " + predecessor.getNumExceptions() + " more exception(s) at the start of this list!");
            }
        }

        super.printStackTrace(s);
    }

    /**
     * @see #printStackTrace(PrintStream)
     * @see #printStackTrace(PrintWriter)
     */
    public static int getMaxPrintExceptions() {
        return maxPrintExceptions;
    }
    
    /**
     * @see #printStackTrace(PrintStream)
     * @see #printStackTrace(PrintWriter)
     */
    public static void setMaxPrintExceptions(int maxPrintExcepions) {
        SequentialIOException.maxPrintExceptions = maxPrintExcepions;
    }
}
