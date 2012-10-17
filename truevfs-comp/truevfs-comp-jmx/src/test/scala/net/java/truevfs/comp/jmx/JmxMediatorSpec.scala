/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.jmx

import javax.management._
import org.junit.runner._
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
class JmxMediatorSpec extends WordSpec with ShouldMatchers {

  "A JmxMediator" should {
    val mediator = new TestMediator
    var name = mediator.nameBuilder(classOf[Messenger]).get

    "build the correct object name" in {
      name should equal (new ObjectName(mediator.getClass.getPackage.getName + ":type=" + classOf[Messenger].getSimpleName))
    }

    "succeed to register an MBean" in {
      mediator register (name, new Messenger)
      verifyNoMoreInteractions(mediator.logger)
    }

    "fail to register another MBean for the same object name" in {
      mediator register (name, new Messenger)
      verify(mediator.logger) warn (anyString(), meq(name))
      verifyNoMoreInteractions(mediator.logger)
    }

    "fail to deregister an MBean for an unknown object name" in {
      name = new ObjectName(":type=unknown")
      mediator deregister name
      verify(mediator.logger) warn (anyString(), meq(name))
      verifyNoMoreInteractions(mediator.logger)
    }
  }
}

private object JmxMediatorSpec {
  private class TestMediator extends JmxMediator[TestMediator] {
    logger = mock[Logger]
  }

  trait MessengerMXBean { def getMessage: String }

  class Messenger extends MessengerMXBean {
    override def getMessage = "Hello world!"
  }
}
