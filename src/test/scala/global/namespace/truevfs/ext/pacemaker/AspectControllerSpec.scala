/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.pacemaker

import global.namespace.truevfs.commons.cio.{Entry, _}
import global.namespace.truevfs.ext.pacemaker.AspectController.Op
import global.namespace.truevfs.kernel.api._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.Ignore
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.Optional

/** @author Christian Schlichtherle */
@Ignore
class AspectControllerSpec extends AnyWordSpec {

  def calling: AfterWord = afterWord("calling")

  private trait Fixture1 {

    val delegate: FsController = mock[FsController]
    when(delegate.getModel).thenReturn(mock[FsModel])
    val controller: TestController = spy(new TestController(delegate))
  }

  private trait Fixture2 extends Fixture1 {

    val delegateSocket: InputSocket[Entry] = mock[InputSocket[Entry]]
    when(delegate.input(any, any).asInstanceOf[InputSocket[Entry]]).thenReturn(delegateSocket)

    val socket: InputSocket[_ <: Entry] = controller.input(null, null)
    verify(delegate).input(isNull, isNull)
  }

  private trait Fixture3 extends Fixture1 {

    val delegateSocket: OutputSocket[Entry] = mock[OutputSocket[Entry]]
    when(delegate.output(any, any, any).asInstanceOf[OutputSocket[Entry]]).thenReturn(delegateSocket)

    val socket: OutputSocket[_ <: Entry] = controller.output(null, null, null)
    verify(delegate).output(isNull, isNull, isNull)
  }

  "An AspectController" should {
    "apply its aspect" when calling {

      "node(**)" in new Fixture1 {
        controller.node(null, null)
        verify(controller).apply(any)
        verify(delegate).node(isNull, isNull)
      }

      "checkAccess(**)" in new Fixture1 {
        controller.checkAccess(null, null, null)
        verify(controller).apply(any)
        verify(delegate).checkAccess(isNull, isNull, isNull)
      }

      "setReadOnly(**)" in new Fixture1 {
        controller.setReadOnly(null, null)
        verify(controller).apply(any)
        verify(delegate).setReadOnly(isNull, isNull)
      }

      "setTime(*, *, *)" in new Fixture1 {
        controller.setTime(null, null, null)
        verify(controller).apply(any)
        verify(delegate).setTime(isNull, isNull, isNull)
      }

      "setTime(*, *, *, *)" in new Fixture1 {
        controller.setTime(null, null, null, 0)
        verify(controller).apply(any)
        verify(delegate).setTime(isNull, isNull, isNull, ArgumentMatchers.eq(0L))
      }

      "input(**)" when calling {

        "target()" in new Fixture2 {
          socket.getTarget
          verify(controller).apply(any)
          verify(delegateSocket).getTarget
        }

        "stream(*)" in new Fixture2 {
          socket.stream(Optional.empty())
          verify(controller).apply(any)
          verify(delegateSocket).stream(Optional.empty())
        }

        "channel(*)" in new Fixture2 {
          socket.channel(Optional.empty())
          verify(controller).apply(any)
          verify(delegateSocket).channel(Optional.empty())
        }
      }

      "output(**)" when calling {

        "target()" in new Fixture3 {
          socket.getTarget
          verify(controller).apply(any)
          verify(delegateSocket).getTarget
        }

        "stream(*)" in new Fixture3 {
          socket.stream(Optional.empty())
          verify(controller).apply(any)
          verify(delegateSocket).stream(Optional.empty())
        }

        "channel(*)" in new Fixture3 {
          socket.channel(Optional.empty())
          verify(controller).apply(any)
          verify(delegateSocket).channel(Optional.empty())
        }
      }

      "make(**)" in new Fixture3 {
        controller.make(null, null, null, null)
        verify(controller).apply(any)
        verify(delegate).make(isNull, isNull, isNull, isNull)
      }

      "unlink(**)" in new Fixture3 {
        controller.unlink(null, null)
        verify(controller).apply(any)
        verify(delegate).unlink(isNull, isNull)
      }
    }
  }
}

class TestController(controller: FsController) extends AspectController(controller) {

  override def apply[V](op: Op[V]): V = op.call()
}
