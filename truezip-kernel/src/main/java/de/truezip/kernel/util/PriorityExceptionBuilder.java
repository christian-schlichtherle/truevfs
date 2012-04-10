/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Assembles an {@link Exception} from one or more input exceptions by
 * {@linkplain Exception#addSuppressed(Throwable) suppressing} all but the
 * first input exception with the highest priority.
 * The priority of the exceptions is determined by the {@link Comparator}
 * provided to the constructor.
 * 
 * @param  <X> the type of the input and assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
public class PriorityExceptionBuilder<X extends Throwable>
extends AbstractExceptionBuilder<X, X> {

    private final Comparator<? super X> comparator;
    private final List<X> exceptions;

    /**
     * Constructs a new priority exception builder.
     * This builder will use the first input exception as its initial assembly.
     * Whenever a new input exception gets added, the given comparator will get
     * used to determine if the new input exception shall get suppressed by the
     * current assembly or shall suppress the current assembly and take its
     * place.
     * The comparator will get called like this:
     * {@code comparator.compare(assembly, input)} where {@code assembly}
     * is the current assembly and {@code input} is the input exception to add
     * to the assembly.
     * 
     * @param comparator the comparator used for prioritizing the exceptions in
     *        the assembly.
     */
    public PriorityExceptionBuilder(final Comparator<? super X> comparator) {
        if (null == (this.comparator = comparator))
            throw new NullPointerException();
        exceptions = new LinkedList<>();
    }

    @Override
    protected final X update(final X input, final @CheckForNull X assembly) {
        exceptions.add(input);
        return null == assembly
                ? input
                : comparator.compare(assembly, input) >= 0 ? assembly : input;
    }

    @Override
    protected final X post(final X assembly) {
        for (   final Iterator<X> i = exceptions.iterator();
                i.hasNext();
                i.remove()) {
            final X exception = i.next();
            if (exception != assembly)
                assembly.addSuppressed(exception);
        }
        assert exceptions.isEmpty();
        return assembly;
    }
}
