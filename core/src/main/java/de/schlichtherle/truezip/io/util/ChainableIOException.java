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
    private static final long serialVersionUID = 2305749434187324928L;

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
            return cmp != 0 ? cmp : APPEARANCE_COMP.compare(l, r);
        }
    };

    /**
     * Compares two {@code ChainableIOException}s in descending order of their
     * appearance.
     */
    // Note: Not private for unit testing purposes only!
    static final Comparator<ChainableIOException> APPEARANCE_COMP
            = new Comparator<ChainableIOException>() {
        public int compare(ChainableIOException l, ChainableIOException r) {
            return l.getAppearance() - r.getAppearance();
        }
    };
    
    /**
     * The tail chain of this exception chain.
     * Maybe {@code null} if there are no more exceptions.
     * If this exception chain has not been reordered,
     * the head of the tail is either {@code null} or an exception which
     * occured before this exception was created.
     */
    private ChainableIOException predecessor;

    private final int priority;

    private final int appearance;

    int maxAppearance;

    /**
     * Constructs a new chainable I/O exception with the specified predecessor.
     *
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception! May be {@code null}.
     */
    public ChainableIOException(ChainableIOException predecessor) {
        this(predecessor, null, null, 0);
    }

    /**
     * Constructs a new chainable I/O exception with the specified predecessor
     * and message.
     *
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception! May be {@code null}.
     * @param  message The message for this exception.
     */
    public ChainableIOException(
            ChainableIOException predecessor,
            String message) {
        this(predecessor, message, null, 0);
    }
    
    /**
     * Constructs a new chainable I/O exception with the specified predecessor
     * and cause.
     *
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception! May be {@code null}.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).
     *         A {@code null} value is permitted, and indicates that the cause
     *         is nonexistent or unknown.
     */
    public ChainableIOException(
            ChainableIOException predecessor,
            IOException cause) {
        this(predecessor, null, cause, 0);
    }

    /**
     * Constructs a new chainable I/O exception with the specified predecessor,
     * message and cause.
     *
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception! May be {@code null}.
     * @param  message The message for this exception.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).
     *         A {@code null} value is permitted, and indicates that the cause
     *         is nonexistent or unknown.
     */
    public ChainableIOException(
            final ChainableIOException predecessor,
            final String message,
            final IOException cause) {
        this(predecessor, message, cause, 0);
    }

    /**
     * Constructs a new chainable I/O exception with the specified predecessor,
     * message, cause and priority.
     *
     * @param  predecessor An exception that happened <em>before</em> and is
     *         <em>not</em> the cause for this exception! May be {@code null}.
     * @param  message The message for this exception.
     * @param  cause The cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).
     *         A {@code null} value is permitted, and indicates that the cause
     *         is nonexistent or unknown.
     * @param priority The priority of this exception to be used for
     *        {@link #sortPriority() sorting}.
     */
    public ChainableIOException(
            final ChainableIOException predecessor,
            final String message,
            final IOException cause,
            final int priority) {
        super(message != null ? message : cause != null ? cause.toString() : null);
        this.predecessor = predecessor;
        this.priority = priority;
        if (cause != null)
            super.initCause(cause);
        if (predecessor != null)
            maxAppearance = predecessor.maxAppearance + 1;
        else
            maxAppearance = 0;
        appearance = maxAppearance;
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

    /** Returns the priority of this exception. */
    public final int getPriority() {
        return priority;
    }

    /**
     * @return The order of appearance for this ZIP exception.
     */
    public final int getAppearance() {
        return appearance;
    }

    /**
     * @return The exception chain represented by the predecessor exception,
     *         or {@code null} if no predecessing exception exists.
     */
    public ChainableIOException getPredecessor() {
        return predecessor;
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
     * of their appearance.
     *
     * @return The sorted exception chain, consisting of cloned elements where
     *         required to enforce the immutability of this class.
     *         This is a non-destructive sort, i.e. elements already in order
     *         are guaranteed not to get rearranged.
     *         If and only if all elements are in order, this exception chain
     *         is returned and no elements are cloned.
     */
    public ChainableIOException sortAppearance() {
        return sort(APPEARANCE_COMP);
    }

    private ChainableIOException sort(
            final Comparator<ChainableIOException> comp) {
        if (predecessor != null) {
            final ChainableIOException sortedPrior = predecessor.sort(comp);
            if (sortedPrior == predecessor && comp.compare(this, predecessor) >= 0)
                return this;
            else
                return sortedPrior.insert(clone(), comp);
        } else {
            return this;
        }
    }

    private ChainableIOException insert(
            final ChainableIOException element,
            final Comparator<ChainableIOException> comp) {
        if (comp.compare(element, this) >= 0) {
            // Prepend to chain.
            element.predecessor = this;
            element.maxAppearance = Math.max(element.appearance, maxAppearance);
            return element;
        } else {
            // Insert element in the prior exception chain.
            final ChainableIOException clone = clone();
            if (predecessor != null) {
                clone.predecessor = predecessor.insert(element, comp);
                clone.maxAppearance = Math.max(clone.appearance, clone.predecessor.maxAppearance);
            } else {
                element.predecessor = null;
                clone.predecessor = element;
                clone.maxAppearance = element.maxAppearance;
            }
            return clone;
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
