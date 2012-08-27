/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.access

import collection.JavaConverters._
import java.util.concurrent._
import net.java.truecommons.io.Loan._
import net.java.truecommons.services._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.driver.mock._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.FsAccessOption._
import net.java.truevfs.kernel.spec.FsAccessOptions._
import net.java.truevfs.kernel.spec.sl._
import net.java.truevfs.kernel.spec.spi._
import org.junit.runner._
import org.scalatest._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.slf4j._

/**
  * DO NOT MODIFY THE GLOBAL CONFIGURATION IN THESE TESTS!
  * Its global scope makes it available to any other test running in parallel,
  * if any.
  * 
  * @author Christian Schlichtherle
  */
@RunWith(classOf[JUnitRunner])
class TConfigSpec
extends WordSpec with ShouldMatchers with PropertyChecks {

  private def inNewChild[V](operation: => V) {
    var ex: Throwable = null
    val r = new Runnable() {
      def run() {
        try { operation }
        catch { case ex2: Throwable => ex = ex2 }
      }
    }
    val t = new Thread(r)
    t start ()
    t join ()
    if (null != ex) throw new ExecutionException(ex)
  }

  import TConfig._

  "The TConfig class" should {
    "have the global configuration as its current configuration by default" in {
      get should be theSameInstanceAs (GLOBAL)
    }

    "throw IllegalStateException when popping without a prior push" in {
      intercept[IllegalStateException] { get close () }
    }

    "correctly implement get/open/close" in {
      val c1 = get
      loan(open()) to { c2 =>
        c2 should not be theSameInstanceAs (c1)
        c2 should equal (c1)
        get should be theSameInstanceAs (c2)
        inNewChild {
          get should be theSameInstanceAs (c2)
          intercept[IllegalStateException] { get close () }
        }
        loan(open()) to { c3 =>
          c3 should not be theSameInstanceAs (c2)
          c3 should equal (c2)
          get should be theSameInstanceAs (c3)
          inNewChild {
            get should be theSameInstanceAs (c3)
            intercept[IllegalStateException] { get close () }
          }
        }
        get should be theSameInstanceAs (c2)
      }
      get should be theSameInstanceAs (c1)
    }
  }

  "The global configuration" should {
    "be setup correctly" in {
      val config = GLOBAL
      config.getManager should be theSameInstanceAs (FsManagerLocator.SINGLETON.get)
      config.getArchiveDetector should be theSameInstanceAs (TArchiveDetector.ALL)
      config.getAccessPreferences should equal (BitField.of(CREATE_PARENTS))
    }
  }

  "A TConfig" should {
    "update its mutable property for a file system manager" in {
      loan(open()) to { config =>
        intercept[NullPointerException] { config setManager null }
        config.getManager should be theSameInstanceAs (FsManagerLocator.SINGLETON.get)
        val manager = new Locator(classOf[FsManagerLocator])
        .factory(classOf[FsManagerFactory], classOf[FsManagerDecorator])
        .get
        config setManager manager
        config.getManager should be theSameInstanceAs (manager)
      }
    }

    "update its mutable property for an archive detector" in {
      loan(open()) to { config =>
        intercept[NullPointerException] { config setArchiveDetector null }
        config.getArchiveDetector should be theSameInstanceAs (TArchiveDetector.ALL)
        val detector = new TArchiveDetector("mok", new MockArchiveDriver())
        config setArchiveDetector detector
        config.getArchiveDetector should be theSameInstanceAs (detector)
      }
    }

    "update its mutable property for access preferences" in {
      loan(open()) to { config =>
        intercept[NullPointerException] { config setAccessPreferences null }
        config.getAccessPreferences should equal (BitField.of(CREATE_PARENTS))
        val preferences = BitField.of(CACHE)
        config setAccessPreferences preferences
        config.getAccessPreferences should be theSameInstanceAs (preferences)
      }
    }

    "provide the correct default value of its property for access preferences" in {
      loan(open()) to { config =>
        intercept[NullPointerException] { config setAccessPreferences null }
        config.getAccessPreferences should equal (BitField.of(CREATE_PARENTS))
      }
    }

    "correctly update its property for access preferences with legal values" in {
      loan(open()) to { config =>
        val legal = Table(
          ("preferences"),
          (BitField.of(CACHE)),
          (BitField.of(CREATE_PARENTS)),
          (BitField.of(GROW)),
          (BitField.of(STORE)),
          (BitField.of(COMPRESS)),
          (BitField.of(ENCRYPT))
        );
        forAll(legal) { preferences =>
          config setAccessPreferences preferences
          config.getAccessPreferences should be theSameInstanceAs (preferences)
          for (preference <- preferences.asScala) {
            config.setAccessPreference(preference, false)
            config.getAccessPreferences should equal (preferences.clear(preference))
            config.setAccessPreference(preference, true)
            config.getAccessPreferences should equal (preferences)
            config.getAccessPreferences should not be theSameInstanceAs (preferences)
          }
        }
      }
    }

    "refuse to update its property for access preferences with illegal values" in {
      loan(open()) to { config =>
        val illegal = Table(
          ("preferences"),
          (BitField.of(EXCLUSIVE)),
          (BitField.of(APPEND)),
          (BitField.of(STORE, COMPRESS))
        );
        forAll(illegal) { preferences =>
          intercept[IllegalArgumentException] {
            config.setAccessPreferences(preferences)
          }
          if (1 == preferences.cardinality) {
            for (preference <- preferences.asScala) {
              intercept[IllegalArgumentException] {
                config.setAccessPreference(preference, true)
              }
            }
          }
        }
      }
    }
  }
}
