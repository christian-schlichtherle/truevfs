/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.util;

import java.util.Comparator;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Assembles an {@link Exception} from one or more input exceptions by
 * {@linkplain Exception#addSuppressed(Throwable) suppressing} all but the
 * first input exception.
 * 
 * @param  <X> the type of the input and assembled (output) exceptions.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
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

        @Override
        public int compare(Throwable o1, Throwable o2) {
            return 0;
        }
    } // Null
}
