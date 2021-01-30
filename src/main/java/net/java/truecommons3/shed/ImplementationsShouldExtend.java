/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that you should rather extend the specified abstract class than
 * implementing the annotated interface directly in order to maintain binary
 * backwards compatibility when additional methods get added to the annotated
 * interface.
 *
 * @since  TrueCommons 2.2
 * @author Christian Schlichtherle
 */
@Documented
@Target(ElementType.TYPE)
public @interface ImplementationsShouldExtend {

    /**
     * Returns the abstract class which should be extended rather than
     * implementing the annotated interface directly.
     */
    Class<?> value();
}
