/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.api.sl

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/**
 * Tests if the class path has been properly configured so that any file system manager is locatable at RUNTIME!
 *
 * @author Christian Schlichtherle
 */
class FsManagerLocatorIT extends AnyWordSpec {

  "The file system manager locator singleton" should {
    "provide a file system manager" in {
      FsManagerLocator.SINGLETON.get should not be null
    }
  }
}
