package net.java.truecommons.io

import org.scalatest.ParallelTestExecution
import org.scalatest.wordspec.AnyWordSpec

/** @author Christian Schlichtherle */
class ChannelOutputStreamTest
extends AnyWordSpec
   with ParallelTestExecution {

  "A channel output stream" when {
    "constructed with a null channel parameteter" should {
      "report a null pointer exception" in {
        intercept[NullPointerException] { new ChannelOutputStream(null) }
      }
    }
  }
}
