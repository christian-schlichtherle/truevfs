package global.namespace.truevfs.comp.io

import org.scalatest.wordspec.AnyWordSpec

/** @author Christian Schlichtherle */
class ChannelOutputStreamSpec extends AnyWordSpec {

  "A channel output stream" when {
    "constructed with a null channel parameteter" should {
      "report a null pointer exception" in {
        intercept[NullPointerException] { new ChannelOutputStream(null) }
      }
    }
  }
}
