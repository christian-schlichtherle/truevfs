/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight.stats

import global.namespace.fun.io.bios.BIOS
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/** @author Christian Schlichtherle */
class FsStatsSpec extends AnyWordSpec {

  private val original = FsStats
    .getInstance
    .logRead(1000 * 1000, 1024, 1)
    .logWrite(1000 * 1000, 1024, 1)
    .logSync(1000 * 1000 * 1000, 1)

  "The file system statistics" should {
    "be serializable" in {
      val clone = BIOS.clone(original)
      clone should not be theSameInstanceAs(original)
      clone shouldBe original
    }
  }
}
