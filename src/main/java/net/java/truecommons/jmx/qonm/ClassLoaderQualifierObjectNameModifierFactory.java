/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx.qonm;

import javax.annotation.concurrent.Immutable;
import net.java.truecommons.annotations.ServiceImplementation;
import net.java.truecommons.jmx.ObjectNameModifier;
import net.java.truecommons.jmx.spi.ObjectNameModifierFactory;

/**
 * Okay, this class name is completely nuts!
 *
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@Immutable
@ServiceImplementation
public final class ClassLoaderQualifierObjectNameModifierFactory
extends ObjectNameModifierFactory {

    @Override
    public ObjectNameModifier get() {
        final ClassLoader cl = getClass().getClassLoader();
        return new QualifierObjectNameModifier(
                "CLASS_LOADER",
                cl.getClass().getName() + '@' +
                Integer.toHexString(System.identityHashCode(cl)));
    }

    /** @return -100 */
    @Override
    public int getPriority() { return -100; }
}
