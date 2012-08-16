/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.model

import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._
import java.beans._
import java.io._
import java.nio.charset._
import net.java.truecommons.io.Loan._
import org.slf4j.LoggerFactory

/**
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class FsStatisticsSpec extends WordSpec with ShouldMatchers with PropertyChecks {
  import FsStatisticsSpec._

  val original = FsStatistics()
  .logRead(1000 * 1000, 1024)
  .logWrite(1000 * 1000, 1024)
  .logSync(1000 * 1000 * 1000)

  "File system statics" should {
    "be serializable with Object(Out|In)putStream" in {
      def toArray(o: AnyRef) = {
        val baos = new ByteArrayOutputStream(1024)
        loan (new ObjectOutputStream(baos)) to (_ writeObject o)
        baos.toByteArray
      }

      def toObject(a: Array[Byte]) = {
        loan (new ObjectInputStream(new ByteArrayInputStream(a)))
        .to(_ readObject)
      }

      val array = toArray(original)
      logger.debug("Serialized byte stream length: {} bytes", array.length)
      val clone = toObject(array)
      clone should not be theSameInstanceAs (original)
      clone should equal (original)
    }
  }
}

object FsStatisticsSpec {
  val logger = LoggerFactory.getLogger(classOf[FsStatisticsSpec])
}
