/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.beans.*;
import java.util.Objects;

/**
 * Adds support for the {@link BitField} class when using {@link XMLEncoder}.
 *
 * @deprecated This class is only public for technical reasons.
 * @author Christian Schlichtherle
 */
@Deprecated
public final class BitFieldBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        final BeanDescriptor descriptor = new BeanDescriptor(BitField.class, null);
        descriptor.setValue("persistenceDelegate", new BitFieldPersistenceDelegate());
        return descriptor;
    }
}

final class BitFieldPersistenceDelegate extends PersistenceDelegate {

    @Override
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        return Objects.equals(oldInstance, newInstance);
    }

    @Override
    protected Expression instantiate(Object oldInstance, Encoder out) {
        final BitField<?> bitField = (BitField<?>) oldInstance;
        return new Expression(
                oldInstance,
                BitField.class,
                "of",
                0 < bitField.cardinality()
                        ? new Object[] {
                            bitField.toEnumSet().iterator().next().getClass(),
                            bitField.toString(),
                        }
                        : new Object[] {
                            bitField.toEnumSet(), // doesn't work with Sun's JDK 1.6.* and is terribly inefficient in JDK 1.7.0-ea
                        });
    }

    @Override
    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
    }
}
