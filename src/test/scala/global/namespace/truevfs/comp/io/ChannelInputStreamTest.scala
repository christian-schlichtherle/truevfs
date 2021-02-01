package global.namespace.truevfs.comp.io

import global.namespace.truevfs.comp.io.ChannelInputStreamTest._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.io._
import java.nio.channels._
import scala.util.Random

/** @author Christian Schlichtherle */
class ChannelInputStreamTest
  extends AnyWordSpec
    with ScalaCheckDrivenPropertyChecks
    with ParallelTestExecution {

  "A channel input stream" when {
    "constructed with a null channel parameteter" should {
      "report a null pointer exception" in {
        intercept[NullPointerException] {
          new ChannelInputStream(null)
        }
      }
    }

    "asked to use the mark" should {
      "confirm that it supports marking" in {
        stream.markSupported should equal(true)
      }

      "report an IOException if the mark isn't defined" in {
        intercept[IOException] {
          stream.reset()
        }
      }

      "accept setting the mark with any readLimit" in {
        forAll("readLimit") { readLimit: Int =>
          val in = stream
          in mark readLimit
          verify(in.channel).position
          ()
        }
      }

      "reset to the mark no matter how much data was skipped" in {
        forAll((Gen.choose(1, Integer.MAX_VALUE), "size")) { size: Int =>
          whenever(0 < size) {
            val in = stream
            val channel = in.channel
            when(channel.size).thenReturn(size)
            in.mark(Random.nextInt())
            in.skip(size)
            verify(channel).position(size)
            in.reset()
            verify(channel).position(0)
            intercept[IOException] {
              in.reset()
            }
          }
        }
      }

      "refuse to reset if the channel fails to report its position" in {
        val in = stream
        val channel = in.channel
        when(channel.position).thenThrow(new IOException)
        in.mark(Random.nextInt())
        intercept[IOException] {
          in.reset()
        }
        in.markSupported should equal(false)
      }

      "refuse to reset if the channel fails to change its position" in {
        val in = stream
        val channel = in.channel
        doThrow(new IOException).when(channel).position(any())
        in.mark(Random.nextInt())
        intercept[IOException] {
          in.reset()
        }
        in.markSupported should equal(false)
      }
    }
  }
}

object ChannelInputStreamTest {
  private def stream = new ChannelInputStream(mock[SeekableByteChannel])
}
