/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import org.junit._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._

/**
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class ArchiveManagerServiceSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  "An archive file system manager service" should {
    val service = new ArchiveManagerService

    "provide an archive file system manager" in {
      service.getManager should not be (null)
    }

    "have priority -100" in {
      service.getPriority should be (-100)
    }
  }
}
