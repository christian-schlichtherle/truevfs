/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import java.beans._
import java.io._
import java.net._

import net.java.truecommons.io.Loan._
import net.java.truevfs.access.TFile2Test._
import net.java.truevfs.kernel.spec._
import org.junit._
import org.scalatest.Matchers._
import org.scalatest.junit.JUnitSuite
import org.scalatest.mock.MockitoSugar.mock
import org.scalatest.prop.PropertyChecks._
import org.slf4j._

class TFile2Test extends JUnitSuite {

  private val config = TConfig open ()

  @Before def setUp() {
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

  @After def tearDown() {
    config close ()
  }

  @Test def testSerialization() {
    val table = Table(
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
    forAll(table) { uri =>
      val file = new TFile(URI.create(uri))
      objectRoundTrip(file)
      xmlRoundTrip(file)
    }
  }

  def objectRoundTrip(file: TFile) {
    val bos = new ByteArrayOutputStream
    loan(new ObjectOutputStream(bos)) to (_ writeObject file)

    logger trace ("Number of serialized bytes: {}", bos.size)

    val bis = new ByteArrayInputStream(bos.toByteArray)
    val clone = loan(new ObjectInputStream(bis)) to (_.readObject)

    clone should not be theSameInstanceAs (file)
    clone should equal (file.getAbsoluteFile)
  }

  def xmlRoundTrip(file: TFile) {
    val bos = new ByteArrayOutputStream
    loan(new XMLEncoder(bos)) to { enc =>
      enc setExceptionListener listener
      enc writeObject file
    }

    logger trace ("XML String: {}", bos.toString("UTF-8"))

    val bis = new ByteArrayInputStream(bos.toByteArray)
    val clone = loan(new XMLDecoder(bis)) to (_.readObject)

    clone should not be theSameInstanceAs (file)
    clone should equal (file.getAbsoluteFile)
  }
}

object TFile2Test {
  private val logger = LoggerFactory.getLogger(classOf[TFile2Test])

  private val listener = new ExceptionListener {
    def exceptionThrown(ex: Exception) = throw new AssertionError(ex)
  }
}
