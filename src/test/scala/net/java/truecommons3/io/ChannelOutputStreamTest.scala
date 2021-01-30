package net.java.truecommons3.io

import org.junit.runner.RunWith
import org.scalatest.{ParallelTestExecution, WordSpec}
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
/** @author Christian Schlichtherle */
class ChannelOutputStreamTest
extends WordSpec
   with ParallelTestExecution {

  "A channel output stream" when {
    "constructed with a null channel parameteter" should {
      "report a null pointer exception" in {
        intercept[NullPointerException] { new ChannelOutputStream(null) }
      }
    }
  }
}
