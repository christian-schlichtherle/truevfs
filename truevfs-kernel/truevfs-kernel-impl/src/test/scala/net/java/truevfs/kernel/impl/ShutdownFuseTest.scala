package net.java.truevfs.kernel.impl

import org.junit.runner.RunWith
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner

import scala.sys.ShutdownHookThread

/**
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
private class ShutdownFuseTest extends WordSpec {

  trait Simple {
    var executed = false
    val fuse = ShutdownFuse {
      executed = true
    }

    def blowUp(): Unit = {
      val thread = fuse.thread
      thread start ()
      thread join ()
    }
  }

  "A shutdown fuse" when {
    "constructed" should {
      "not run the shutdown hook" in new Simple {
        executed should equal (false)
      }
    }

    "constructed and blown-up" should {
      "run the shutdown hook" in new Simple {
        blowUp()
        executed should equal (true)
      }
    }

    "constructed, disarmed and blown-up" should {
      "not run the shutdown hook" in new Simple {
        fuse disarm ()
        blowUp()
        executed should equal (false)
      }
    }

    "constructed, armed and blown-up" should {
      "run the shutdown hook" in new Simple {
        fuse arm ()
        blowUp()
        executed should equal (true)
      }
    }

    "constructed, disarmed, armed and blown-up" should {
      "run the shutdown hook" in new Simple {
        fuse disarm ()
        fuse arm ()
        blowUp()
        executed should equal (true)
      }
    }

    "constructed, armed, disarmed and blown-up" should {
      "not run the shutdown hook" in new Simple {
        fuse arm ()
        fuse disarm ()
        blowUp()
        executed should equal (false)
      }
    }
  }
}
