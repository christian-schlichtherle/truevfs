/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.jmx;

import java.io.ObjectInputStream;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import javax.management.*;
import javax.management.loading.ClassLoaderRepository;

/**
 * An MBean server which delegates all methods to another MBean server.
 *
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@Immutable
public abstract class DelegatingMBeanServer implements MBeanServer {

    /** Returns the delegate MBean server. */
    protected abstract MBeanServer mbs();

    @Override
    public ObjectInstance createMBean(String className, ObjectName name)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return mbs().createMBean(className, name);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return mbs().createMBean(className, name, loaderName);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return mbs().createMBean(className, name, params, signature);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return mbs().createMBean(className, name, loaderName, params, signature);
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name)
    throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        return mbs().registerMBean(object, name);
    }

    @Override
    public void unregisterMBean(ObjectName name)
    throws MBeanRegistrationException, InstanceNotFoundException {
        mbs().unregisterMBean(name);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return mbs().getObjectInstance(name);
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        return mbs().queryMBeans(name, query);
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return mbs().queryNames(name, query);
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        return mbs().isRegistered(name);
    }

    @Override
    public Integer getMBeanCount() {
        return mbs().getMBeanCount();
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        return mbs().getAttribute(name, attribute);
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return mbs().getAttributes(name, attributes);
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        mbs().setAttribute(name, attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return mbs().setAttributes(name, attributes);
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return mbs().invoke(name, operationName, params, signature);
    }

    @Override
    public String getDefaultDomain() {
        return mbs().getDefaultDomain();
    }

    @Override
    public String[] getDomains() {
        return mbs().getDomains();
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        mbs().addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        mbs().addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        mbs().removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return mbs().getMBeanInfo(name);
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return mbs().isInstanceOf(name, className);
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return mbs().instantiate(className);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return mbs().instantiate(className, loaderName);
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return mbs().instantiate(className, params, signature);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return mbs().instantiate(className, loaderName, params, signature);
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
        return mbs().deserialize(name, data);
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        return mbs().deserialize(className, data);
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
        return mbs().deserialize(className, loaderName, data);
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        return mbs().getClassLoaderFor(mbeanName);
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return mbs().getClassLoader(loaderName);
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        return mbs().getClassLoaderRepository();
    }
}
