/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.services;

import javax.annotation.concurrent.Immutable;

/**
 * A locatable function.
 * <p>
 * If multiple function classes get located on the class path at run time,
 * the instances get applied in ascending order of their
 * {@linkplain #getPriority() priority} so that the result of the instance
 * with the greatest number becomes the result of the entire function chain.
 * <p>
 * Implementations should be thread-safe.
 *
 * @see    Locator
 * @param  <P> the type of the products to map.
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class LocatableFunction<P>
extends LocatableService implements Function<P> { }
