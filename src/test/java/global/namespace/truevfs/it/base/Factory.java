/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.base;

interface Factory<R, P, X extends Exception> {

    R create(P param) throws X;
}
