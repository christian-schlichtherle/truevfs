/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl

import global.namespace.truevfs.commons.shed.ConcurrencyUtils
import global.namespace.truevfs.kernel.impl.ShutdownFuseSpec._
import org.mockito.ArgumentMatchers.any
import org.mockito.InOrder
import org.mockito.Mockito.inOrder
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

/** @author Christian Schlichtherle */
class ShutdownFuseSpec extends AnyWordSpec {

  "A shutdown fuse with a mock thread registry" when {
    "just constructed" should {
      "add the shutdown hook" in new Fixture {
        io.verify(registry).add(any[Thread])
        io.verifyNoMoreInteractions()
      }
    }

    "disarmed" should {
      "add and remove the shutdown hook" in new Fixture {
        fuse.disarm()
        io.verify(registry).add(any[Thread])
        io.verify(registry).remove(any[Thread])
        io.verifyNoMoreInteractions()
      }
    }

    "armed" should {
      "add the shutdown hook" in new Fixture {
        fuse.arm()
        io.verify(registry).add(any[Thread])
        io.verifyNoMoreInteractions()
      }
    }

    "disarmed and armed again" should {
      "add, remove and add the shutdown hook again" in new Fixture {
        fuse.disarm().arm()
        io.verify(registry).add(any[Thread])
        io.verify(registry).remove(any[Thread])
        io.verify(registry).add(any[Thread])
        io.verifyNoMoreInteractions()
      }
    }

    "armed and disarmed again" should {
      "add and remove the shutdown hook" in new Fixture {
        fuse.arm().disarm()
        io.verify(registry).add(any[Thread])
        io.verify(registry).remove(any[Thread])
        io.verifyNoMoreInteractions()
      }
    }

    "blown-up" should {
      "register and execute the shutdown hook" in new Fixture {
        blowUp()
        io.verify(registry).add(any[Thread])
        io.verifyNoMoreInteractions()
      }
    }
  }

  "A shutdown fuse with the default thread registry" when {
    "disarmed and armed again concurrently" should {
      "not throw an exception" in {
        // Use the DefaultThreadRegistry
        val fuse = new ShutdownFuse(() => ())
        runConcurrently(ConcurrencyUtils.NUM_CPU_THREADS) { _ =>
          for (_ <- 1 to 10) {
            fuse.disarm().arm()
          }
        }
      }
    }
  }
}

private object ShutdownFuseSpec {

  def runConcurrently(numThreads: Int)(fun: Int => Unit): Unit = startConcurrently(numThreads)(fun).join()

  def startConcurrently(numThreads: Int)(fun: Int => Unit): ConcurrencyUtils.TaskJoiner = {
    ConcurrencyUtils.start(numThreads, threadNum => () => fun(threadNum))
  }

  trait Fixture {

    val registry: ShutdownFuse.ThreadRegistry = mock[ShutdownFuse.ThreadRegistry]

    val io: InOrder = inOrder(registry)

    private[this] var executed: Boolean = _

    val fuse: ShutdownFuse = {
      new ShutdownFuse(() => {
        fuse.disarm() // must cause no harm!
        executed = true
      }, registry).arm()
    }

    def blowUp(): Unit = {
      executed shouldBe false
      fuse.blowUp()
      executed shouldBe true
    }
  }

}
