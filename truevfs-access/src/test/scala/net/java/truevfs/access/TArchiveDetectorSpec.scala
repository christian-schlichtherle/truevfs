/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import net.java.truevfs.kernel.spec._
import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.scalatest.mockito.MockitoSugar.mock
import org.scalatest.prop.PropertyChecks._

import scala.collection.JavaConverters._

/**
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
class TArchiveDetectorSpec extends WordSpec {

  "A TArchiveDetector" should {
    "retain drivers in the map which are not accepted by the extension set" in {
      val table = Table(
        ("sequence", "accept", "test"),
        (Seq("a", "b"), "a", "b")
      )
      forAll(table) { (sequence, accept, test) =>
        val map = {
          sequence map (FsScheme.create(_) -> mock[FsArchiveDriver[FsArchiveEntry]])
        }.toMap[FsScheme, FsDriver]
        val detector = new TArchiveDetector(
          new TArchiveDetector(TArchiveDetector.NULL, map.asJava),
          accept)
        val scheme = FsScheme create test
        detector scheme test should be (null)
        detector.get get scheme should not be null
      }
    }

    "retain drivers in the map which have been replaced by a null value mapping" in {
      val table = Table(
        ("sequence", "accept", "test"),
        (Seq("a", "b"), "a", "b")
      )
      forAll(table) { (sequence, accept, test) =>
        val map = {
          sequence map (FsScheme.create(_) -> mock[FsArchiveDriver[FsArchiveEntry]])
        }.toMap[FsScheme, FsDriver]
        val scheme = FsScheme create test
        val detector = new TArchiveDetector(
          new TArchiveDetector(TArchiveDetector.NULL, map.asJava),
          Map(scheme -> (null: FsDriver)).asJava)
        detector.scheme(test) should be (null)
        detector.get get scheme should not be null
      }
    }
  }
}
