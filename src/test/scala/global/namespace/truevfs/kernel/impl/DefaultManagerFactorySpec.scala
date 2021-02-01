/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.kernel.impl

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/**
 * @author Christian Schlichtherle
 */
class DefaultManagerFactorySpec extends AnyWordSpec {

  "A default file system manager factory" should {
    val service = new DefaultManagerFactory

    "provide a default file system manager" in {
      service.get should not be null
    }
  }
}
