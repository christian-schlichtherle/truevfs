/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.jmx.mmbs;

import java.io.ObjectInputStream;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import javax.management.*;
import net.java.truecommons3.jmx.*;
import net.java.truecommons3.shed.HashMaps;

/**
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@Immutable
public final class MultiplexingMBeanServer extends DelegatingMBeanServer {

    private final MBeanServer mbs;
    private final ObjectNameModifier modifier;

    public MultiplexingMBeanServer(final MBeanServer mbs, final ObjectNameModifier modifier) {
        this.mbs = Objects.requireNonNull(mbs);
        this.modifier = Objects.requireNonNull(modifier);
    }

    @Override
    protected MBeanServer mbs() { return mbs; }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return modifier.unapply(mbs().createMBean(className, modifier.apply(name)));
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return modifier.unapply(mbs().createMBean(className, modifier.apply(name), loaderName));
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return modifier.unapply(mbs().createMBean(className, modifier.apply(name), params, signature));
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return modifier.unapply(mbs().createMBean(className, modifier.apply(name), loaderName, params, signature));
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name)
    throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        return modifier.unapply(mbs().registerMBean(object, modifier.apply(name)));
    }

    @Override
    public void unregisterMBean(ObjectName name)
    throws MBeanRegistrationException, InstanceNotFoundException {
        mbs().unregisterMBean(modifier.apply(name));
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return modifier.unapply(mbs().getObjectInstance(modifier.apply(name)));
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        final Set<ObjectInstance> is = mbs().queryMBeans(modifier.apply(name), query);
        final Set<ObjectInstance> os = new LinkedHashSet<>(HashMaps.initialCapacity(is.size()));
        for (ObjectInstance i : is)
            os.add(modifier.unapply(i));
        return os;
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        final Set<ObjectName> is = mbs().queryNames(modifier.apply(name), query);
        final Set<ObjectName> os = new LinkedHashSet<>(HashMaps.initialCapacity(is.size()));
        for (ObjectName i : is)
            os.add(modifier.unapply(i));
        return os;
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        return mbs().isRegistered(modifier.apply(name));
    }

    @Override
    public Integer getMBeanCount() {
        return mbs().getMBeanCount();
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        return mbs().getAttribute(modifier.apply(name), attribute);
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return mbs().getAttributes(modifier.apply(name), attributes);
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        mbs().setAttribute(modifier.apply(name), attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return mbs().setAttributes(modifier.apply(name), attributes);
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return mbs().invoke(modifier.apply(name), operationName, params, signature);
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        mbs().addNotificationListener(modifier.apply(name), listener, filter, handback);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        mbs().addNotificationListener(modifier.apply(name), listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(modifier.apply(name), listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(modifier.apply(name), listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(modifier.apply(name), listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(modifier.apply(name), listener, filter, handback);
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return mbs().getMBeanInfo(modifier.apply(name));
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return mbs().isInstanceOf(modifier.apply(name), className);
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
        return mbs().deserialize(modifier.apply(name), data);
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return mbs().getClassLoaderFor(modifier.apply(mbeanName));
    }
}
