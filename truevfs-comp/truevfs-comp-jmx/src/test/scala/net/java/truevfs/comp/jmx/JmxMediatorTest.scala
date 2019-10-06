/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx

import javax.management._
import net.java.truevfs.comp.jmx.JmxMediatorTest._
import org.scalatest.Matchers._
import org.scalatest._

/** @author Christian Schlichtherle */
class JmxMediatorTest extends WordSpec with OneInstancePerTest {

  "A JmxMediator" should {
    val mediator = new TestMediator
    def name = mediator.nameBuilder(classOf[Messenger]).get

    "build the correct object name" in {
      name should equal (new ObjectName(mediator.getClass.getPackage.getName + ":type=" + classOf[Messenger].getSimpleName))
    }

    "succeed to register an MBean for the object name" in {
      mediator register (name, new Messenger) should equal (true)
    }

    "fail to register another MBean for an equal object name" in {
      mediator register (name, new Messenger) should equal (false)
    }

    "succeed to deregister the MBean for an equal object name" in {
      mediator deregister name should equal (true)
    }

    "fail to deregister an MBean for any other object name" in {
      mediator deregister new ObjectName(":type=unknown") should equal (false)
    }
  }
}

private object JmxMediatorTest {

  private[this] val mbs = MBeanServerFactory.newMBeanServer

  private class TestMediator extends JmxMediator[TestMediator] {

    override def getMBeanServer: MBeanServer = mbs
  }

  trait MessengerMXBean {

    def getMessage: String
  }

  class Messenger extends MessengerMXBean {

    override def getMessage = "Hello world!"
  }
}
