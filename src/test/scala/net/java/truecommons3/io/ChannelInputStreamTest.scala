package net.java.truecommons3.io

import java.io._
import java.nio.channels._

import net.java.truecommons3.io.ChannelInputStreamTest._
import org.junit.runner.RunWith
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar.mock
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ParallelTestExecution, WordSpec}

import scala.util.Random

@RunWith(classOf[JUnitRunner])
/** @author Christian Schlichtherle */
class ChannelInputStreamTest
extends WordSpec
   with PropertyChecks
   with ParallelTestExecution {

  "A channel input stream" when {
    "constructed with a null channel parameteter" should {
      "report a null pointer exception" in {
        intercept[NullPointerException] { new ChannelInputStream(null) }
      }
    }

    "asked to use the mark" should {
      "confirm that it supports marking" in {
        stream.markSupported should equal (true)
      }

      "report an IOException if the mark isn't defined" in {
        intercept[IOException] { stream reset () }
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
          whenever (0 < size) {
            val in = stream
            val channel = in.channel
            when(channel.size) thenReturn size
            in mark Random.nextInt
            in skip size
            verify(channel) position size
            in reset ()
            verify(channel) position 0
            intercept[IOException] { in reset () }
          }
        }
      }

      "refuse to reset if the channel fails to report its position" in {
        val in = stream
        val channel = in.channel
        when(channel.position) thenThrow new IOException
        in mark Random.nextInt
        intercept[IOException] { in reset () }
        in.markSupported should equal (false)
      }

      "refuse to reset if the channel fails to change its position" in {
        val in = stream
        val channel = in.channel
        doThrow(new IOException) when channel position any()
        in mark Random.nextInt
        intercept[IOException] { in reset () }
        in.markSupported should equal (false)
      }
    }
  }
}

object ChannelInputStreamTest {
  private def stream = new ChannelInputStream(mock[SeekableByteChannel])
}
