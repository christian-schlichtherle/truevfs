/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip

import net.java.truecommons.io._
import org.junit.runner._
import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.junit._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class DefaultExtraFieldSpec
extends WordSpec with ShouldMatchers with ParallelTestExecution {

  "A default extra field" when {
    "constructed with wrong parameters" should {
      "throw an IllegalArgumentException 1" in {
        intercept[IllegalArgumentException] {
          new DefaultExtraField(UShort.MIN_VALUE - 1, 0)
        }
      }

      "throw an IllegalArgumentException 2" in {
        intercept[IllegalArgumentException] {
          new DefaultExtraField(UShort.MAX_VALUE + 1, 0)
        }
      }

      "throw an IllegalArgumentException 3" in {
        intercept[IllegalArgumentException] {
          new DefaultExtraField(0, UShort.MIN_VALUE - 1)
        }
      }

      "throw an IllegalArgumentException 4" in {
        intercept[IllegalArgumentException] {
          new DefaultExtraField(0, UShort.MAX_VALUE + 1)
        }
      }
    }

    "constructed with correct parameters" should {
      val field = new DefaultExtraField(UShort.MAX_VALUE, UShort.MAX_VALUE - 4)

      "return the correct Total Size" in {
        field.getTotalSize should be (UShort.MAX_VALUE)
      }

      "return the correct Header Id" in {
        field.getHeaderId should be (UShort.MAX_VALUE)
      }

      "return the correct Data Size" in {
        field.getDataSize should be (UShort.MAX_VALUE - 4)
      }

      "return a Total Block with the correct Total Size" in {
        field.totalBlock should have ('remaining (UShort.MAX_VALUE))
      }

      "return a different Total Block on each call" in {
        field.totalBlock should not be theSameInstanceAs (field.totalBlock)
      }

      "return an equal Total Block on each call" in {
        field.totalBlock should equal (field.totalBlock)
      }

      "return a Data Block with the correct Data Size" in {
        field.dataBlock should have ('remaining (UShort.MAX_VALUE - 4))
      }

      "return a different Data Block on each call" in {
        field.dataBlock should not be theSameInstanceAs (field.dataBlock)
      }

      "return an equal Data Block on each call" in {
        field.dataBlock should equal (field.dataBlock)
      }
    }

    "constructed with a valid immutable buffer" should {
      val buffer = (MutableBuffer allocate 4).littleEndian
      val field = new DefaultExtraField(buffer)

      "advance the position of the buffer" in {
        buffer.position should be (4)
      }

      "return the correct Total Size" in {
        field.getTotalSize should be (4)
      }

      "return the correct Header Id" in {
        field.getHeaderId should be (0)
      }

      "be a view of the buffer" in {
        buffer putShort (0, UShort.MAX_VALUE.asInstanceOf[Short])
        field.getHeaderId should be (UShort.MAX_VALUE)
      }

      "return the correct Data Size" in {
        field.getDataSize should be (0)
      }

      "return a Total Block with the correct Total Size" in {
        field.totalBlock should have ('remaining (4))
      }

      "return a different Total Block on each call" in {
        field.totalBlock should not be theSameInstanceAs (field.totalBlock)
      }

      "return an equal Total Block on each call" in {
        field.totalBlock should equal (field.totalBlock)
      }

      "return a Data Block with the correct Data Size" in {
        field.dataBlock should have ('remaining (0))
      }

      "return a different Data Block on each call" in {
        field.dataBlock should not be theSameInstanceAs (field.dataBlock)
      }

      "return an equal Data Block on each call" in {
        field.dataBlock should equal (field.dataBlock)
      }
    }
  }
}
