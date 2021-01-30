/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.annotations.spec;

import net.java.truecommons.annotations.ServiceSpecification;

/**
 * Yet another service specification.
 *
 * @author Christian Schlichtherle
 */
@ServiceSpecification
public abstract class YetAnotherServiceSpecification {

    protected YetAnotherServiceSpecification() { }

    @ServiceSpecification
    public static abstract class BadPracticeSpecification { }
}
