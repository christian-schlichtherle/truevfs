/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.util;

import java.util.Comparator;

/**
 * Assembles an {@link Exception} from one or more input exceptions by
 * {@linkplain Exception#addSuppressed(Throwable) suppressing} all but the
 * first input exception.
 * 
 * @param  <X> the type of the input and assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
public class SuppressedExceptionBuilder<X extends Throwable>
extends PriorityExceptionBuilder<X> {

    /**
     * Constructs a new suppressed exception builder.
     * This builder will use the first input exception as its assembly and
     * {@linkplain Exception#addSuppressed(Throwable) suppress} all other
     * input exceptions.
     */
    public SuppressedExceptionBuilder() {
        super(Null.INSTANCE);
    }

    private static final class Null implements Comparator<Throwable> {

        static final Null INSTANCE = new Null();

        @SuppressWarnings("ComparatorMethodParameterNotUsed")
        @Override
        public int compare(Throwable o1, Throwable o2) {
            return 0;
        }
    }
}
