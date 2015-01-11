package net.java.truevfs.kernel.impl

import java.util.concurrent.Callable

import net.java.truecommons.shed.ConcurrencyUtils
import net.java.truecommons.shed.ConcurrencyUtils.TaskFactory
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.Mockito.inOrder
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar.mock

/**
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
private class ShutdownFuseTest extends WordSpec {

  trait Fixture {
    val registry = mock[ShutdownFuse.ThreadRegistry]
    val io = inOrder(registry)
    private[this] var executed: Boolean = _
    val fuse: ShutdownFuse = ShutdownFuse(armed = true, registry) {
      fuse disarm () // must cause no harm!
      executed = true
    }

    def blowUp() {
      executed should equal (false)
      fuse blowUp ()
      executed should equal (true)
    }
  }

  "A shutdown fuse" when {
    "just constructed" should {
      "add the shutdown hook" in new Fixture {
        io verify registry add any[Thread]
        io verifyNoMoreInteractions ()
      }
    }

    "disarmed" should {
      "add and remove the shutdown hook" in new Fixture {
        fuse disarm ()
        io verify registry add any[Thread]
        io verify registry remove any[Thread]
        io verifyNoMoreInteractions ()
      }
    }

    "armed" should {
      "add the shutdown hook" in new Fixture {
        fuse arm ()
        io verify registry add any[Thread]
        io verifyNoMoreInteractions ()
      }
    }

    "disarmed and armed again" should {
      "add, remove and add the shutdown hook again" in new Fixture {
        fuse disarm ()
        fuse arm ()
        io verify registry add any[Thread]
        io verify registry remove any[Thread]
        io verify registry add any[Thread]
        io verifyNoMoreInteractions ()
      }
    }

    "armed and disarmed again" should {
      "add and remove the shutdown hook" in new Fixture {
        fuse arm ()
        fuse disarm ()
        io verify registry add any[Thread]
        io verify registry remove any[Thread]
        io verifyNoMoreInteractions ()
      }
    }

    "blown-up" should {
      "register and execute the shutdown hook" in new Fixture {
        blowUp()
        io verify registry add any[Thread]
        io verifyNoMoreInteractions ()
      }
    }

    "disarmed and armed again concurrently" should {
      "not throw an exception" in {
        // Use the DefaultThreadRegistry
        val fuse = ShutdownFuse { /* no-op */ }
        runConcurrently(ConcurrencyUtils.NUM_CPU_THREADS) { _ =>
          for (i <- 1 to 10) {
            fuse disarm()
            fuse arm()
          }
        }
      }
    }

    def runConcurrently(numThreads: Int)(fun: Int => Unit) {
      startConcurrently(numThreads)(fun) join ()
    }

    def startConcurrently(numThreads: Int)(fun: Int => Unit) = {
      ConcurrencyUtils start (
        numThreads,
        new TaskFactory {
          override def newTask(threadNum: Int) = new Callable[Unit] {
            override def call() { fun(threadNum) }
          }
        }
      )
    }
  }
}
