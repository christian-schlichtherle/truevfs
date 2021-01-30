/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons.jmx

import net.java.truecommons.jmx.sl._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

import javax.management._

/**
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
class ObjectNameModifierTest extends AnyWordSpec {

  def modifier = ObjectNameModifierLocator.SINGLETON.get

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
