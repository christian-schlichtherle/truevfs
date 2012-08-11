/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.concurrent.ThreadSafe;
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
     * {@linkplain JmxColleague#start Starts} and returns the given
     * {@code colleague}.
     * 
     * @param  <C> the type of the colleague to start.
     * @param  colleague the colleague to start.
     * @return The started colleague.
     */
    protected final <C extends JmxColleague> C start(C colleague) {
        colleague.start();
        return colleague;
    }
}
