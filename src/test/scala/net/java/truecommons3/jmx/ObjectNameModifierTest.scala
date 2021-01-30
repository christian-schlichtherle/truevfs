/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.jmx

import javax.management._

import net.java.truecommons3.jmx.sl._
import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._

/**
 * @since  TrueCommons 2.3
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
class ObjectNameModifierTest extends WordSpec {

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
