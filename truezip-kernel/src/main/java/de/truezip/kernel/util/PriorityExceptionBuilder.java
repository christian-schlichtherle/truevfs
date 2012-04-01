/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Assembles an {@link Exception} from one or more {@link Exception}s by
 * {@linkplain Exception#addSuppressed(Throwable) suppressing} and optionally
 * prioritizing them with the help of a {@link Comparator}.
 * 
 * @param  <X> the type of the input and assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public final class PriorityExceptionBuilder<X extends Exception>
extends AbstractExceptionBuilder<X, X> {

    private final Comparator<? super X> comparator;
    private LinkedList<X> suppressed;

    /**
     * Constructs a new priority exception builder.
     * This builder will use the first exception as the assembly and
     * {@linkplain Exception#addSuppressed(Throwable) suppress} all other
     * exceptions.
     */
    public PriorityExceptionBuilder() {
        this(NullComparator.INSTANCE);
    }

    /**
     * Constructs a new priority exception builder.
     * This builder will use the first exception as its initial assembly.
     * Whenever a new exception gets added, the given comparator will get used
     * to determine if the new exception shall get suppressed by the current
     * assembly or shall suppress the current assembly and take its place.
     * The comparator will get called like this:
     * {@code comparator.compare(assembly, exception)} where {@code assembly}
     * is the current assembly and {@code exception} is the exception to add to
     * the assembly.
     * 
     * @param comparator the comparator used for prioritizing the exceptions in
     *        the assembly.
     */
    public PriorityExceptionBuilder(final Comparator<? super X> comparator) {
        if (null == (this.comparator = comparator))
            throw new NullPointerException();
        suppressed = new LinkedList<>();
    }

    @Override
    protected X update(final X input, final @CheckForNull X previous) {
        if (null == previous)
            return input;
        if (comparator.compare(previous, input) >= 0) {
            suppressed.addLast(input);
            return previous;
        } else {
            suppressed.addFirst(previous);
            return input;
        }
    }

    @Override
    protected X post(final X assembly) {
        final Iterator<X> i = suppressed.iterator();
        while (i.hasNext()) {
            assembly.addSuppressed(i.next());
            i.remove();
        }
        assert suppressed.isEmpty();
        return assembly;
    }

    private static final class NullComparator implements Comparator<Exception> {
        static final NullComparator INSTANCE = new NullComparator();

        @Override
        public int compare(Exception o1, Exception o2) {
            return 0;
        }
    } // NullComparator
}
