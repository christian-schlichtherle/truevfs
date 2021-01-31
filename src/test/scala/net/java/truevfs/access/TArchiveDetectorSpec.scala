/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import net.java.truevfs.kernel.spec._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.util.Optional
import scala.jdk.CollectionConverters._

/**
 * @author Christian Schlichtherle
 */
class TArchiveDetectorSpec extends AnyWordSpec {

  "A TArchiveDetector" should {
    "retain drivers in the map which are not accepted by the extension set" in {
      val table = Table(
        ("sequence", "accept", "test"),
        (Seq("a", "b"), "a", "b")
      )
      forAll(table) { (sequence, accept, test) =>
        val map = {
          sequence.map(FsScheme.create(_) -> Optional.of(mock[FsArchiveDriver[FsArchiveEntry]]))
        }.toMap[FsScheme, Optional[_ <: FsDriver]]
        val detector = new TArchiveDetector(accept, new TArchiveDetector(map.asJava, TArchiveDetector.NULL))
        val scheme = FsScheme.create(test)
        detector.scheme(test).isPresent shouldBe false
        detector.get.get(scheme) shouldNot be(null)
      }
    }

    "retain drivers in the map which have been replaced by an empty mapping" in {
      val table = Table(
        ("sequence", "accept", "test"),
        (Seq("a", "b"), "a", "b")
      )
      forAll(table) { (sequence, accept, test) =>
        val map = {
          sequence.map(FsScheme.create(_) -> Optional.of(mock[FsArchiveDriver[FsArchiveEntry]]))
        }.toMap[FsScheme, Optional[_ <: FsDriver]]
        val scheme = FsScheme.create(test)
        val config: Map[FsScheme, Optional[_ <: FsDriver]] = Map(scheme -> Optional.empty[FsDriver]())
        val detector = new TArchiveDetector(config.asJava, new TArchiveDetector(map.asJava, TArchiveDetector.NULL))
        detector.scheme(test).isPresent shouldBe false
        detector.get.get(scheme) shouldNot be(null)
      }
    }
  }
}
