/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.io

import java.nio.ByteOrder._
import java.nio._

import org.junit.runner.RunWith
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ParallelTestExecution, WordSpec}

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class PowerBufferTest extends WordSpec with ParallelTestExecution {

  "A power buffer" when {
    "allocated on the heap" should {
      val pb: PowerBuffer[_] = PowerBuffer allocate 1

      "provide access to its adapted byte buffer" in {
        pb.buffer should not be null
      }

      "not be direct" in {
        pb.isDirect should equal (false)
      }

      "provide access to its underlying array" in {
        pb.array should not be null
      }

      "be a mutable adapter of its byte buffer" in {
        testAdapter(pb)
      }

      "provide mutable and immutable views" in {
        testViews(pb)
      }
    }

    "not allocated on the heap" should {
      val pb: PowerBuffer[_] = PowerBuffer allocateDirect 1

      "provide access to its adapted byte buffer" in {
        pb.buffer should not be null
      }

      "be direct" in {
        pb.isDirect should equal (true)
      }

      "not provide access to its underlying array" in {
        intercept[UnsupportedOperationException] { pb.array }
      }

      "be a mutable adapter of its byte buffer" in {
        testAdapter(pb)
      }

      "provide mutable and immutable views" in {
        testViews(pb)
      }
    }

    "wrapping an array" should {
      val array = new Array[Byte](1)
      val pb: PowerBuffer[_] = PowerBuffer wrap array

      "provide access to its adapted byte buffer" in {
        pb.buffer should not be null
      }

      "not be direct" in {
        pb.isDirect should equal (false)
      }

      "provide access to its underlying array" in {
        pb.array should (be theSameInstanceAs array)
      }

      "be a mutable adapter of its byte buffer" in {
        testAdapter(pb)
      }

      "provide mutable and immutable views" in {
        testViews(pb)
      }
    }

    "wrapping a part of an array" should {
      val array = new Array[Byte](3)
      val pb: PowerBuffer[_] = PowerBuffer wrap (array, 1, 1)

      "provide access to its adapted byte buffer" in {
        pb.buffer should not be null
      }

      "not be direct" in {
        pb.isDirect should equal (false)
      }

      "provide access to its underlying array" in {
        pb.array should (be theSameInstanceAs array)
      }

      "be a mutable adapter of its byte buffer" in {
        testAdapter(pb)
      }

      "provide mutable and immutable views" in {
        testViews(pb)
      }
    }

    "wrapping a byte buffer" should {
      val buf = ByteBuffer allocate 1
      val pb: PowerBuffer[_] = PowerBuffer wrap buf

      "provide access to its adapted byte buffer" in {
        pb.buffer should be theSameInstanceAs buf
      }

      "not be direct" in {
        pb.isDirect should equal (false)
      }

      "provide access to its underlying array" in {
        pb.array should not equal null
      }

      "be a mutable adapter of its byte buffer" in {
        testAdapter(pb)
      }

      "provide mutable and immutable views" in {
        testViews(pb)
      }
    }

    "viewed as mutable and read-only" should {
      val pb: PowerBuffer[_] = PowerBuffer allocate 1
      val rob = pb.asMutableBuffer.asReadOnlyBuffer

      "not support write access" in {
        intercept[UnsupportedOperationException] { rob put (0, 0) }
      }

      "not return itself when asked to be set read-only again" in {
        rob.asReadOnlyBuffer should not be theSameInstanceAs (rob)
      }
    }

    "viewed as immutable and read-only" should {
      val pb: PowerBuffer[_] = PowerBuffer allocate 1
      val rob = pb.asImmutableBuffer.asReadOnlyBuffer

      "not support write access" in {
        intercept[UnsupportedOperationException] { rob put (0, 0) }
      }

      "not return itself when asked to be set read-only again" in {
        rob.asReadOnlyBuffer should not be theSameInstanceAs (rob)
      }
    }

    "set to little-endian" should {
      val bb = (MutableBuffer allocate 1).asReadOnlyBuffer.littleEndian.asImmutableBuffer

      "retain this byte order" when {
        "cloned" in {
          bb.clone.order() should be (LITTLE_ENDIAN)
        }

        "viewed as a mutable buffer" in {
          bb.asMutableBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as an immutable buffer" in {
          bb.asMutableBuffer.asImmutableBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as a char buffer" in {
          bb.asCharBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as a short buffer" in {
          bb.asShortBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as an int buffer" in {
          bb.asIntBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as a long buffer" in {
          bb.asLongBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as a float buffer" in {
          bb.asFloatBuffer.order() should be (LITTLE_ENDIAN)
        }

        "viewed as a double buffer" in {
          bb.asDoubleBuffer.order() should be (LITTLE_ENDIAN)
        }
      }
    }

    "set to little-endian" should {
      val bb = (MutableBuffer allocate 1).asReadOnlyBuffer.littleEndian.asImmutableBuffer

      "set the byte order to big-endian" when {
        "sliced" in {
          bb.slice.order() should be (BIG_ENDIAN)
        }

        "duplicated" in {
          bb.duplicate.order() should be (BIG_ENDIAN)
        }

        "viewed as read-only" in {
          bb.asReadOnlyBuffer.order() should be (BIG_ENDIAN)
        }
      }
    }
  }

  def testAdapter(pb: PowerBuffer[_]) {
    val buf = pb.buffer
    pb position 1
    buf.position should be (1)
    buf position 0
    pb.position should be (0)
  }

  def testViews(pb: PowerBuffer[_]) {
    pb position 0

    val mb = pb.asMutableBuffer
    mb should be theSameInstanceAs pb

    val ib = pb.asImmutableBuffer
    ib should not be theSameInstanceAs (pb)

    mb.asMutableBuffer should be theSameInstanceAs mb
    ib.asImmutableBuffer should be theSameInstanceAs ib
    mb.asImmutableBuffer should not be theSameInstanceAs (mb)
    ib.asMutableBuffer should not be theSameInstanceAs (ib)

    mb position 1
    ib.position() should be (0)
    intercept[UnsupportedOperationException] { ib position 1 }
    ib.asMutableBuffer position 1
    ib.position() should be (0)
  }
}
