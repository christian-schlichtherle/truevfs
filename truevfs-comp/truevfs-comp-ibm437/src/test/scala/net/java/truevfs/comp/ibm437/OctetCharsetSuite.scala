/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.comp.ibm437

import org.scalatest.Matchers._
import org.scalatest.WordSpec

import java.nio.ByteBuffer
import java.nio.charset.Charset

abstract class OctetCharsetSuite extends WordSpec {

  val charset: Charset

  "An octet charset" should {
    "encode and decode strings" in {
      val dec = charset.newDecoder
      val enc = charset.newEncoder
      val b1 = Array.tabulate(256)(_.asInstanceOf[Byte])
      val bb1 = ByteBuffer.wrap(b1)
      val cb = dec.decode(bb1)
      val bb2 = enc.encode(cb)
      val b2 = bb2.array
      b1 shouldBe b2
    }
  }
}
