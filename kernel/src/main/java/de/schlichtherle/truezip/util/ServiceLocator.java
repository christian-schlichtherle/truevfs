/*
 * Copyright (C) 2011 Schlichtherle IT Services
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
package de.schlichtherle.truezip.util;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

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
 * @version $Id$
 */
public final class ServiceLocator {

    private final @NonNull ClassLoader l1;

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
     * This method should be preferred over {@link #getService} if <em>more
     * than one</em> meaningful implementation of a service provider interface
     * is expected at run time.
     * 
     * @param  <S> The type of the service provider specification.
     * @param  service the service provider specification.
     * @return A concatenated iteration for the service provider implementation
     *         instances.
     * @throws ServiceConfigurationError if an exception occurs.
     */
    public @NonNull <S> Iterator<S> getServices(@NonNull Class<S> service) {
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
     * the service provider interface class name as the key with the given
     * default service provider implementation class name.
     * The service provider implementation class is then loaded using
     * {@link #getClass} and final finally instantiated by calling its nullary
     * constructor.
     * <p>
     * This method should be preferred over {@link #getServices} if <em>only
     * one</em> meaningful implementation of a service provider interface
     * is expected at run time.
     *
     * @param  <S> The type of the service provider specification.
     * @param  service the service provider specification.
     * @param  def the default service provider implementation.
     * @return A new service provider implementation instance.
     * @throws RuntimeException if a {@link RuntimeException} occurs.
     * @throws ServiceConfigurationError if an {@link Exception} occurs.
     */
    @SuppressWarnings("unchecked")
    public @NonNull <S> S
    getService( @NonNull Class<S> service,
                @CheckForNull Class<? extends S> def) {
        String name = System.getProperty(   service.getName(),
                                            null == def ? null : def.getName());
        try {
            return ((Class<S>) getClass(name)).newInstance();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceConfigurationError(ex.toString(), ex);
        }
    }

    /**
     * Loads a class according to the algorithm described in the class Javadoc.
     * 
     * @param  name The class to locate.
     * @return The loaded class.
     * @throws ClassNotFoundException if loading the class failed for some
     *         reason.
     */
    public @NonNull Class<?> getClass(@NonNull String name)
    throws ClassNotFoundException {
        try {
            return l1.loadClass(name);
        } catch (ClassNotFoundException ex) {
            ClassLoader l2 = Thread.currentThread().getContextClassLoader();
            if (l1 == l2)
                throw ex; // there's no point in trying this twice.
            return l2.loadClass(name);
        }
    }

    /**
     * Enumerates resources according to the algorithm described in the class
     * Javadoc.
     *
     * @param  name The resource to locate.
     * @return A concatenated enumeration for the resource on the class path.
     * @throws IOException If an I/O exception occurs.
     */
    public @NonNull Enumeration<URL> getResources(@NonNull String name)
    throws IOException {
        ClassLoader l2 = Thread.currentThread().getContextClassLoader();
        return l1 == l2
                ? l1.getResources(name)
                : new JointEnumeration<URL>(l1.getResources(name),
                                            l2.getResources(name));
    }
}
