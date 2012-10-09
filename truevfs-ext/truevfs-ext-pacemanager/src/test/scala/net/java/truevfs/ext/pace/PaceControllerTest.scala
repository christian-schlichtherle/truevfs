/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import net.java.truecommons.shed.ControlFlowException
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
class PaceControllerTest extends WordSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  "A PaceController" when {
    val manager = mock[PaceManager]
    val delegate = mock[FsController]
    when(delegate.getModel) thenReturn mock[FsModel]
    val controller = spy(new PaceController(manager, delegate))

    "apply()ing its aspect" should {
      "call only PaceManager.retain(*) if the operation fails" in {
        intercept[ControlFlowException] {
          controller apply (() => throw new ControlFlowException)
        }
        verify(manager) retain delegate
        verifyNoMoreInteractions(manager)
      }

      "call only PaceManager.retain(*) and .accessed(*) if the operation succeeded" in {
        val result = new AnyRef
        controller apply (() => result) should be theSameInstanceAs (result)
        verify(manager) retain delegate
        verify(manager) accessed delegate
        verifyNoMoreInteractions(manager)
      }
    }

    "calling sync(*)" should {
      "not apply() its aspect" in {
        controller sync null
        verify(controller, never()) apply any()
        ()
      }

      "forward the call to the decorated controller" in {
        controller sync null
        verify(delegate) sync null
      }
    }
  }
}
