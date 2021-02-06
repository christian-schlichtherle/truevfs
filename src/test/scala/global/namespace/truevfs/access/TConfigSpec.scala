/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.access

import global.namespace.fun.io.api.Socket
import global.namespace.service.wight.core.ServiceLocator
import global.namespace.truevfs.access.TConfig._
import global.namespace.truevfs.access.TConfigSpec._
import global.namespace.truevfs.comp.util._
import global.namespace.truevfs.driver.mock.MockArchiveDriver
import global.namespace.truevfs.kernel.api.FsAccessOption._
import global.namespace.truevfs.kernel.api.FsManager
import global.namespace.truevfs.kernel.api.sl._
import global.namespace.truevfs.kernel.api.spi._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec

import java.util.Optional
import java.util.concurrent._
import scala.jdk.CollectionConverters._

/**
 * DO NOT MODIFY THE GLOBAL CONFIGURATION IN THESE TESTS!
 * Its global scope makes it available to any other test running in parallel.
 *
 * @author Christian Schlichtherle
 */
class TConfigSpec extends AnyWordSpec {

  "The TConfig class" should {
    "have the GLOBAL configuration as its current() configuration by default" in {
      current should be theSameInstanceAs GLOBAL
    }

    "throw an  IllegalStateException when calling close() without a prior call to open()" in {
      intercept[IllegalStateException] {
        current.close()
      }
    }

    "correctly implement current()/open()/close()" in {
      val c1 = current
      configSocket accept { c2: TConfig =>
        c2 should not be theSameInstanceAs(c1)
        c2 should equal(c1)
        current should be theSameInstanceAs c2
        inNewChild {
          current should be theSameInstanceAs c2
          intercept[IllegalStateException] {
            current.close()
          }
        }
        configSocket accept { c3: TConfig =>
          c3 should not be theSameInstanceAs(c2)
          c3 should equal(c2)
          current should be theSameInstanceAs c3
          inNewChild {
            current should be theSameInstanceAs c3
            intercept[IllegalStateException] {
              current.close()
            }
          }
        }
        current should be theSameInstanceAs c2
      }
      current should be theSameInstanceAs c1
    }
  }

  "The GLOBAL configuration" should {
    "be correctly initialized" in {
      GLOBAL.getManager should be theSameInstanceAs FsManagerLocator.SINGLETON.get
      GLOBAL.getArchiveDetector should be theSameInstanceAs TArchiveDetector.ALL
      GLOBAL.getAccessPreferences should equal(BitField.of(CREATE_PARENTS))
    }

    "throw an IllegalStateException" when {
      "calling close()" in {
        intercept[IllegalStateException] {
          GLOBAL.close()
        }
      }

      "calling close() in an open()/close() block" in {
        intercept[IllegalStateException] {
          configSocket accept { _: TConfig => GLOBAL.close() }
        }
      }
    }
  }

  "A TConfig" should {
    "update its mutable property for a file system manager" in {
      configSocket accept { config: TConfig =>
        intercept[NullPointerException] {
          config setManager null
        }
        config.getManager should be theSameInstanceAs FsManagerLocator.SINGLETON.get
        val manager: FsManager = new ServiceLocator()
          .provider[FsManager, FsManagerFactory, FsManagerDecorator](classOf[FsManagerFactory], classOf[FsManagerDecorator])
          .get
        config setManager manager
        config.getManager should be theSameInstanceAs manager
      }
    }

    "update its mutable property for an archive detector" in {
      configSocket accept { config: TConfig =>
        intercept[NullPointerException] {
          config setArchiveDetector null
        }
        config.getArchiveDetector should be theSameInstanceAs TArchiveDetector.ALL
        val detector = new TArchiveDetector("mok", Optional.of(new MockArchiveDriver()))
        config setArchiveDetector detector
        config.getArchiveDetector should be theSameInstanceAs detector
      }
    }

    "update its mutable property for access preferences" in {
      configSocket accept { config: TConfig =>
        intercept[NullPointerException] {
          config setAccessPreferences null
        }
        config.getAccessPreferences should equal(BitField.of(CREATE_PARENTS))
        val preferences = BitField.of(CACHE)
        config setAccessPreferences preferences
        config.getAccessPreferences should be theSameInstanceAs preferences
      }
    }

    "provide the correct default value of its property for access preferences" in {
      configSocket accept { config: TConfig =>
        intercept[NullPointerException] {
          config setAccessPreferences null
        }
        config.getAccessPreferences should equal(BitField.of(CREATE_PARENTS))
      }
    }

    "correctly update its property for access preferences with legal values" in {
      configSocket accept { config: TConfig =>
        val legal = Table(
          "preferences",
          BitField.of(CACHE),
          BitField.of(CREATE_PARENTS),
          BitField.of(GROW),
          BitField.of(STORE),
          BitField.of(COMPRESS),
          BitField.of(ENCRYPT)
        )
        forAll(legal) { preferences =>
          config setAccessPreferences preferences
          config.getAccessPreferences should be theSameInstanceAs preferences
          for (preference <- preferences.asScala) {
            config.setAccessPreference(preference, false)
            config.getAccessPreferences should equal(preferences.clear(preference))
            config.setAccessPreference(preference, true)
            config.getAccessPreferences should equal(preferences)
            config.getAccessPreferences should not be theSameInstanceAs(preferences)
          }
        }
      }
    }

    "refuse to update its property for access preferences with illegal values" in {
      configSocket accept { config: TConfig =>
        val illegal = Table(
          "preferences",
          BitField.of(EXCLUSIVE),
          BitField.of(APPEND),
          BitField.of(STORE, COMPRESS)
        )
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

private object TConfigSpec {

  private lazy val configSocket: Socket[TConfig] = () => TConfig.open()

  private def inNewChild[V](operation: => V): Unit = {
    var ex: Throwable = null
    val r = new Runnable() {

      def run(): Unit = {
        try {
          operation
        } catch {
          case ex2: Throwable => ex = ex2
        }
      }
    }
    val t = new Thread(r)
    t.start()
    t.join()
    if (null != ex) {
      throw new ExecutionException(ex)
    }
  }
}