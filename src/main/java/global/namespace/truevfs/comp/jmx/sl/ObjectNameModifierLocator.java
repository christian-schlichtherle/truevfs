/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx.sl;

import global.namespace.service.wight.core.ServiceLocator;
import global.namespace.truevfs.comp.jmx.ObjectNameModifier;
import global.namespace.truevfs.comp.jmx.spi.ObjectNameModifierDecorator;
import global.namespace.truevfs.comp.jmx.spi.ObjectNameModifierFactory;

import java.util.function.Supplier;

/**
 * A container of the singleton object name codec.
 * The codec is created by using a {@link ServiceLocator} to search for advertised implementations of the factory
 * service specification class {@link ObjectNameModifierFactory} and the decorator service specification class
 * {@link ObjectNameModifierDecorator}.
 *
 * @author Christian Schlichtherle
 */
public final class ObjectNameModifierLocator implements Supplier<ObjectNameModifier> {

    /** The singleton instance of this class. */
    public static final ObjectNameModifierLocator SINGLETON = new ObjectNameModifierLocator();

    private ObjectNameModifierLocator() { }

    @Override
    public ObjectNameModifier get() { return Lazy.codec; }

    /** A static data utility class used for lazy initialization. */
    private static final class Lazy {
        static final ObjectNameModifier codec =
                new ServiceLocator().provider(ObjectNameModifierFactory.class, ObjectNameModifierDecorator.class).get();
    }
}
