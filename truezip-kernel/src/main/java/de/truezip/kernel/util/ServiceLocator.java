/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.truezip.kernel.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Provides utility methods for convenient class, resource and service location
 * which is designed to work in both OSGi and JEE environments.
 * <p>
 * If the class loader provided to the constructor is the current thread's
 * context class loader, then the methods of this class will locate classes,
 * resources or services by using this class loader only.
 * Otherwise, the given class loader is used first. Second, the current
 * thread's context class loader is used.
 * <p>
 * When loading a class, the search stops on the first hit, so if the given
 * class loader finds the class, the current thread's context class loader is
 * not used.
 * <p>
 * When enumerating resources and services, the results of both class loaders
 * are concatenated, so a resource or a service may get enumerated twice!
 * If this is not acceptable, you should create a set from the enumeration
 * results.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
public final class ServiceLocator {

    static {
        Logger  .getLogger( ServiceLocator.class.getName(),
                            ServiceLocator.class.getName())
                .config("banner");
    }

    private final ClassLoader l1;

    /**
     * Equivalent to
     * {@link #ServiceLocator(java.lang.ClassLoader) new ServiceLocator(null)}.
     */
    public ServiceLocator() {
        this(null);
    }

    /**
     * Constructs a new service locator which uses the given class loader first
     * to locate classes, resources or services on the class path.
     * If this is {@code null}, then the system class loader is used.
     *
     * @param loader the nullable class loader to locate classes, resources and
     *        services.
     */
    public ServiceLocator(final @CheckForNull ClassLoader loader) {
        this.l1 = null != loader ? loader : ClassLoader.getSystemClassLoader();
    }

    /**
     * Enumerates all service provider implementation instances.
     * The service provider implementations are resolved as if by calling
     * {@link ServiceLoader#load(java.lang.Class, java.lang.ClassLoader)}.
     * <p>
     * This method should be preferred over {@link #getService} if more than
     * one meaningful implementation of a service provider interface is
     * expected at run time.
     * 
     * @param  <S> The type of the service provider specification.
     * @param  service the service provider specification.
     * @return A concatenated iteration for the service provider implementation
     *         instances.
     * @throws ServiceConfigurationError if an exception occurs.
     */
    public <S> Iterator<S> getServices(Class<S> service) {
        ClassLoader l2 = Thread.currentThread().getContextClassLoader();
        return l1 == l2
                ? ServiceLoader.load(service, l1).iterator()
                : new JointIterator<S>( ServiceLoader.load(service, l1).iterator(),
                                        ServiceLoader.load(service, l2).iterator());
    }

    /**
     * Returns a new service provider implementation instance.
     * The class name of the service provider implementation is resolved by
     * using the value of the {@link System#getProperty system property} with
     * the service provider interface class name as the key and the given
     * service provider implementation class name as the default value.
     * The service provider implementation class is then loaded using
     * {@link #getClass} and finally instantiated by calling its no-arg
     * constructor.
     * <p>
     * This method should be preferred over {@link #getServices} if
     * <ol>
     * <li>only one meaningful implementation of a service provider interface
     *     is expected at run time, and
     * <li>creating a new service provider implementation instance on each call
     *     is acceptable, and
     * <li>either getting {@code null} as the result or providing a default
     *     service provider implementation is acceptable.
     * </ol>
     *
     * @param  <S> The type of the service provider specification.
     * @param  service the service provider specification.
     * @param  def the default service provider implementation.
     * @return A new service provider implementation instance or {@code null}
     *         if no service provider implementation is known.
     * @throws ServiceConfigurationError if locating, instantiating or casting
     *         the service fails for some reason.
     */
    public @Nullable <S> S
    getService(Class<S> service, @CheckForNull Class<? extends S> def) {
        String name = System.getProperty(   service.getName(),
                                            null == def ? null : def.getName());
        if (null == name)
            return null;
        try {
            return def.cast(getClass(name).newInstance());
        } catch (ClassCastException ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
        } catch (InstantiationException ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
        } catch (IllegalAccessException ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
        }
    }

    /**
     * Loads a class according to the algorithm described in the class Javadoc.
     * 
     * @param  name The fully qualified name of the class to locate.
     * @return The loaded class.
     * @throws ServiceConfigurationError if locating the class fails for some
     *         reason.
     */
    public Class<?> getClass(String name) {
        try {
            try {
                return l1.loadClass(name);
            } catch (ClassNotFoundException ex) {
                ClassLoader l2 = Thread.currentThread().getContextClassLoader();
                if (l1 == l2)
                    throw ex; // there's no point in trying this twice.
                return l2.loadClass(name);
            }
        } catch (ClassNotFoundException ex2) {
            throw new ServiceConfigurationError(ex2.toString(), ex2);
        }
    }

    /**
     * Enumerates resources according to the algorithm described in the class
     * Javadoc.
     *
     * @param  name The fully qualified name of the resources to locate.
     * @return A concatenated enumeration for the resource on the class path.
     * @throws ServiceConfigurationError if locating the resources fails for
     *         some reason.
     */
    public Enumeration<URL> getResources(String name) {
        ClassLoader l2 = Thread.currentThread().getContextClassLoader();
        try {
            return l1 == l2
                    ? l1.getResources(name)
                    : new JointEnumeration<URL>(l1.getResources(name),
                                                l2.getResources(name));
        } catch (IOException ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
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
     * to name a class, which is loaded by using a new {@code ServiceLocator}
     * with the class loader of {@code type} as the primary class loader.
     * </li>
     * <li>
     * Next, if {@code object} is an instance of {@link Class} and {@code type}
     * is not the {@link Class} class instance, then it gets instantiated by
     * calling it's public no-argument constructor.
     * </li>
     * <li>
     * Finally, {@code object} is cast to {@code T} and returned.
     * </li>
     * </ol>
     * 
     * @param  <T> the desired type of the object.
     * @param  object the object to promote.
     * @param  type the class describing the desired type.
     * @return an object of the desired type or {@code null} if and only if
     *         {@code object} is {@code null}.
     * @throws IllegalArgumentException if any promotion step fails.
     * @since  TrueZIP 7.2
     */
    public static @CheckForNull <T> T promote(
            @CheckForNull Object object,
            final Class<T> type) {
        try {
            if (object instanceof String && !type.equals(String.class))
                object = new ServiceLocator(type.getClassLoader())
                        .getClass((String) object);
        } catch (ServiceConfigurationError ex) {
            throw new IllegalArgumentException(ex);
        }
        try {
            if (object instanceof Class<?> && !type.equals(Class.class))
                object = ((Class<?>) object).newInstance();
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
        try {
            return type.cast(object);
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
