/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

import global.namespace.fun.io.bios.BIOS
import org.scalatest.Matchers._
import org.scalatest._

/** @author Christian Schlichtherle */
class FsStatsSpec extends WordSpec {

  private val original = FsStats
    .getInstance
    .logRead(1000 * 1000, 1024, 1)
    .logWrite(1000 * 1000, 1024, 1)
    .logSync(1000 * 1000 * 1000, 1)

  "File system statistics" should {
    "be serializable" in {
      val clone = BIOS.clone(original)
      clone should not be theSameInstanceAs(original)
      clone shouldBe original
    }
  }
}
