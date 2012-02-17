/*
 * Copyright 2004-2012 Schlichtherle IT Services
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package de.schlichtherle.truezip.mock;

import de.schlichtherle.truezip.util.InheritableThreadLocalStack;
import de.schlichtherle.truezip.util.Resource;
import edu.umd.cs.findbugs.annotations.CleanupObligation;
import edu.umd.cs.findbugs.annotations.CreatesObligation;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A container for configuration options with global or inheritable thread
 * local scope.
 * <p>
 * A thread can call {@link #get()} to get access to the
 * <i>current configuration</i> at any time .
 * If no configuration has been pushed onto the inheritable thread local
 * configuration stack before, this will return the <i>global configuration</i>
 * which is shared by all threads (hence its name).
 * Mind that access to the global configuration is <em>not</em> synchronized.
 * <p>
 * To create an <i>inheritable thread local configuration</i>, a thread can
 * simply call {@link #push()}.
 * This will copy the <i>current configuration</i> (which may be identical to
 * the global configuration) and push the copy on top of the inheritable thread
 * local configuration stack.
 * <p>
 * Later, the thread can use {@link #pop()} or {@link #close()} to
 * pop the current configuration or {@code this} configuration respectively
 * off the top of the inheritable thread local configuration stack again.
 * <p>
 * Finally, whenever a child thread gets started, it will share the
 * <em>same</em> current configuration with its parent thread.
 * This is achieved by copying the top level element of the parent's
 * inheritable thread local configuration stack.
 * If the parent's inheritable thread local configuration stack is empty, then
 * the child will share the global configuration as its current configuration
 * with its parent.
 * As an implication, {@link #pop()} or {@link #close()} can be called at most
 * once in the child thread.
 * 
 * @since   TrueZIP 7.5
 * @author  Christian Schlichtherle
 * @version $Id$
 */
@ThreadSafe
@CleanupObligation
public final class MockControl
extends Resource<RuntimeException>
implements Closeable { // this could be AutoCloseable in JSE 7

    private static final InheritableThreadLocalStack<MockControl>
            configs = new InheritableThreadLocalStack<MockControl>();

    private static final MockControl GLOBAL = new MockControl();

    // I don't think this field should be volatile.
    // This would make a difference if and only if two threads were changing
    // the GLOBAL configuration concurrently, which is discouraged.
    // Instead, the global configuration should only get changed once at
    // application startup and then each thread should modify only its thread
    // local configuration which has been obtained by a call to MockControl.push().
    private Map<Class<?>, Throwable> map;

    /**
     * Returns the current configuration.
     * First, this method peeks the inheritable thread local configuration
     * stack.
     * If no configuration has been {@link #push() pushed} yet, the global
     * configuration is returned.
     * Mind that the global configuration is shared by all threads.
     * 
     * @return The current configuration.
     * @see    #push()
     */
    public static MockControl get() {
        return configs.peekOrElse(GLOBAL);
    }

    /**
     * Creates a new current configuration by copying the current configuration
     * and pushing the copy onto the inheritable thread local configuration
     * stack.
     * 
     * @return The new current configuration.
     * @see    #get()
     */
    @CreatesObligation
    public static MockControl push() {
        return configs.push(new MockControl(get()));
    }

    /**
     * Pops the {@link #get() current configuration} off the inheritable thread
     * local configuration stack.
     * 
     * @throws IllegalStateException If the {@link #get() current configuration}
     *         is the global configuration.
     */
    public static void pop() {
        configs.popIff(get());
    }

    /** Default constructor for the global configuration. */
    private MockControl() {
        this.map = new HashMap<Class<?>, Throwable>();
    }

    /** Copy constructor for inheritable thread local configurations. */
    private MockControl(final MockControl template) {
        this.map = new HashMap<Class<?>, Throwable>(template.map);
    }

    public static Throwable trigger(Throwable toThrow) {
        return trigger(Object.class, toThrow);
    }

    public static Throwable trigger(final Class<?> from, final Throwable toThrow) {
        from.getClass(); // null check
        wrap(toThrow); // test if wrappable
        return get().put(from, toThrow);
    }

    private Throwable put(final Class<?> from, final Throwable toThrow) {
        // DON'T put wrap(toThrow)! We want the stack trace of the call to
        // check(*), not of the call to this method!
        return map.put(from, toThrow);
    }

    public static Throwable clear(Class<?> from) {
        return get().remove(from);
    }

    private Throwable remove(Class<?> from) {
        return map.remove(from);
    }

    public static <X extends Throwable> void check( final Object thiz,
                                                    final Class<X> throwz)
    throws X {
        final Class<?> clazz = thiz.getClass();
        get().check(clazz, throwz);
    }

    @SuppressWarnings("unchecked")
    private <X extends Throwable> void check(   final Class<?> thiz,
                                                final Class<X> throwz)
    throws X {
        final Throwable toThrow = map.remove(thiz);
        if (null != toThrow)
            if (throwz.isAssignableFrom(toThrow.getClass()))
                throw wrap((X) toThrow);
            else
                map.put(thiz, toThrow); // restore

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
    private static <X extends Throwable> X wrap(final X toThrow) {
        return (X) instantiate(toThrow.getClass()).initCause(toThrow);
    }

    private static <O> O instantiate(final Class<O> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IllegalAccessException ex) {
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

    @Override
    protected void onClose() {
        configs.popIff(this);
    }
}
