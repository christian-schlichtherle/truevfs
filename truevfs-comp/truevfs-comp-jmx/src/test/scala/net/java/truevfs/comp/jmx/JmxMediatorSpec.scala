/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx

import javax.management._
import org.junit.runner._
import org.mockito._
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => meq}
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.mock.MockitoSugar.mock
import org.slf4j._
import JmxMediatorSpec._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class JmxMediatorSpec
extends WordSpec with ShouldMatchers with OneInstancePerTest {

  "A JmxMediator" should {
    val mediator = new TestMediator
    val logger = mediator.logger
    def name = mediator.nameBuilder(classOf[Messenger]).get
    val exception = ArgumentCaptor forClass classOf[JMException]

    "build the correct object name" in {
      name should equal (new ObjectName(mediator.getClass.getPackage.getName + ":type=" + classOf[Messenger].getSimpleName))
    }

    "succeed to register an MBean for the object name" in {
      mediator register (name, new Messenger)
      verifyNoMoreInteractions(logger)
    }

    "fail to register another MBean for an equal object name" in {
      mediator register (name, new Messenger)
      verify(logger) warn (anyString(), meq(name))
      verify(logger) trace (anyString(), exception.capture)
      verifyNoMoreInteractions(logger)
    }

    "succeed to deregister the MBean for an equal object name" in {
      mediator deregister name
      verifyNoMoreInteractions(logger)
    }

    "fail to deregister an MBean for any other object name" in {
      val name = new ObjectName(":type=unknown")
      mediator deregister name
      verify(logger) warn (anyString(), meq(name))
      verify(logger) trace (anyString(), exception.capture)
      verifyNoMoreInteractions(logger)
    }
  }
}

private object JmxMediatorSpec {
  private[this] val mbs = MBeanServerFactory.newMBeanServer
  
  private class TestMediator extends JmxMediator[TestMediator] {
    logger = mock[Logger]

    override def getMBeanServer = mbs
  }

  trait MessengerMXBean { def getMessage: String }

  class Messenger extends MessengerMXBean {
    override def getMessage = "Hello world!"
  }
}
