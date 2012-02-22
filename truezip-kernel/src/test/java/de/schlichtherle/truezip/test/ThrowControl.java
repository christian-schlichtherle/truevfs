/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.test;

import edu.umd.cs.findbugs.annotations.CleanupObligation;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@NotThreadSafe
@CleanupObligation
public final class ThrowControl {

    private final Map<Class<?>, Throwable> throwables;

    /** Default constructor. */
    @SuppressWarnings("CollectionWithoutInitialCapacity")
    public ThrowControl() {
        this.throwables = new HashMap<Class<?>, Throwable>();
    }

    /**
     * Copy constructor.
     * 
     * @param template The template to copy.
     */
    public ThrowControl(final ThrowControl template) {
        this.throwables = new HashMap<Class<?>, Throwable>(template.throwables);
    }

    public Throwable trigger(Throwable toThrow) {
        return trigger(Object.class, toThrow);
    }

    public Throwable trigger(final Class<?> from, final Throwable toThrow) {
        if (null == from)
            throw new NullPointerException();
        wrap(toThrow); // test if wrappable
        // DON'T put wrap(toThrow)! We want the stack trace of the call to
        // check(*), not of the call to this method!
        return throwables.put(from, toThrow);
    }

    public Throwable clear(Class<?> from) {
        return throwables.remove(from);
    }

    public <X extends Throwable> void check(Object thiz, Class<X> throwz)
    throws X {
        check(thiz.getClass(), throwz);
    }

    @SuppressWarnings("unchecked")
    private <X extends Throwable> void check(   final Class<?> thiz,
                                                final Class<X> throwz)
    throws X {
        final Throwable toThrow = throwables.remove(thiz);
        if (null != toThrow)
            if (throwz.isAssignableFrom(toThrow.getClass()))
                throw wrap((X) toThrow);
            else
                throwables.put(thiz, toThrow); // restore

        // No match, now recursively check interfaces first and then super
        // classes.
        // This may result in redundant checks for interfaces.
        for (final Class<?> ic : thiz.getInterfaces())
            check(ic, throwz);
        final Class<?> sc = thiz.getSuperclass();
        if (null != sc)
            check(sc, throwz);
    }

    @SuppressWarnings("unchecked")
    private static <X extends Throwable> X wrap(final X x) {
        try {
            return (X) x.getClass()
                        .getConstructor(String.class)
                        .newInstance(x.toString())
                        .initCause(x);
        } catch (final Exception ex) {
            ex.initCause(x);
            throw new IllegalArgumentException(ex);
        }
    }

    public static boolean contains(Throwable thiz, final Throwable that) {
        do {
            if (thiz == that)
                return true;
        } while (null != (thiz = thiz.getCause()));
        return false;
    }
}
