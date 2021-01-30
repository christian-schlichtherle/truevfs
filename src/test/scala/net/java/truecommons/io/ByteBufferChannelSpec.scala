/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.io

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import java.nio._
import java.nio.channels._
import scala.util._

object ByteBufferChannelSpec {
  val bufferSize = 128
}

/**
 * @author Christian Schlichtherle
 */
class ByteBufferChannelSpec
  extends AnyWordSpec with BeforeAndAfter {

  import ByteBufferChannelSpec._

  var array: Array[Byte] = _

  before {
    array = new Array[Byte](bufferSize)
    Random nextBytes array
  }

  def newReadOnlyByteBufferChannel =
    new ByteBufferChannel((ByteBuffer wrap array)
      .asReadOnlyBuffer
      .position(64) // spoiling attempt
      .asInstanceOf[ByteBuffer])

  def newEmptyByteBufferChannel =
    new ByteBufferChannel(ByteBuffer allocate 0)

  def newClosedByteBufferChannel = {
    val channel = newEmptyByteBufferChannel
    channel.isOpen should be(true)
    channel.close()
    channel.isOpen should be(false)
    channel
  }

  "A ByteBufferChannel" when {
    "constructed" should {
      "accept no null ByteBuffer" in {
        intercept[NullPointerException] {
          new ByteBufferChannel(null)
        }
      }
    }

    def not = afterWord("not")

    "closed" should not {
      "read(ByteBuffer)" in {
        intercept[ClosedChannelException](newClosedByteBufferChannel read (ByteBuffer allocate bufferSize))
      }

      "write(ByteBufer)" in {
        intercept[ClosedChannelException](newClosedByteBufferChannel write (ByteBuffer wrap array))
      }

      "position()" in {
        intercept[ClosedChannelException](newClosedByteBufferChannel.position)
      }

      "position(long)" in {
        intercept[ClosedChannelException](newClosedByteBufferChannel position 0)
      }

      "size()" in {
        intercept[ClosedChannelException](newClosedByteBufferChannel.size)
      }

      "truncate(long)" in {
        intercept[ClosedChannelException](newClosedByteBufferChannel truncate 0)
      }
    }

    "given a read-only ByteBuffer" should {
      "have buffer duplicate position zero" in {
        newReadOnlyByteBufferChannel.getBuffer.position should be(0)
      }

      "have the size() of the given buffer" in {
        newReadOnlyByteBufferChannel should have size array.length
      }

      "have position() zero" in {
        newReadOnlyByteBufferChannel.position should be(0)
      }

      "support reading the ByteBuffer and repositioning repeatedly" in {
        val channel = newReadOnlyByteBufferChannel
        for (i <- 1 to 2) {
          val data = ByteBuffer allocate array.length
          channel read data should be(array.length)
          data.array should equal(array)
          channel.position should be(array.length)
          channel read (ByteBuffer allocate bufferSize) should be(-1)
          channel.position should be(array.length)
          channel position 0
        }
      }

      "throw a NonWritableChannelException on write(ByteBuffer)" in {
        intercept[NonWritableChannelException] {
          newReadOnlyByteBufferChannel write (ByteBuffer allocate bufferSize)
        }
      }

      "throw a NonWritableChannelException on truncate(long)" in {
        intercept[NonWritableChannelException] {
          newReadOnlyByteBufferChannel truncate 0
        }
      }
    }

    "given an empty ByteBuffer" should {
      "have size() zero" in {
        newEmptyByteBufferChannel should have size 0
      }

      "have position() zero" in {
        newEmptyByteBufferChannel.position() should be(0)
      }

      "report EOF on read(ByteBuffer)" in {
        newEmptyByteBufferChannel read (ByteBuffer allocate bufferSize) should be(-1)
      }

      "support writing, rewinding, rereading and truncating the ByteBuffer repeatedly" in {
        val channel = newEmptyByteBufferChannel
        for (i <- 1 to 2) {
          // Write.
          channel write (ByteBuffer wrap array) should be(array.length)
          channel.position should be(array.length)
          channel should have size array.length

          // Rewind.
          channel position 0
          channel should have size array.length

          // Reread.
          val copy = ByteBuffer allocate array.length
          channel read copy should be(array.length)
          copy.array should equal(array)
          channel.position should be(array.length)
          channel should have size array.length

          // Check EOF.
          channel read (ByteBuffer allocate bufferSize) should be(-1)
          channel.position should be(array.length)
          channel should have size array.length

          // Truncate.
          channel truncate 0
          channel.position should be(0)
          channel should have size 0
        }
      }
    }
  }
}
