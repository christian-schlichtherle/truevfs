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

package de.schlichtherle.truezip.io.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Comparator;

/**
 * Represents a chain of subsequently occured {@link IOException}s which have
 * <em>not</em> caused each other.
 * <p>
 * This class supports chaining exceptions for reasons other than causes
 * (which is a functionality already provided by J2SE 1.4 and later).
 * A {@code ChainableIOException} can be used to implement an algorithm
 * which must be able to continue with some work although one or more
 * {@code IOException}s have occured.
 * <p>
 * For example, when looping through a list of files, an algorithm might
 * encounter an {@link IOException} when processing a file element in the list.
 * However, it may still be required to process the remaining files in the list
 * before actually throwing the corresponding {@link IOException}.
 * Hence, whenever this algorithm encounters an {@link IOException},
 * it would catch the {@link IOException}, create a
 * {@code ChainableIOException} for it and continue processing the
 * remainder of the list.
 * Finally, at the end of the algorithm, if any {@link IOException}s
 * have occured, the {@code ChainableIOException} chain would get sorted
 * according to priority (see {@link #getPriority()} and
 * {@link #sortPriority()}) and finally thrown.
 * This would allow a client application to filter the exceptions by priority
 * with a simple try-catch statement, ensuring that no other exception of
 * higher priority is in the catched exception chain.
 *
 * @author Christian Schlichtherle
 * @version $Id$
 */
public class ChainableIOException extends IOException implements Cloneable {
    private static final long serialVersionUID = 2203967634187324928L;

    private static int maxPrintExceptions = 3;

    /**
     * Compares two {@code ChainableIOException}s in descending order of their
     * priority.
     * If the priority is equal, the elements are compared in descending
     * order of their appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator<ChainableIOException> PRIORITY_COMP
            = new Comparator<ChainableIOException>() {
        public int compare(ChainableIOException l, ChainableIOException r) {
            final int cmp = l.getPriority() - r.getPriority();
            return cmp != 0 ? cmp : INDEX_COMP.compare(l, r);
        }
    };

    /**
     * Compares two {@code ChainableIOException}s in descending order of their
     * appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator<ChainableIOException> INDEX_COMP
            = new Comparator<ChainableIOException>() {
        public int compare(ChainableIOException l, ChainableIOException r) {
            return l.getIndex() - r.getIndex();
        }
    };

    /**
     * The tail of this exception chain.
     * Maybe {@code this} if the predecessor hasn't been
     * {@link #initPredecessor(ChainableIOException)} initialized yet or
     * {@code null} if there are no more exceptions in this chain.
     */
    private ChainableIOException predecessor = this;

    private int priority; // effectively final

    private int index; // effectively final

    // Note: Not private for unit testing purposes only!
    int maxIndex; // effectively final

    public ChainableIOException() {
    }

    public ChainableIOException(ChainableIOException predecessor) {
        init(predecessor, 0);
    }

    public ChainableIOException(
            String message,
            ChainableIOException predecessor) {
        super(message);
        init(predecessor, 0);
    }
    
    public ChainableIOException(
            IOException cause,
            ChainableIOException predecessor) {
        super.initCause(cause);
        init(predecessor, 0);
    }

