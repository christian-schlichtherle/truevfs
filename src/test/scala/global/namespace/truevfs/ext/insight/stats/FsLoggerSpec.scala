/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.ext.insight.stats

import global.namespace.truevfs.ext.insight.stats.FsLoggerSpec._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

/** @author Christian Schlichtherle */
class FsLoggerSpec extends AnyWordSpec {

  def create = new FsLogger

  "A file system logger" when {
    "newly created" should {
      "have ten as its default size" in {
        create should have size 10
      }

      "provide empty statistics for all offsets" in {
        val empty = FsStats.getInstance
        val logger = create
        for (offset <- 0 until logger.size) {
          logger stats offset equalsIgnoreTime empty should equal(true)
        }
      }

      "throw a RuntimeException when accessing negative offsets" in {
        intercept[RuntimeException] {
          create stats -1
        }
      }

      "throw a RuntimeException when accessing offsets greater than or equal to size" in {
        val logger = create
        intercept[RuntimeException] {
          logger stats logger.size
        }
      }
    }

    "formatting offsets" should {
      "consider the size of the logger" in {
        new FsLogger(100).format(0) should be("00")
      }
    }

    "logging read operations" should {
      "reflect the update at the current position" in {
        val logger = create
        val expected = logger.current logRead(nanos, bytes, 1)
        Thread sleep 1
        val readStats = logger logRead(nanos, bytes)
        val current = logger.current
        current.getReadStats should be theSameInstanceAs readStats
        current.getReadStats equalsIgnoreTime expected.getReadStats shouldBe true
        current.getWriteStats shouldBe expected.getWriteStats
        current.getSyncStats shouldBe expected.getSyncStats
        current.getTimeMillis shouldBe expected.getTimeMillis
      }
    }

    "logging write operations" should {
      "reflect the update at the current position" in {
        val logger = create
        val expected = logger.current logWrite(nanos, bytes, 1)
        Thread sleep 1
        val writeStats = logger logWrite(nanos, bytes)
        val current = logger.current
        current.getReadStats shouldBe expected.getReadStats
        current.getWriteStats should be theSameInstanceAs writeStats
        current.getWriteStats equalsIgnoreTime expected.getWriteStats shouldBe true
        current.getSyncStats shouldBe expected.getSyncStats
        current.getTimeMillis shouldBe expected.getTimeMillis
      }
    }

    "logging sync operations" should {
      "reflect the update at the current position" in {
        val logger = create
        val expected = logger.current logSync(nanos, 1)
        Thread sleep 1
        val syncStats = logger logSync nanos
        val current = logger.current
        current.getReadStats shouldBe expected.getReadStats
        current.getWriteStats shouldBe expected.getWriteStats
        current.getSyncStats should be theSameInstanceAs syncStats
        current.getSyncStats equalsIgnoreTime expected.getSyncStats shouldBe true
        current.getTimeMillis shouldBe expected.getTimeMillis
      }
    }

    "rotated" should {
      val logger = create
      val size = logger.size

      "rotate its current position" in {
        for (i <- 0 until size) {
          val position = logger.rotate()
          val next = (i + 1) % size
          position shouldBe next
        }
      }

      "maintain offset zero as its current position" in {
        for (i <- 0 until size) {
          logger.rotate()
          val current = logger.current
          logger stats 0 should be theSameInstanceAs current
        }
      }

      "move the old statistics to offset 1" in {
        for (i <- 0 until size) {
          val oldStats = logger.current
          logger.rotate()
          val newStats = logger.current
          newStats should not be theSameInstanceAs(oldStats)
          logger stats 1 should be theSameInstanceAs oldStats
        }
      }

      "clear the statistics at the new position" in {
        for (i <- 0 until size) {
          logger.rotate()
          val newStats = logger.current
          newStats equalsIgnoreTime FsStats.getInstance shouldBe true
        }
      }
    }
  }
}

object FsLoggerSpec {

  private val nanos = 1000 * 1000
  private val bytes = 1024
}
