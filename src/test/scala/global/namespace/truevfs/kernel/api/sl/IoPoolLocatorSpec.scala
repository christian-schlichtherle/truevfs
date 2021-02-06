/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.sl

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/**
 * Tests if the class path has been properly configured so that any I/O buffer pool is locatable at RUNTIME!
 *
 * @author Christian Schlichtherle
 */
class IoPoolLocatorSpec extends AnyWordSpec {

  "The I/O buffer pool locator singleton" should {
    "provide an I/O buffer pool" in {
      IoBufferPoolLocator.SINGLETON.get should not be null
    }
  }
}
