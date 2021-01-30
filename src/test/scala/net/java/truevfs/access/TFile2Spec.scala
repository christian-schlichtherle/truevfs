/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import java.net.URI

import global.namespace.fun.io.api.Socket
import global.namespace.fun.io.bios.BIOS
import net.java.truevfs.access.TFile2Spec._
import net.java.truevfs.kernel.spec._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.mockito.MockitoSugar.mock

/** @author Christian Schlichtherle */
class TFile2Spec extends WordSpec {

  "A TFile" should {
    "be round-trip encodable using object serialization and XML" in {
      configSocket.accept { config: TConfig =>
        setUp(config)
        forAll(uris) { uri =>
          val original = new TFile(URI.create(uri))
          forAll(codecs) { codec =>
            val clone = codec.connect(BIOS.memory).clone(original)
            clone should not be theSameInstanceAs(original)
            clone shouldBe original.getAbsoluteFile
          }
        }
      }
    }
  }
}

private object TFile2Spec {

  val configSocket: Socket[TConfig] = () => TConfig.open()

  def setUp(config: TConfig): Unit = {
    val manager = mock[FsManager]
    val driver = mock[FsDriver]
    val archiveDriver = mock[FsArchiveDriver[FsArchiveEntry]]
    val detector = new TArchiveDetector(
      TArchiveDetector.NULL,
      Array(
        Array("file", driver),
        Array("a1|a2|a3", archiveDriver)
      )
    )
    config setManager manager
    config setArchiveDetector detector
  }

  val uris = Table(
    "uri",
    "file:/file",
    "a1:file:/archive.a1!/",
    "a1:file:/archive.a1!/entry",
    "a2:a1:file:/foo.a1!/bar.a2!/",
    "a2:a1:file:/foo.a1!/bar.a2!/META-INF/MANIFEST.MF",
    "a2:a1:file:/föö%20bär.a1!/föö%20bär.a2!/föö%20bär",
    "a1:file:/föö%20bär.a1!/föö%20bär",
    "file:/föö%20bär/föö%20bär",
    "a1:file:/foo.a1!/bar",
    "file:/foo/bar",
    "file:/foo/bar"
  )

  val codecs = Table(
    "codec",
    BIOS.serialization,
    BIOS.xml
  )
}
