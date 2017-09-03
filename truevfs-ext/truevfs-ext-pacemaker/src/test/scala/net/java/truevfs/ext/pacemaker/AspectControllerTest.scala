/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truecommons.cio.{Entry, _}
import net.java.truevfs.ext.pacemaker.AspectControllerTest._
import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.mockito.MockitoSugar.mock

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class AspectControllerTest extends WordSpec with OneInstancePerTest {

  def calling = afterWord("calling")

  "An AspectController" should {
    val delegate = mock[FsController]
    when(delegate.getModel) thenReturn mock[FsModel]
    val controller = spy(new TestController(delegate))

    "apply its aspect" when calling {

      "node(**)" in {
        controller node (null, null)
        verify(controller) apply any()
        verify(delegate) node (null, null)
      }

      "checkAccess(**)" in {
        controller checkAccess (null, null, null)
        verify(controller) apply any()
        verify(delegate) checkAccess (null, null, null)
      }

      "setReadOnly(**)" in {
        controller setReadOnly (null, null)
        verify(controller) apply any()
        verify(delegate) setReadOnly (null, null)
      }

      "setTime(*, *, *)" in {
        controller setTime (null, null, null)
        verify(controller) apply any()
        verify(delegate) setTime (null, null, null)
      }

      "setTime(*, *, *, *)" in {
        controller setTime (null, null, null, 0)
        verify(controller) apply any()
        verify(delegate) setTime (null, null, null, 0)
      }

      "input(**)" when calling {
        val delegateSocket = mock[InputSocket[Entry]]
        when((delegate input(null, null)).asInstanceOf[InputSocket[Entry]])
        .thenReturn(delegateSocket)

        val socket = controller input (null, null)
        verify(delegate) input (null, null)

        "target()" in {
          socket target ()
          verify(controller) apply any()
          verify(delegateSocket) target ()
        }

        "stream(*)" in {
          socket stream null
          verify(controller) apply any()
          verify(delegateSocket) stream null
        }

        "channel(*)" in {
          socket channel null
          verify(controller) apply any()
          verify(delegateSocket) channel null
        }
      }

      "output(**)" when calling {
        val delegateSocket = mock[OutputSocket[Entry]]
        when((delegate output (null, null, null)).asInstanceOf[OutputSocket[Entry]])
        .thenReturn(delegateSocket)

        val socket = controller output (null, null, null)
        verify(delegate) output (null, null, null)

        "target()" in {
          socket target ()
          verify(controller) apply any()
          verify(delegateSocket) target ()
        }

        "stream(*)" in {
          socket stream null
          verify(controller) apply any()
          verify(delegateSocket) stream null
        }

        "channel(*)" in {
          socket channel null
          verify(controller) apply any()
          verify(delegateSocket) channel null
        }
      }

      "make(**)" in {
        controller make (null, null, null, null)
        verify(controller) apply any()
        verify(delegate) make (null, null, null, null)
      }

      "unlink(**)" in {
        controller unlink (null, null)
        verify(controller) apply any()
        verify(delegate) unlink (null, null)
      }

      "sync(*)" in {
        controller sync null
        verify(controller) apply any()
        verify(delegate) sync null
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
