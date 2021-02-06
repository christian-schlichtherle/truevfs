/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.jmx

import global.namespace.truevfs.comp.jmx.sl._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import javax.management._

/**
 * @author Christian Schlichtherle
 */
class ObjectNameModifierSpec extends AnyWordSpec {

  def modifier: ObjectNameModifier = ObjectNameModifierLocator.SINGLETON.get

  "An object name modifier" should {
    "do a proper round trip conversion" in{
      val original = new ObjectName(":key=value")
      val modified = modifier apply original
      modified should not be original
      val clone = modifier unapply modified
      clone should not be theSameInstanceAs (original)
      clone should be (original)
    }
  }
}
