/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import net.java.truecommons.logging.LocalizedLogger;
import net.java.truevfs.comp.inst.Mediator;
import net.java.truevfs.comp.jmx.sl.MBeanServerLocator;
import org.slf4j.Logger;

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 * 
 * @param  <This> the type of this mediator.
 * @author Christian Schlichtherle
 */
@ThreadSafe
public abstract class JmxMediator<This extends JmxMediator<This>>
extends Mediator<This> {

    /**
     * The localized logger to use.
     * This field is available for unit testing purposes only!
     */ 
    Logger logger = new LocalizedLogger(JmxMediator.class);

    private Package getDomain() { return getClass().getPackage(); }

    public final JmxObjectNameBuilder nameBuilder(Class<?> type) {
        return new JmxObjectNameBuilder(getDomain())
                .put("type", type.getSimpleName());
    }

    /**
     * Returns the MBean server for registering the MBeans.
     * <p>
     * The implementation in the class {@link JmxMediator} returns
     * {@code MBeanServerLocator.SINGLETON.get()}.
     * 
     * @return the MBean server for registering the MBeans.
     */
    public MBeanServer getMBeanServer() {
        return MBeanServerLocator.SINGLETON.get();
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

    /**
     * Maps the given object {@code name} to the given {@code mbean}
     * in the {@linkplain #getMBeanServer MBean server}.
     * 
     * @param  name the object name.
     * @param  mbean the MBean.
     * @return {@code true} if the MBean has been successfully registered
     *         with the given {@code name}.
     *         {@code false} if any MBean was already registered
     *         with the given {@code name}.
     * @throws IllegalArgumentException if registering the MBean failed with an
     *         {@link JMException}.
     */
    public boolean register(
            final @CheckForNull ObjectName name,
            final Object mbean) {
        try {
            getMBeanServer().registerMBean(mbean, name);
            return true;
        } catch (final InstanceAlreadyExistsException ex) {
            logger.warn("instanceAlreadyExists.warn", name);
            logger.trace("instanceAlreadyExists.trace", ex);
            return false;
        } catch (final JMException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Removes any MBean with the given object {@code name}
     * in the configured MBean server.
     * 
     * @param  name the object name.
     * @return {@code true} if any MBean has been successfully removed
     *         with the given {@code name}.
     *         {@code false} if no MBean was registered
     *         with the given {@code name}.
     * @throws IllegalArgumentException if removing an MBean failed with an
     *         {@link JMException}.
     */
    public boolean deregister(final ObjectName name) {
        try {
            getMBeanServer().unregisterMBean(name);
            return true;
        } catch (final InstanceNotFoundException ex) {
            logger.warn("instanceNotFound.warn", name);
            logger.trace("instanceNotFound.trace", ex);
            return false;
        } catch (final JMException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
