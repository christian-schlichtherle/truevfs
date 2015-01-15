/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.zip

import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._

import scala.util._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class ZipEntrySpec extends WordSpec {

  "A ZIP entry" should {
    val entry = new ZipEntry("foo")

    "throw an IllegalArgumentException when deserializing a data block which does not conform to the ZIP File Format Specification" in {
      val rnd = new Random()
      // Make space for a header id (2), a field length (2) and some data.
      val buf = new Array[Byte](4 + (rnd nextInt 100))
      var retry = false
      do {
        rnd.nextBytes(buf)
        intercept[IllegalArgumentException] {
          entry.setExtra(buf)
          retry = true
        }
      } while (retry)
    }
  }
}
