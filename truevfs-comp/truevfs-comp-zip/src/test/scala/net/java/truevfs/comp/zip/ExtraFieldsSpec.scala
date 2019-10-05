/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip

import org.scalatest._

import scala.util._

/** @author Christian Schlichtherle */
class ExtraFieldsSpec extends WordSpec {

  "A collection of extra fields" should {
    val fields = new ExtraFields

    "throw a RuntimeException when deserializing a data block which does not conform to the ZIP File Format Specification" in {
      val rnd = new Random()
      // Make space for a header id (2), a field length (2) and some data.
      val buf = new Array[Byte](4 + (rnd nextInt 100))
      var retry = false
      do {
        rnd.nextBytes(buf)
        intercept[RuntimeException] {
          fields.readFrom(buf, 0, buf.size)
          retry = true
        }
      } while (retry)
    }
  }
}
