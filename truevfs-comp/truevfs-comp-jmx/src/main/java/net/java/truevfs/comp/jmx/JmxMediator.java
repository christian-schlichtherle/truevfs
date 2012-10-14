/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.ObjectName;
import net.java.truevfs.comp.inst.Mediator;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 * 
 * @param  <This> the type of this mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class JmxMediator<This extends JmxMediator<This>>
extends Mediator<This> {

    private Package getDomain() { return getClass().getPackage(); }

    public final JmxObjectNameBuilder nameBuilder(Class<?> type) {
        return new JmxObjectNameBuilder(getDomain())
                .put("type", type.getSimpleName());
    }

    /**
     * {@linkplain JmxComponent#activate Activates} and returns the given
     * {@code component}.
     * 
     * @param  <C> the type of the component to activate.
     * @param  component the component to activate.
     * @return The activated component.
     */
    protected final <C extends JmxComponent> C activate(C component) {
        component.activate();
        return component;
    }

    public boolean register(@CheckForNull ObjectName name, Object mbean) {
        return JmxUtils.register(name, mbean);
    }

    public boolean deregister(ObjectName name) {
        return JmxUtils.deregister(name);
    }
}
