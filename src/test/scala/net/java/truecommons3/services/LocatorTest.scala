/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truecommons3.services

import java.util._

import org.junit.runner._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.junit._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class LocatorTest extends WordSpec {
  import net.java.truecommons3.services.LocatorTest._

  def locator[P] = new LocatorSugar

  "A locator" when {
    val l = locator

    "asked to create a container" should {
      "report a service configuration error if it can't locate a factory" in {
        intercept[ServiceConfigurationError] {
          l.container[String, UnlocatableFactory]
        }
      }

      "not report a service configuration error if it can't locate a decorator" in {
        val c = l.container[String, LocatableFactory[String], UnlocatableDecorator]
        c.get should not be null
      }
    }

    "asked to create a container" should {
      val c = l.container[String, LocatableFactory[String], LocatableDecorator[String]]

      "always reproduce the expected product" in {
        c.get should equal (expected)
        c.get should equal (expected)
      }

      "provide the same product" in {
        val p1 = c.get
        val p2 = c.get
        p1 should be theSameInstanceAs p2
      }
    }

    "asked to create a factory" should {
      val f = l.factory[String, LocatableFactory[String], LocatableDecorator[String]]

      "always reproduce the expected product" in {
        f.get should equal (expected)
        f.get should equal (expected)
      }

      "provide an equal, but not same product" in {
        val p1 = f.get
        val p2 = f.get
        p1 should equal (p2)
        p1 should not be theSameInstanceAs(p2)
      }
    }
  }
}

object LocatorTest {

  val expected  = "Hello Christian! How do you do?"

  final class LocatorSugar {
    private[this] val l = new ServiceLocator(classOf[LocatorTest])

    def container[P, F <: LocatableFactory[P] : Manifest] =
      l container implicitly[Manifest[F]].runtimeClass.asInstanceOf[Class[F]]

    def container[P, F <: LocatableFactory[P] : Manifest, D <: LocatableDecorator[P] : Manifest] =
      l container (implicitly[Manifest[F]].runtimeClass.asInstanceOf[Class[F]],
                   implicitly[Manifest[D]].runtimeClass.asInstanceOf[Class[D]])

    def factory[P, F <: LocatableFactory[P] : Manifest] =
      l factory implicitly[Manifest[F]].runtimeClass.asInstanceOf[Class[F]]

    def factory[P, F <: LocatableFactory[P] : Manifest, D <: LocatableDecorator[P] : Manifest] =
      l factory (implicitly[Manifest[F]].runtimeClass.asInstanceOf[Class[F]],
                 implicitly[Manifest[D]].runtimeClass.asInstanceOf[Class[D]])
  }
}

abstract class UnlocatableFactory extends LocatableFactory[String]
abstract class UnlocatableDecorator extends LocatableDecorator[String]

final class World extends LocatableFactory[String] {
  def get = new String("World") // return a new string upon each call
  override def getPriority = -1
}

final class Christian extends LocatableFactory[String] {
  def get = new String("Christian") // return a new string upon each call
}

final class Salutation extends LocatableDecorator[String] {
  def apply(text: String) = "Hello %s!" format text
  override def getPriority = -1
}

final class Smalltalk extends LocatableDecorator[String] {
  def apply(text: String) = text + " How do you do?"
}
