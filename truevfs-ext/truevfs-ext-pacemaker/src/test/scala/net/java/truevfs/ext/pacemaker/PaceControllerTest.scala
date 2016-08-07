/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import java.io.IOException
import java.net.URI

import net.java.truecommons.cio.Entry
import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.mockito.Matchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.mock.MockitoSugar.mock

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class PaceControllerTest extends WordSpec with OneInstancePerTest {

  "A PaceController" when {
    val manager = mock[PaceManager]
    val mountPoint = new FsMountPoint(new URI("file:///"))
    val model = mock[FsModel]
    when(model.getMountPoint) thenReturn mountPoint
    val delegate = mock[FsController]
    when(delegate.getModel) thenReturn model
    val controller = spy(new PaceController(manager, delegate))

    "apply()ing its aspect" should {
      def verifyAspect() {
        val io = Mockito inOrder (delegate, manager)
        import io._
        verify(delegate) checkAccess (FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)
        verify(manager) recordAccess mountPoint
        verifyNoMoreInteractions()
      }

      "call PaceManager.recordAccess(delegate) if the operation succeeds" in {
        controller checkAccess (FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)
        verifyAspect()
      }

      "call PaceManager.recordAccess(delegate) even if an IOException is thrown" in {
        when(delegate checkAccess (FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)) thenThrow new IOException()
        intercept[IOException] {
          controller checkAccess (FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)
        }
        verifyAspect()
      }
    }

    "calling its sync(*) method" should {
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
