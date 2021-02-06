/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class TPath2Spec extends AnyWordSpec {

  "A TPath" should {
    "cut trailing separators" in {
      TPath.cutTrailingSeparators("c://", 3) shouldBe "c:/"
      TPath.cutTrailingSeparators("///", 2) shouldBe "//"
      TPath.cutTrailingSeparators("//", 1) shouldBe "/"
    }
  }
}
