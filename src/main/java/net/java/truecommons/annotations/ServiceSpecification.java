/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated class or interface specifies a locatable service.
 *
 * @see    <a href="package-summary.html">Package Summary</a>
 * @since  TrueCommons 2.1
 * @author Christian Schlichtherle
 */
@Target(ElementType.TYPE)
@Documented
public @interface ServiceSpecification { }
