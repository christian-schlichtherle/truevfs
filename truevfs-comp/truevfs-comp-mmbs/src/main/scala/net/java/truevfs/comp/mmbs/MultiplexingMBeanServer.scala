/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.mmbs

import MultiplexingMBeanServer._
import collection.JavaConverters._
import java.util._
import javax.management._

private object MultiplexingMBeanServer {

  def apply(mbs: MBeanServer) = {
    if (classLoader == ClassLoader.getSystemClassLoader) mbs
    else new MultiplexingMBeanServer(mbs)
  }

  private def classLoader = getClass.getClassLoader

  object qualifier {
    val key = "CLASS_LOADER"
    val value = classLoader.getClass.getName + '@' +
      (Integer toHexString (System identityHashCode classLoader))
  }

  private def getKeyPropertyList(name: ObjectName) = name.getKeyPropertyList

  private def copyKeyPropertyList(name: ObjectName) =
    new Hashtable[String, String](name.getKeyPropertyList)

  private val keyPropertyList = {
    val name = new ObjectName(":test=foo")
    try {
      val table = getKeyPropertyList(name)
      table remove "test"
      getKeyPropertyList _
    } catch {
      case _: UnsupportedOperationException =>
        copyKeyPropertyList _
    }
  }

  private def apply(name: ObjectName): ObjectName = {
    if (null eq name) return null
    val domain = name.getDomain
    val table = keyPropertyList(name)
    table put (qualifier.key, qualifier.value)
    ObjectName getInstance (domain, table)
  }

  private def unapply(name: ObjectName): ObjectName = {
    if (null eq name) return null
    val domain = name.getDomain
    val table = keyPropertyList(name)
    table remove qualifier.key
    ObjectName getInstance (domain, table)
  }

  private def apply(instance: ObjectInstance): ObjectInstance = {
    if (null eq instance) return null
    new ObjectInstance(apply(instance.getObjectName), instance.getClassName)
  }

  private def unapply(instance: ObjectInstance): ObjectInstance = {
    if (null eq instance) return null
    new ObjectInstance(unapply(instance.getObjectName), instance.getClassName)
  }
}

private final class MultiplexingMBeanServer(mbs: MBeanServer)
extends DecoratingMBeanServer(mbs) with Immutable {

  override def createMBean(className: String, name: ObjectName) =
    unapply(mbs createMBean (className, apply(name)))

  override def createMBean(className: String, name: ObjectName, loaderName: ObjectName) =
    unapply(mbs createMBean (className, apply(name), loaderName))

  override def createMBean(className: String, name: ObjectName, params: Array[AnyRef], signature: Array[String]) =
    unapply(mbs createMBean (className, apply(name), params, signature))

  override def createMBean(className: String, name: ObjectName, loaderName: ObjectName, params: Array[AnyRef], signature: Array[String]) =
    unapply(mbs createMBean (className, apply(name), loaderName, params, signature))

  override def registerMBean(`object`: Any, name: ObjectName) =
    mbs registerMBean (`object`, apply(name))

  override def unregisterMBean(name: ObjectName) {
    mbs unregisterMBean apply(name)
  }

  override def getObjectInstance(name: ObjectName) =
    unapply(mbs getObjectInstance apply(name))

  override def queryMBeans(name: ObjectName, query: QueryExp) =
    ((mbs queryMBeans (apply(name), query)).asScala map unapply).asJava

  override def queryNames(name: ObjectName, query: QueryExp) =
    ((mbs queryNames (apply(name), query)).asScala map unapply).asJava

  override def isRegistered(name: ObjectName) =
    mbs isRegistered apply(name)

  override def getAttribute(name: ObjectName, attribute: String) =
    mbs getAttribute (apply(name), attribute)

  override def getAttributes(name: ObjectName, attributes: Array[String]) =
    mbs getAttributes (apply(name), attributes)

  override def setAttribute(name: ObjectName, attribute: Attribute) {
    mbs setAttribute (apply(name), attribute)
  }

  override def setAttributes(name: ObjectName, attributes: AttributeList) =
    mbs setAttributes (apply(name), attributes)

  override def invoke(name: ObjectName, operationName: String, params: Array[AnyRef], signature: Array[String]) =
    mbs invoke (apply(name), operationName, params, signature)

  override def addNotificationListener(name: ObjectName, listener: NotificationListener, filter: NotificationFilter, handback: Any) {
    mbs addNotificationListener (apply(name), listener, filter, handback)
  }

  override def addNotificationListener(name: ObjectName, listener: ObjectName, filter: NotificationFilter, handback: Any) {
    mbs addNotificationListener (apply(name), listener, filter, handback)
  }

  override def removeNotificationListener(name: ObjectName, listener: ObjectName) {
    mbs removeNotificationListener (apply(name), listener)
  }

  override def removeNotificationListener(name: ObjectName, listener: ObjectName, filter: NotificationFilter, handback: Any) {
    mbs removeNotificationListener (apply(name), listener, filter, handback)
  }

  override def removeNotificationListener(name: ObjectName, listener: NotificationListener) {
    mbs removeNotificationListener (apply(name), listener)
  }

  override def removeNotificationListener(name: ObjectName, listener: NotificationListener, filter: NotificationFilter, handback: Any) {
    mbs removeNotificationListener (apply(name), listener, filter, handback)
  }

  override def getMBeanInfo(name: ObjectName) = mbs getMBeanInfo apply(name)

  override def isInstanceOf(name: ObjectName, className: String) =
    mbs isInstanceOf (apply(name), className)

  @Deprecated
  override def deserialize(name: ObjectName, data: Array[Byte]) =
    mbs deserialize (apply(name), data)

  override def getClassLoaderFor(mbeanName: ObjectName) =
    mbs getClassLoaderFor apply(mbeanName)
}
