/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.services;

import net.java.truecommons3.logging.BundledMessage;
import net.java.truecommons3.logging.LocalizedLogger;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Creates containers or factories of products.
 * Resolving service instances is done in several steps:
 * <p>
 * First, the name of a given <i>provider</i> service class is used as the
 * key string to lookup a {@link System#getProperty system property}.
 * If this yields a value then it's supposed to name a class which gets loaded
 * and instantiated by calling its public no-argument constructor.
 * <p>
 * Otherwise, the class path is searched for any resources with the name
 * {@code "META-INF/services/"} plus the name of the given locatable
 * <i>provider<i> class.
 * If this yields no results, a {@link ServiceConfigurationError} is thrown.
 * <p>
 * Otherwise the classes with the names contained in these resources get loaded
 * and instantiated by calling their public no-argument constructor.
 * Next, the instances are filtered according to their
 * {@linkplain Locatable#getPriority() priority}.
 * Only the instance with the highest priority is kept for subsequent use.
 * <p>
 * Next, the class is searched again for any resources with the name
 * {@code "META-INF/services/"} plus the name of the given locatable
 * <i>function</i> class.
 * If this yields some results, the classes with the names contained in these
 * resources get loaded and instantiated by calling their public no-argument
 * constructor.
 * Next, the instances get sorted in ascending order of their
 * {@linkplain Locatable#getPriority() priority} and kept for subsequent use.
 * <p>
 * Finally, depending on the requesting method either a container or a factory
 * gets created which will use the instantiated provider and functions
 * to obtain a product and map it in order of their priorities.
 *
 * @see    ServiceLoader
 * @author Christian Schlichtherle
 */
@Immutable
public final class ServiceLocator {

    private static final Logger logger = new LocalizedLogger(ServiceLocator.class);
    private static final Marker CONFIG = MarkerFactory.getMarker("CONFIG");

    private final Loader loader;

    /**
     * Constructs a new locator which uses the class loader of the given client
     * class before using the current thread context's class loader unless the
     * latter is identical to the former.
     *
     * @param client the class which identifies the calling client.
     */
    public ServiceLocator(final Class<?> client) {
        this(client.getClassLoader());
    }

    /**
     * Constructs a new locator which uses the given class loader before using
     * the current thread context's class loader unless the latter is identical
     * to the former.
     *
     * @param loader the class loader to use before the current thread
     *        context's class loader unless the the latter is identical to the
     *        former.
     * @since TrueCommons 1.0.13
     */
    public ServiceLocator(final ClassLoader loader) {
        this.loader = new Loader(loader);
    }

    /**
     * Creates a new factory for products.
     *
     * @param  <P> the type of the products to create.
     * @param  factory the class of the locatable factory for the products.
     * @return A new factory of products.
     * @throws ServiceConfigurationError if loading or instantiating
     *         a located class fails for some reason.
     */
    public <P> Factory<P> factory(Class<? extends LocatableFactory<P>> factory)
    throws ServiceConfigurationError {
        return factory(factory, null);
    }

    /**
     * Creates a new factory for products.
     *
     * @param  <P> the type of the products to create.
     * @param  factory the class of the locatable factory for the products.
     * @param  functions the class of the locatable functions for the products.
     * @return A new factory of products.
     * @throws ServiceConfigurationError if loading or instantiating
     *         a located class fails for some reason.
     */
    public <P> Factory<P> factory(
            final Class<? extends LocatableFactory<P>> factory,
            final @Nullable Class<? extends LocatableFunction<P>> functions)
    throws ServiceConfigurationError {
        final LocatableFactory<P> p = provider(factory);
        final LocatableFunction<P>[] f = null == functions ? null
                : functions(functions);
        return null == f || 0 == f.length ? p
                : new FactoryWithSomeFunctions<P>(p, f);
    }

    /**
     * Creates a new container with a single product.
     *
     * @param  <P> the type of the product to contain.
     * @param  provider the class of the locatable provider for the product.
     * @return A new container with a single product.
     * @throws ServiceConfigurationError if loading or instantiating
     *         a located class fails for some reason.
     */
    public <P> Container<P> container(Class<? extends LocatableProvider<P>> provider)
    throws ServiceConfigurationError {
        return container(provider, null);
    }

    /**
     * Creates a new container with a single product.
     *
     * @param  <P> the type of the product to contain.
     * @param  provider the class of the locatable provider for the product.
     * @param  decorator the class of the locatable decoractors for the product.
     * @return A new container with a single product.
     * @throws ServiceConfigurationError if loading or instantiating
     *         a located class fails for some reason.
     */
    public <P> Container<P> container(
            final Class<? extends LocatableProvider<P>> provider,
            final @Nullable Class<? extends LocatableDecorator<P>> decorator)
    throws ServiceConfigurationError {
        final LocatableProvider<P> p = provider(provider);
        final LocatableDecorator<P>[] d = null == decorator ? null
                : functions(decorator);
        return new Store<P>(null == d || 0 == d.length ? p
                : new ProviderWithSomeFunctions<P>(p, d));
    }

    private <S extends LocatableProvider<?>> S provider(final Class<S> spec)
    throws ServiceConfigurationError {
        S service = loader.instanceOf(spec, null);
        if (null == service) {
            for (final S newService : loader.instancesOf(spec)) {
                logger.debug(CONFIG, "located", newService);
                if (null == service) {
                    service = newService;
                } else {
                    final int op = service.getPriority();
                    final int np = newService.getPriority();
                    if (op < np) {
                        service = newService;
                    } else if (op == np) {
                        // Mind you that the loader may return multiple class
                        // instances with an equal name which are loaded by
                        // different class loaders.
                        if (!service.getClass().getName()
                                .equals(newService.getClass().getName()))
                            logger.warn("collision",
                                    new Object[] { op, service, newService });
                    }
                }
            }
        }
        if (null == service)
            throw new ServiceConfigurationError(
                    new BundledMessage(ServiceLocator.class, "null", spec).toString());
        logger.debug(CONFIG, "selecting", service);
        return service;
    }

    private <S extends LocatableFunction<?>> S[] functions(final Class<S> spec)
    throws ServiceConfigurationError {
        final Collection<S> c = new LinkedList<S>();
        for (final S service : loader.instancesOf(spec)) c.add(service);
        @SuppressWarnings("unchecked")
        final S[] a = c.toArray((S[]) Array.newInstance(spec, c.size()));
        Arrays.sort(a, new LocatableComparator());
        for (final S service : a) logger.debug(CONFIG, "selecting", service);
        return a;
    }
}
