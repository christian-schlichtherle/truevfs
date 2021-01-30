/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl;

import java.util.concurrent.Callable;

/**
 * An operation which completes with an arbitrary return value or terminates with an arbitrary exception.
 *
 * @param <T> the type of return value.
 * @param <X> the type of exception.
 */
@FunctionalInterface
interface Op<T, X extends Exception> extends Callable<T> {

    T call() throws X;
}
