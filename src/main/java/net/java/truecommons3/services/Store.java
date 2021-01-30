/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.services;

import javax.annotation.concurrent.Immutable;
import javax.inject.Provider;

/** @author Christian Schlichtherle */
@Immutable
final class Store<P> implements Container<P> {

    private final P product;

    Store(final Provider<P> provider) { this.product = provider.get(); }

    @Override public P get() { return product; }
}
