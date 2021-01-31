/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.qonm;

import global.namespace.service.wight.annotation.ServiceImplementation;
import net.java.truecommons.jmx.ObjectNameModifier;
import net.java.truecommons.jmx.spi.ObjectNameModifierFactory;

/**
 * Okay, this class name is completely nuts!
 *
 * @author Christian Schlichtherle
 */
@ServiceImplementation(priority = -100)
public final class ClassLoaderQualifierObjectNameModifierFactory implements ObjectNameModifierFactory {

    @Override
    public ObjectNameModifier get() {
        final ClassLoader cl = getClass().getClassLoader();
        return new QualifierObjectNameModifier(
                "CLASS_LOADER",
                cl.getClass().getName() + '@' +
                        Integer.toHexString(System.identityHashCode(cl)));
    }
}
