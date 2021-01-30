/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.services;

import javax.annotation.concurrent.Immutable;
import javax.inject.Provider;

/**
 * A locatable provider.
 * <p>
 * If multiple provider classes get located on the class path at run time,
 * the instance with the greatest {@linkplain #getPriority() priority} gets
 * selected.
 * <p>
 * Implementations should be thread-safe.
 *
 * @see    Locator
 * @param  <P> the type of the products to provide.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class LocatableProvider<P>
extends LocatableService implements Provider<P> { }
