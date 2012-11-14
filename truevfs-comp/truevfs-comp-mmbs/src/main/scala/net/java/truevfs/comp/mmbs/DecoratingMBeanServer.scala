/*
 * Copyright (C) 2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.mmbs

import javax.management._

private class DecoratingMBeanServer(mbs: MBeanServer) extends MBeanServer {

  def createMBean(className: String, name: ObjectName) =
    mbs createMBean (className, name)

  def createMBean(className: String, name: ObjectName, loaderName: ObjectName) =
    mbs createMBean (className, name, loaderName)

  def createMBean(className: String, name: ObjectName, params: Array[AnyRef], signature: Array[String]) =
    mbs createMBean (className, name, params, signature)

  def createMBean(className: String, name: ObjectName, loaderName: ObjectName, params: Array[AnyRef], signature: Array[String]) =
    mbs createMBean (className, name, loaderName, params, signature)

  def registerMBean(`object`: Any, name: ObjectName) =
    mbs registerMBean (`object`, name)

  def unregisterMBean(name: ObjectName) { mbs unregisterMBean name }

  def getObjectInstance(name: ObjectName) = mbs getObjectInstance name

  def queryMBeans(name: ObjectName, query: QueryExp) =
    mbs queryMBeans (name, query)

  def queryNames(name: ObjectName, query: QueryExp) =
    mbs queryNames (name, query)

  def isRegistered(name: ObjectName) =
    mbs isRegistered name

  def getMBeanCount() = mbs.getMBeanCount

  def getAttribute(name: ObjectName, attribute: String) =
  mbs getAttribute (name, attribute)

  def getAttributes(name: ObjectName, attributes: Array[String]) =
    mbs getAttributes (name, attributes)

  def setAttribute(name: ObjectName, attribute: Attribute) {
    mbs setAttribute (name, attribute)
  }

  def setAttributes(name: ObjectName, attributes: AttributeList) =
    mbs setAttributes (name, attributes)

  def invoke(name: ObjectName, operationName: String, params: Array[AnyRef], signature: Array[String]) =
    mbs invoke (name, operationName, params, signature)

  def getDefaultDomain() = mbs.getDefaultDomain

  def getDomains() = mbs.getDomains

  def addNotificationListener(name: ObjectName, listener: NotificationListener, filter: NotificationFilter, handback: Any) {
    mbs addNotificationListener (name, listener, filter, handback)
  }

  def addNotificationListener(name: ObjectName, listener: ObjectName, filter: NotificationFilter, handback: Any) {
    mbs addNotificationListener (name, listener, filter, handback)
  }

  def removeNotificationListener(name: ObjectName, listener: ObjectName) {
    mbs removeNotificationListener (name, listener)
  }

  def removeNotificationListener(name: ObjectName, listener: ObjectName, filter: NotificationFilter, handback: Any) {
    mbs removeNotificationListener (name, listener, filter, handback)
  }

  def removeNotificationListener(name: ObjectName, listener: NotificationListener) {
    mbs removeNotificationListener (name, listener)
  }

  def removeNotificationListener(name: ObjectName, listener: NotificationListener, filter: NotificationFilter, handback: Any) {
    mbs removeNotificationListener (name, listener, filter, handback)
  }

  def getMBeanInfo(name: ObjectName) = mbs getMBeanInfo name

  def isInstanceOf(name: ObjectName, className: String) =
    mbs isInstanceOf (name, className)

  def instantiate(className: String) = mbs instantiate className

  def instantiate(className: String, loaderName: ObjectName) =
    mbs instantiate (className, loaderName)

  def instantiate(className: String, params: Array[AnyRef], signature: Array[String]) =
    mbs instantiate (className, params, signature)

  def instantiate(className: String, loaderName: ObjectName, params: Array[AnyRef], signature: Array[String]) =
    mbs instantiate (className, loaderName, params, signature)

  def deserialize(name: ObjectName, data: Array[Byte]) =
    mbs deserialize (name, data)

  def deserialize(className: String, data: Array[Byte]) =
    mbs deserialize (className, data)

  def deserialize(className: String, loaderName: ObjectName, data: Array[Byte]) =
    mbs deserialize (className, loaderName, data)

  def getClassLoaderFor(mbeanName: ObjectName) = mbs getClassLoaderFor mbeanName

  def getClassLoader(loaderName: ObjectName) = mbs getClassLoader loaderName

  def getClassLoaderRepository() = mbs.getClassLoaderRepository
}
