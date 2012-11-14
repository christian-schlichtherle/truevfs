package net.java.truevfs.comp.mmbs

import javax.management._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import MultiplexingMBeanServerSpec._

private object MultiplexingMBeanServerSpec {

  private trait MessengerMBean {
    def getMessage: String
  }

  private class Messenger extends MessengerMBean {
    def getMessage = "Hello world!"
  }

  private val on = new ObjectName("Test:type=Test") // original name
  private val mnp = new ObjectName("Test:type=Test,CLASS_LOADER=*") // modified name pattern
  private val mbean = new Messenger
}

@RunWith(classOf[JUnitRunner])
class MultiplexingMBeanServerSpec extends WordSpec with ShouldMatchers {

  "A multiplexing MBean server" should {
    val ombs = MBeanServerFactory.newMBeanServer
    val mmbs = new MultiplexingMBeanServer(ombs)

    "register the MBean" in {
      mmbs registerMBean (mbean, on)
    }

    "have its qualifier added to the MBean's object name in the original MBean server" in {
      val set = ombs queryNames (mnp, null)
      set should have size (1)
      set.iterator.next getKeyProperty ("CLASS_LOADER") should not be (null)
    }

    "find the registered MBean when quering object names" in {
      val set = mmbs queryNames(on, null)
      set should have size (1)
      set.iterator.next should equal (on)
    }

    "find the registered MBean when quering object instances" in {
      val set = mmbs queryMBeans(on, null)
      set should have size (1)
      set.iterator.next.getObjectName should equal (on)
    }

    "unregister the MBean" in {
      mmbs.unregisterMBean(on)
    }

    "have unregistered the MBean in the original MBean server" in {
      val set = ombs queryNames (mnp, null)
      set should have size (0)
    }
  }
}
