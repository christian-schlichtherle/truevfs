/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.shed;

/**
 * An operation may terminate with an arbitrary result or exception.
 *
 * @param <T> the type of the result.
 * @param <X> the type of the exception.
 */
@FunctionalInterface
public interface Operation<T, X extends Exception> {

    T run() throws X;
}
