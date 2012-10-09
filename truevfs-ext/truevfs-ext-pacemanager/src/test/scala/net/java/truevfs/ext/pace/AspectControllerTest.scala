/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._
import org.junit.runner._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import AspectControllerTest._
import org.scalatest.mock._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class AspectControllerTest extends WordSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  def calling = afterWord("calling")

  "An AspectController" should {
    val model = mock[FsModel]
    val back = mock[FsController]
    when(back.getModel) thenReturn model
    val front = spy(new TestController(back))

    "apply its aspect" when calling {

      "node(**)" in {
        front.node(null, null)
        verify(front).apply(any())
        verify(back).node(null, null)
      }

      "checkAccess(**)" in {
        front.checkAccess(null, null, null)
        verify(front).apply(any())
        verify(back).checkAccess(null, null, null)
      }

      "setReadOnly(*)" in {
        front.setReadOnly(null)
        verify(front).apply(any())
        verify(back).setReadOnly(null)
      }

      "setTime(*, *, *)" in {
        front.setTime(null, null, null)
        verify(front).apply(any())
        verify(back).setTime(null, null, null)
      }

      "setTime(*, *, *, *)" in {
        front.setTime(null, null, null, 0)
        verify(front).apply(any())
        verify(back).setTime(null, null, null, 0)
      }

      "input(**)" when calling {
        val backSocket = mock[InputSocket[Entry]]
        when(back.input(null, null).asInstanceOf[InputSocket[Entry]])
        .thenReturn(backSocket)

        val frontSocket = front.input(null, null)
        verify(back).input(null, null)

        "target()" in {
          frontSocket target ()
          verify(front).apply(any())
          verify(backSocket) target ()
        }

        "stream(*)" in {
          frontSocket stream (null)
          verify(front).apply(any())
          verify(backSocket) stream (null)
        }

        "channel(*)" in {
          frontSocket channel (null)
          verify(front).apply(any())
          verify(backSocket) channel (null)
        }
      }

      "output(**)" when calling {
        val backSocket = mock[OutputSocket[Entry]]
        when(back.output(null, null, null).asInstanceOf[OutputSocket[Entry]])
        .thenReturn(backSocket)

        val frontSocket = front.output(null, null, null)
        verify(back).output(null, null, null)

        "target()" in {
          frontSocket target ()
          verify(front).apply(any())
          verify(backSocket) target ()
        }

        "stream(*)" in {
          frontSocket stream (null)
          verify(front).apply(any())
          verify(backSocket) stream (null)
        }

        "channel(*)" in {
          frontSocket channel (null)
          verify(front).apply(any())
          verify(backSocket) channel (null)
        }
      }

      "make(**)" in {
        front.make(null, null, null, null)
        verify(front).apply(any())
        verify(back).make(null, null, null, null)
      }

      "unlink(**)" in {
        front.unlink(null, null)
        verify(front).apply(any())
        verify(back).unlink(null, null)
      }

      "sync(*)" in {
        front.sync(null)
        verify(front).apply(any())
        verify(back).sync(null)
      }
    }
  }
}

object AspectControllerTest {
  private class TestController(controller: FsController)
  extends AspectController(controller) {
    override def apply[V](operation: () => V) = operation()
  }
}
