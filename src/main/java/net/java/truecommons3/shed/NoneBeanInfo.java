/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.shed;

import java.beans.*;
import java.util.Objects;

/**
 * Adds support for the {@link None} class when using {@link XMLEncoder}.
 *
 * @deprecated This class is only public for technical reasons.
 * @author Christian Schlichtherle
 */
@Deprecated
public final class NoneBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        final BeanDescriptor descriptor = new BeanDescriptor(None.class, null);
        descriptor.setValue("persistenceDelegate", new NonePersistenceDelegate());
        return descriptor;
    }
}

final class NonePersistenceDelegate extends PersistenceDelegate {

    @Override
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        return Objects.equals(oldInstance, newInstance);
    }

    @Override
    protected Expression instantiate(Object oldInstance, Encoder out) {
        return new Expression(oldInstance, Option.class, "none", null);
    }

    @Override
    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
    }
}
