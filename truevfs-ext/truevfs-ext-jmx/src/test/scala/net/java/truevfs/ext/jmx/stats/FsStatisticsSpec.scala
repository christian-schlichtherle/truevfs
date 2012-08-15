/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.stats

import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._
import java.beans._
import java.io._
import java.nio.charset._
import net.java.truecommons.io.Loan._

/**
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsStatisticsSpec extends WordSpec with ShouldMatchers with PropertyChecks {

  "File system statics" should {
    "be serializable with Object(Out|In)putStream" in {
      def toArray(o: AnyRef) = {
        val baos = new ByteArrayOutputStream(512)
        loan (new ObjectOutputStream(baos)) to (_ writeObject o)
        baos.toByteArray
      }

      def toObject(a: Array[Byte]) = {
        loan (new ObjectInputStream(new ByteArrayInputStream(a)))
        .to(_ readObject)
      }

      val original = FsStatistics.create()
      val array = toArray(original)
      val clone = toObject(array)
      clone should not be theSameInstanceAs (original)
      clone should equal (original)
    }

    "be serializable with XML(En|De)coder" in {
      def toArray(o: AnyRef) = {
        val baos = new ByteArrayOutputStream(512)
        loan (new XMLEncoder(baos)) to (_ writeObject o)
        baos.toByteArray
      }

      def toObject(a: Array[Byte]) = {
        loan (new XMLDecoder(new ByteArrayInputStream(a)))
        .to(_ readObject)
      }

      val original = FsStatistics.create()
      val array = toArray(original)
      //println(new String(array, StandardCharsets.UTF_8))
      val clone = toObject(array)
      clone should not be theSameInstanceAs (original)
      clone should equal (original)
    }
  }
}
