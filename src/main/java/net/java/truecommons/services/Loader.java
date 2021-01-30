/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.services;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Loads resources and classes on the class path by using a given class loader
 * and eventually the current thread context's class loader in order.
 * If the primary class loader is the current thread context's class loader,
 * then only the primary class loader will be used.
 * Note that using two class loaders may result in duplicate results - see
 * method Javadoc.
 * If this is undesirable, then you should create a set from the results.
 *
 * @author Christian Schlichtherle
 */
@Immutable
public final class Loader {

    private final ClassLoader primary;

    /**
     * Constructs a new loader which uses the given class loader before using
     * the current thread context's class loader unless the latter is identical
     * to the former.
     *
     * @param loader the nullable primary class loader.
     *        If this is {@code null}, then the system class loader is used.
     */
    public Loader(final @Nullable ClassLoader loader) {
        this.primary = null != loader ? loader : ClassLoader.getSystemClassLoader();
    }

    /**
     * Returns a new iterable collection of URLs for the given resource name.
     *
     * @param  name The fully qualified name of the resources to locate.
     * @return A concatenated enumeration for the resource on the class path.
     * @throws ServiceConfigurationError if locating the resources fails for
     *         some reason.
     */
    public Iterable<URL> resourcesFor(final String name)
    throws ServiceConfigurationError {
        final class IterableResources implements Iterable<URL> {
            @Override
            public Iterator<URL> iterator() {
                try {
                    return new EnumerationIterator<URL>(
                            classLoader().getResources(name));
                } catch (final IOException ex) {
                    throw new ServiceConfigurationError(ex.toString(), ex);
                }
            }
        } // IterableResources
        return new IterableResources();
    }

    private static final class EnumerationIterator<E> implements Iterator<E> {
        private final Enumeration<E> e;

        EnumerationIterator(final Enumeration<E> e) {
            this.e = e;
        }

        @Override
        public boolean hasNext() {
            return e.hasMoreElements();
        }

        @Override
        public E next() {
            return e.nextElement();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    } // EnumerationIterator

    /**
     * Returns a new iterable collection of instances of all implementation
     * classes of the given specification class which are advertised as
     * services on the class path.
     * This method should be preferred over {@link #instanceOf(Class, Class)}
     * if more than one meaningful implementation class of a specification
     * class is expected at run time.
     * <p>
     * The implementation classes will get instantiated as if by calling
     * <code>{@link ServiceLoader#load(Class, ClassLoader) ServiceLoader.load(spec, cl)}.iterator()</code>,
     * where {@code cl} is the resolved class loader.
     *
     * @param  <S> the type of the services.
     * @param  spec the specification class of the services.
     * @return A new iterable collection of all services on the class path.
     */
    public <S> Iterable<S> instancesOf(final Class<S> spec) {
        final class IterableServices implements Iterable<S> {
            @Override
            public Iterator<S> iterator() {
                return ServiceLoader.load(spec, classLoader()).iterator();
            }
        } // IterableServices
        return new IterableServices();
    }

    private ClassLoader classLoader() {
        return UnifiedClassLoader.resolve(primary,
                Thread.currentThread().getContextClassLoader());
    }

    /**
     * Instantiates an implementation class which is named as the value of a
     * system property with the name of the given specification class as the
     * key.
     * If no such system property exists, then the given default implementation
     * class gets instantiated unless it's {@code null}.
     * The implementation class gets loaded using {@link #classFor} and
     * instantiated by calling its public no-arg constructor.
     * <p>
     * This method should be preferred over {@link #instancesOf(Class)} if
     * <ol>
     * <li>only one meaningful implementation of a service provider interface
     *     is expected at run time, and
     * <li>creating a new service provider implementation instance on each call
     *     is acceptable, and
     * <li>either getting {@code null} as the result or providing a default
     *     service provider implementation is acceptable.
     * </ol>
     *
     * @param  <S> the type of the service.
     * @param  spec the specification class of the service.
     * @param  impl the default implementation class of the service.
     * @return A new instance of the service or {@code null}
     *         if no implementation class is known.
     * @throws ServiceConfigurationError if loading or instantiating
     *         the implementation class fails for some reason.
     */
    public @Nullable <S> S instanceOf(
            final Class<S> spec,
            final @Nullable Class<? extends S> impl)
    throws ServiceConfigurationError {
        final String name = System.getProperty(spec.getName(),
                null == impl ? null : impl.getName());
        if (null == name) return null;
        try {
            return impl.cast(classFor(name).newInstance());
        } catch (final InstantiationException ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
        } catch (final IllegalAccessException ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
        }
    }

    /**
     * Loads a class according to the algorithm described in the class Javadoc.
     *
     * @param  name The fully qualified name of the class to classFor.
     * @return The loaded class.
     * @throws ServiceConfigurationError if loading the class fails for some
     *         reason.
     */
    public Class<?> classFor(final String name)
    throws ServiceConfigurationError {
        try {
            try {
                return primary.loadClass(name);
            } catch (final ClassNotFoundException ex) {
                final ClassLoader secondary
                        = Thread.currentThread().getContextClassLoader();
                if (primary == secondary) throw ex; // there's no point in trying this twice.
                return secondary.loadClass(name);
            }
        } catch (final ClassNotFoundException ex2) {
            throw new ServiceConfigurationError(ex2.toString(), ex2);
        }
    }

    /**
     * Promotes the given {@code object} to an instance of the given
     * {@code type}.
     * The following steps are consecutively applied in order to promote the
     * given object by this utility method:
     * <ol>
     * <li>
     * If {@code object} is an instance of {@link String} and {@code type} is
     * not the {@link String} class instance, then {@code object} is considered
     * to name a class, which is loaded by using a new {@code Locator}
     * with the class loader of {@code type} as the primary class loader.
     * </li>
     * <li>
     * Next, if {@code object} is an instance of {@link Class} and {@code type}
     * is not the {@link Class} class instance, then it gets instantiated by
     * calling it's public no-argument constructor.
     * </li>
     * <li>
     * Finally, {@code object} is {@linkplain Class#cast cast} to {@code T} and
     * returned.
     * </li>
     * </ol>
     *
     * @param  <T> the desired type of the object.
     * @param  object the object to promote.
     * @param  type the class describing the desired type.
     * @return an object of the desired type or {@code null} if and only if
     *         {@code object} is {@code null}.
     * @throws IllegalArgumentException if any promotion step fails.
     */
    public static @Nullable <T> T promote(
            @Nullable Object object,
            final Class<T> type)
    throws IllegalArgumentException {
        try {
            if (object instanceof String && !type.equals(String.class))
                object = new Loader(type.getClassLoader())
                        .classFor((String) object);
            if (object instanceof Class<?> && !type.equals(Class.class))
                object = ((Class<?>) object).newInstance();
            return type.cast(object);
        } catch (final ServiceConfigurationError ex) {
            throw new IllegalArgumentException(ex);
        } catch (final InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        } catch (final IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        } catch (final ClassCastException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
