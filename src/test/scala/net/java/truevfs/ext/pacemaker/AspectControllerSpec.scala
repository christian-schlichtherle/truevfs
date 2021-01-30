/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truecommons.cio.{Entry, _}
import net.java.truevfs.ext.pacemaker.AspectController.Op
import net.java.truevfs.ext.pacemaker.AspectControllerSpec.TestController
import net.java.truevfs.kernel.spec._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

/** @author Christian Schlichtherle */
class AspectControllerSpec extends AnyWordSpec {

  def calling: AfterWord = afterWord("calling")

  private trait Fixture1 {

    val delegate = mock[FsController]
    when(delegate.getModel) thenReturn mock[FsModel]
    val controller = spy(new TestController(delegate))
  }

  private trait Fixture2 extends Fixture1 {

    val delegateSocket = mock[InputSocket[Entry]]
    when(delegate.input(null, null).asInstanceOf[InputSocket[Entry]]) thenReturn delegateSocket

    val socket = controller.input(null, null)
    verify(delegate).input(null, null)
  }

  private trait Fixture3 extends Fixture1 {

    val delegateSocket = mock[OutputSocket[Entry]]
    when(delegate.output(null, null, null).asInstanceOf[OutputSocket[Entry]])
      .thenReturn(delegateSocket)

    val socket = controller.output(null, null, null)
    verify(delegate).output(null, null, null)
  }

  "An AspectController" should {
    "apply its aspect" when calling {

      "node(**)" in new Fixture1 {
        controller.node(null, null)
        verify(controller).apply(any())
        verify(delegate).node(null, null)
      }

      "checkAccess(**)" in new Fixture1 {
        controller.checkAccess(null, null, null)
        verify(controller).apply(any())
        verify(delegate).checkAccess(null, null, null)
      }

      "setReadOnly(**)" in new Fixture1 {
        controller.setReadOnly(null, null)
        verify(controller).apply(any())
        verify(delegate).setReadOnly(null, null)
      }

      "setTime(*, *, *)" in new Fixture1 {
        controller.setTime(null, null, null)
        verify(controller).apply(any())
        verify(delegate).setTime(null, null, null)
      }

      "setTime(*, *, *, *)" in new Fixture1 {
        controller.setTime(null, null, null, 0)
        verify(controller).apply(any())
        verify(delegate).setTime(null, null, null, 0)
      }

      "input(**)" when calling {

        "target()" in new Fixture2 {
          socket.target()
          verify(controller).apply(any())
          verify(delegateSocket).target()
        }

        "stream(*)" in new Fixture2 {
          socket.stream(null)
          verify(controller).apply(any())
          verify(delegateSocket).stream(null)
        }

        "channel(*)" in new Fixture2 {
          socket.channel(null)
          verify(controller).apply(any())
          verify(delegateSocket).channel(null)
        }
      }

      "output(**)" when calling {

        "target()" in new Fixture3 {
          socket.target()
          verify(controller).apply(any())
          verify(delegateSocket).target()
        }

        "stream(*)" in new Fixture3 {
          socket.stream(null)
          verify(controller).apply(any())
          verify(delegateSocket).stream(null)
        }

        "channel(*)" in new Fixture3 {
          socket.channel(null)
          verify(controller).apply(any())
          verify(delegateSocket).channel(null)
        }
      }

      "make(**)" in new Fixture3 {
        controller.make(null, null, null, null)
        verify(controller).apply(any())
        verify(delegate).make(null, null, null, null)
      }

      "unlink(**)" in new Fixture3 {
        controller.unlink(null, null)
        verify(controller).apply(any())
        verify(delegate).unlink(null, null)
      }
    }
  }
}

private object AspectControllerSpec {

  class TestController(controller: FsController) extends AspectController(controller) {

    override def apply[V](op: Op[V]): V = op.call()
  }

}
