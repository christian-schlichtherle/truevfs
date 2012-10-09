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
class PaceControllerTest extends WordSpec with ShouldMatchers with MockitoSugar with OneInstancePerTest {

  "A PaceController" should {
    val manager = mock[PaceManager]
    val back = mock[FsController]
    when(back.getModel) thenReturn mock[FsModel]
    val front = spy(new PaceController(manager, back))

    "call the PaceManager" when {
      "apply()ing its aspect" in {
        val result = new AnyRef
        front apply (() => result) should be theSameInstanceAs (result)
        verify(manager) retain back
        verify(manager) accessed back
      }
    }

    "not apply() its aspect to sync(*)" in {
      front sync null
      verify(front, never()) apply any()
      verify(back) sync null
    }
  }
}
