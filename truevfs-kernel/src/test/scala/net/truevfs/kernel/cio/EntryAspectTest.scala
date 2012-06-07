/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.cio

import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry.PosixEntity._
import net.truevfs.kernel.cio.Entry.Size._
import net.truevfs.kernel.cio.Entry.Type._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.mock._
import net.truevfs.kernel._
import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._

@RunWith(classOf[JUnitRunner])
class RichEntryTest extends WordSpec with ShouldMatchers with PropertyChecks {

  "A rich entry" should {
    "properly forward all calls to its decorated entry" in {
      val e = new MockArchiveDriverEntry("foo", FILE)
      val o: ArchiveEntryOps[_] = e
      o.name should be (e.getName)
      o.tµpe should be (e.getType)
      o.dataSize should be (e.getSize(DATA))
      o.storageSize should be (e.getSize(STORAGE))
      o.createTime should be (e.getTime(CREATE))
      o.readTime should be (e.getTime(READ))
      o.writeTime should be (e.getTime(WRITE))
      o.executeTime should be (e.getTime(EXECUTE))
      o.readPermission(USER) should be (Option(e.isPermitted(READ, USER)))
      o.writePermission(GROUP) should be (Option(e.isPermitted(WRITE, GROUP)))
      o.executePermission(OTHER) should be (Option(e.isPermitted(EXECUTE, OTHER)))

      o.readPermission(USER) = None
      o.readPermission(USER) should be (None)
      o.readPermission(USER) = Option(false)
      o.readPermission(USER) should be (Option(false))
      o.readPermission(USER) = Option(true)
      o.readPermission(USER) should be (Option(true))
    }
  }
}
