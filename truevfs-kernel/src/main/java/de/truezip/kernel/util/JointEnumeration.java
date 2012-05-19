/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Concatenates two enumerations.
 *
 * @param  <E> the type of the enumerated elements.
 * @author Christian Schlichtherle
 */
@NotThreadSafe
final class JointEnumeration<E> implements Enumeration<E> {
    private Enumeration<? extends E> e1;
    private final Enumeration<? extends E> e2;

    JointEnumeration(
            final Enumeration<? extends E> e1,
            final Enumeration<? extends E> e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
	public boolean hasMoreElements() {
        return e1.hasMoreElements()
           || (e1 != e2 && (e1 = e2).hasMoreElements());
    }

    @Override
	public E nextElement() {
        try {
            return e1.nextElement();
        } catch (NoSuchElementException ex) {
            if (e1 == e2)
                throw ex;
            return (e1 = e2).nextElement();
        }
    }
}
