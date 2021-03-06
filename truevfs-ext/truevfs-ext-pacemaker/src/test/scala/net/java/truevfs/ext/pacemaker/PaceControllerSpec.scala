/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import net.java.truecommons.cio.Entry
import net.java.truevfs.kernel.spec._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar.mock

import java.io.IOException
import java.net.URI

/** @author Christian Schlichtherle */
class PaceControllerSpec extends WordSpec with OneInstancePerTest {

  "A PaceController" when {
    val manager = mock[PaceManager]
    val mountPoint = new FsMountPoint(new URI("file:///"))
    val model = mock[FsModel]
    when(model.getMountPoint).thenReturn(mountPoint)
    val delegate = mock[FsController]
    when(delegate.getModel).thenReturn(model)
    val controller = new PaceController(manager, delegate)

    "apply()ing its aspect" should {
      def verifyAspect(): Unit = {
        val io = Mockito.inOrder(delegate, manager)
        import io._
        verify(delegate).checkAccess(FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)
        verify(manager).recordAccess(mountPoint)
        verifyNoMoreInteractions()
      }

      "call PaceManager.recordAccess(delegate) if the operation succeeds" in {
        controller.checkAccess(FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)
        verifyAspect()
      }

      "call PaceManager.recordAccess(delegate) even if an IOException is thrown" in {
        when(delegate.checkAccess(FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)) thenThrow new IOException()
        intercept[IOException] {
          controller.checkAccess(FsAccessOptions.NONE, FsNodeName.ROOT, Entry.NO_ACCESS)
        }
        verifyAspect()
      }
    }

    "calling its sync(*) method" should {
      "forward the call to the decorated controller and not apply() its aspect" in {
        val io = Mockito.inOrder(delegate, manager)
        import io._
        controller.sync(null)
        verify(delegate).sync(null)
        verify(manager, never()).recordAccess(mountPoint)
        verifyNoMoreInteractions()
      }
    }
  }
}
