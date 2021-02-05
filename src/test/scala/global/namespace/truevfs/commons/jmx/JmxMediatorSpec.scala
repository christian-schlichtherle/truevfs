/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.commons.jmx

import global.namespace.truevfs.commons.jmx.JmxMediatorSpec._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import javax.management._

/** @author Christian Schlichtherle */
class JmxMediatorSpec extends AnyWordSpec {

  private trait Fixture {

    val mediator = new TestMediator

    def name = mediator.nameBuilder(classOf[Messenger]).get
  }

  "A JmxMediator" should {

    "build the correct object name" in new Fixture {
      name should equal(new ObjectName(mediator.getClass.getPackage.getName + ":type=" + classOf[Messenger].getSimpleName))
    }

    "succeed to register an MBean for the object name" in new Fixture {
      mediator register(name, new Messenger) should equal(true)
    }

    "fail to register another MBean for an equal object name" in new Fixture {
      mediator register(name, new Messenger) should equal(false)
    }

    "succeed to deregister the MBean for an equal object name" in new Fixture {
      mediator deregister name should equal(true)
    }

    "fail to deregister an MBean for any other object name" in new Fixture {
      mediator deregister new ObjectName(":type=unknown") should equal(false)
    }
  }
}

private object JmxMediatorSpec {

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
