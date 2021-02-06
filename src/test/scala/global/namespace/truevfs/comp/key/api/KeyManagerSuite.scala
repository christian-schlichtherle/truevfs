/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.key.api

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import java.net.URI

/**
 * @tparam M the type of the keys.
 * @author Christian Schlichtherle
 */
abstract class KeyManagerSuite[M] extends AnyWordSpec with BeforeAndAfter {

  protected final val uri: URI = URI.create("please-ignore-this-test-resource")

  protected val keyManager: KeyManager[M]

  after {
    keyManager.unlink(uri)
  }

  "A key manager" should {
    "implement the life cycle" in {
      val writeKey = keyManager.provider(uri).getKeyForWriting
      val readKey = keyManager.provider(uri).getKeyForReading(false)
      writeKey shouldBe readKey
    }
  }
}