    public ChainableIOException(
            String message,
            IOException cause,
            ChainableIOException predecessor) {
        super(message);
        super.initCause(cause);
        init(predecessor, 0);
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public ChainableIOException(
            String message,
            IOException cause,
            int priority) {
        super(message);
        super.initCause(cause);
        init(this, priority);
    }

    /**
     * Constructs a new chainable I/O exception with the specified
     * {@code message}, {@code cause}, {@code predecessor} and {@code priority}.
     *
     * @param  message The message for this exception.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).
     *         A {@code null} value is permitted, and indicates that the cause
     *         is nonexistent or unknown.
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception! May be {@code null}.
     * @param priority The priority of this exception to be used for
     *        {@link #sortPriority() priority sorting}.
     */
    public ChainableIOException(
            final String message,
            final IOException cause,
            final ChainableIOException predecessor,
            final int priority) {
        super(message);
        super.initCause(cause);
        init(predecessor, priority);
    }

    private void init(
            final ChainableIOException predecessor,
            final int priority) {
        setPredecessor(predecessor);
        this.maxIndex = getPredecessor() != null
                ? predecessor.maxIndex + 1 : 0;
        this.priority = priority;
        this.index = maxIndex;
    }

    /** Returns a <em>shallow</em> clone of this exception. */
    @Override
    public ChainableIOException clone() {
        try {
            return (ChainableIOException) super.clone();
        } catch (CloneNotSupportedException cannotHappen) {
            throw new AssertionError(cannotHappen);
        }
    }

    /**
     * Equivalent to
     * {@code return (ChainableIOException) super.initCause(cause);}.
     */
    @Override
    public ChainableIOException initCause(final Throwable cause) {
        return (ChainableIOException) super.initCause(cause);
    }

    /**
     * Returns the exception chain represented by the predecessor exception,
     * or {@code null} if no predecessing exception exists or this property
     * hasn't been
     * {@link #initPredecessor(ChainableIOException) initialized} yet.
     */
    public final ChainableIOException getPredecessor() {
        return predecessor == this ? null : predecessor;
    }

    private void setPredecessor(
            final ChainableIOException predecessor) {
        if (this.predecessor != this)
            throw new IllegalStateException("Can't overwrite predecessor!");
        if (predecessor == this)
            throw new IllegalArgumentException("Can't be predecessor of myself!");
        if (predecessor != null)
            if (predecessor.predecessor == predecessor)
                throw new IllegalArgumentException("The predecessor's predecessor must be initialized in order to inhibit loops!");
        this.predecessor = predecessor;
    }

    public synchronized ChainableIOException initPredecessor(
            ChainableIOException predecessor) {
        setPredecessor(predecessor);
        return this;
    }

    /** Returns the priority of this exception. */
    public final int getPriority() {
        return priority;
    }

    /**
     * Returns the zero-based index number of this exception when it was first
     * linked into this chain.
     */
    public final int getIndex() {
        return index;
    }

    /** Returns the number of exceptions in this chain. */
    public final int getLength() {
        return maxIndex + 1;
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
    public ChainableIOException sortPriority() {
        return sort(PRIORITY_COMP);
    }
    
    /**
     * Sorts the elements of this exception chain in descending order
     * of their index.
     *
     * @return The sorted exception chain, consisting of cloned elements where
     *         required to enforce the immutability of this class.
     *         This is a non-destructive sort, i.e. elements already in order
     *         are guaranteed not to get rearranged.
     *         If and only if all elements are in order, this exception chain
     *         is returned and no elements are cloned.
     */
    public ChainableIOException sortIndex() {
        return sort(INDEX_COMP);
    }

    private ChainableIOException sort(
            final Comparator<ChainableIOException> cmp) {
        final ChainableIOException pre = getPredecessor();
        if (pre == null)
            return this;
        final ChainableIOException tail = pre.sort(cmp);
        if (tail == pre && cmp.compare(this, pre) >= 0)
            return this;
        else
            return tail.insert(clone(), cmp);
    }

    private ChainableIOException insert(
            final ChainableIOException element,
            final Comparator<ChainableIOException> cmp) {
        if (cmp.compare(element, this) >= 0) {
            // Prepend to chain.
            element.predecessor = this;
            element.maxIndex = Math.max(element.index, maxIndex);
            return element;
        } else {
            // Insert element in the prior exception chain.
            final ChainableIOException predecessor = this.predecessor;
            assert predecessor != this;
            final ChainableIOException clone = clone();
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

        if (predecessor != null) {
            if (maxExceptions > 0) {
                predecessor.printStackTrace(s, maxExceptions);
                s.println("followed, but not caused by:");
            } else {
                s.println("(omitting " + predecessor.getNumExceptions() + " exception(s) at the start of this list)");
            }
        }

        super.printStackTrace(s);
    }

    private int getNumExceptions() {
        return predecessor != null ? predecessor.getNumExceptions() + 1 : 1;
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

        if (predecessor != null) {
            if (maxExceptions > 0) {
                predecessor.printStackTrace(s, maxExceptions);
                s.println("followed, but not caused by:");
            } else {
                s.println("(omitting " + predecessor.getNumExceptions() + " exception(s) at the start of this list)");
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
        ChainableIOException.maxPrintExceptions = maxPrintExcepions;
    }
}
