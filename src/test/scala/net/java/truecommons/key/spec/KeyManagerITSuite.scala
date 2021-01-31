/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.key.spec

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

/**
 * @tparam K the type of the keys.
 * @author Christian Schlichtherle
 */
abstract class KeyManagerITSuite[K] extends AnyWordSpec with BeforeAndAfter {

  protected def keyManager: KeyManager[K]

  protected final def uri: URI = URI.create("please-ignore-this-test-resource")

  after {
    keyManager.unlink(uri)
  }

  "A key manager" should {
    "implement the life cycle" in {
      val writeKey = keyManager.provider(uri).getKeyForWriting
      val readKey = keyManager.provider(uri).getKeyForReading(false)
      writeKey shouldBe readKey
      keyManager.unlink(uri)
    }
  }
}
