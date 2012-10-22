/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip

import java.util.zip._
import net.java.truecommons.io._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import scala.util._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class ExtraFieldsSpec extends WordSpec with ShouldMatchers with ParallelTestExecution {

  "A collection of extra fields" should {
    val fields = new ExtraFields

    "throw an IllegalArgumentException when deserializing a data block which does not conform to the ZIP File Format Specification" in {
      val rnd = new Random()
      // Make space for a header id (2), a field length (2) and some data.
      val buf = new Array[Byte](4 + (rnd nextInt 100))
      var retry = false
      do {
        rnd.nextBytes(buf)
        intercept[ZipException] {
          fields parse (MutableBuffer wrap buf).littleEndian.asImmutableBuffer
          retry = true
        }
      } while (retry)
    }
  }
}
