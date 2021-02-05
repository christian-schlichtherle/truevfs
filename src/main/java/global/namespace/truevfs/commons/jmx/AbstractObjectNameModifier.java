/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.jmx;

import javax.management.ObjectInstance;

/**
 * An abstract object name modifier.
 *
 * @author Christian Schlichtherle
 */
public abstract class AbstractObjectNameModifier implements ObjectNameModifier {

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractObjectNameModifier} uses
     * {@link #apply(javax.management.ObjectName)} to modify the given object
     * instance.
     */
    @Override public ObjectInstance apply(final ObjectInstance instance) {
        return null == instance
                ? null
                : new ObjectInstance(   apply(instance.getObjectName()),
                                        instance.getClassName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The implementation in the class {@link AbstractObjectNameModifier} uses
     * {@link #unapply(javax.management.ObjectName)} to modify the given object
     * instance.
     */
    @Override public ObjectInstance unapply(final ObjectInstance instance) {
        return null == instance
                ? null
                : new ObjectInstance(   unapply(instance.getObjectName()),
                                        instance.getClassName());
    }
}
