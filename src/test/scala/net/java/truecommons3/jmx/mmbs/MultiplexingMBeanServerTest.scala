/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.jmx.mmbs

import javax.management._

import net.java.truecommons3.jmx.mmbs.MultiplexingMBeanServerTest._
import net.java.truecommons3.jmx.qonm._
import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._

/**
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
class MultiplexingMBeanServerTest extends WordSpec {

  "A multiplexing MBean server" should {
    val ombs = MBeanServerFactory.newMBeanServer
    val mmbs = new MultiplexingMBeanServer(
      ombs, new QualifierObjectNameModifier("key", "value"))

    "register the MBean" in {
      mmbs registerMBean (mbean, on)
    }

    "have its qualifier added to the MBean's object name in the original MBean server" in {
      val set = ombs queryNames (mn, null)
      set should have size 1
      set.iterator.next should equal (mn)
    }

    "find the registered MBean when quering object names" in {
      val set = mmbs queryNames(on, null)
      set should have size 1
      set.iterator.next should equal (on)
    }

    "find the registered MBean when quering object instances" in {
      val set = mmbs queryMBeans(on, null)
      set should have size 1
      set.iterator.next.getObjectName should equal (on)
    }

    "find the registered MBean when getting object instances" in {
      val instance = mmbs getObjectInstance on
      instance.getObjectName should equal (on)
    }

    "unregister the MBean" in {
      mmbs.unregisterMBean(on)
    }

    "have unregistered the MBean in the original MBean server" in {
      val set = ombs queryNames (mn, null)
      set should have size 0
    }
  }
}

private object MultiplexingMBeanServerTest {

  private trait MessengerMBean {
    def getMessage: String
  }

  private class Messenger extends MessengerMBean {
    def getMessage = "Hello world!"
  }

  private val on = new ObjectName("Test:type=Test") // original name
  private val mn = new ObjectName("Test:type=Test,key=value") // modified name
  private val mbean = new Messenger
}
