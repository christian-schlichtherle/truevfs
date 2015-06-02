/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

import net.java.truevfs.ext.insight.stats.FsLoggerTest._
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop._

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class FsLoggerTest extends WordSpec with ShouldMatchers with PropertyChecks {

  def create = new FsLogger

  "A file system logger" when {
    "newly created" should {
      "have ten as its default size" in {
        create should have size 10
      }

      "provide empty statistics for all offsets" in {
        val empty = FsStatistics()
        val logger = create
        for (offset <- 0 until logger.size)
          logger stats offset equalsIgnoreTime empty should equal (true)
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
        new FsLogger(100).format(0) should be ("00")
      }
    }

    "logging read operations" should {
      "reflect the update at the current position" in {
        val logger = create
        val expected = logger.current logRead (nanos, bytes, 1)
        Thread sleep 1
        val readStats = logger logRead (nanos, bytes)
        val current = logger.current
        current.readStats should be theSameInstanceAs readStats
        current.readStats equalsIgnoreTime expected.readStats should equal (true)
        current.writeStats should equal (expected.writeStats)
        current.syncStats should equal (expected.syncStats)
        current.timeMillis should be (expected.timeMillis)
      }
    }

    "logging write operations" should {
      "reflect the update at the current position" in {
        val logger = create
        val expected = logger.current logWrite (nanos, bytes, 1)
        Thread sleep 1
        val writeStats = logger logWrite (nanos, bytes)
        val current = logger.current
        current.readStats should equal (expected.readStats)
        current.writeStats should be theSameInstanceAs writeStats
        current.writeStats equalsIgnoreTime expected.writeStats should equal (true)
        current.syncStats should equal (expected.syncStats)
        current.timeMillis should be (expected.timeMillis)
      }
    }

    "logging sync operations" should {
      "reflect the update at the current position" in {
        val logger = create
        val expected = logger.current logSync (nanos, 1)
        Thread sleep 1
        val syncStats = logger logSync nanos
        val current = logger.current
        current.readStats should equal (expected.readStats)
        current.writeStats should equal (expected.writeStats)
        current.syncStats should be theSameInstanceAs syncStats
        current.syncStats equalsIgnoreTime expected.syncStats should equal (true)
        current.timeMillis should be (expected.timeMillis)
      }
    }

    "rotated" should {
      val logger = create
      val size = logger.size

      "rotate its current position" in {
        for (i <- 0 until size) {
          val position = logger rotate ()
          val next = (i + 1) % size
          position should equal (next)
        }
      }

      "maintain offset zero as its current position" in {
        for (i <- 0 until size) {
          logger rotate ()
          val current = logger.current
          logger stats 0 should be theSameInstanceAs current
        }
      }

      "move the old statistics to offset 1" in {
        for (i <- 0 until size) {
          val oldStats = logger.current
          logger rotate ()
          val newStats = logger.current
          newStats should not be theSameInstanceAs (oldStats)
          logger stats 1 should be theSameInstanceAs oldStats
        }
      }

      "clear the statistics at the new position" in {
        for (i <- 0 until size) {
          logger rotate ()
          val newStats = logger.current
          newStats equalsIgnoreTime FsStatistics() should equal (true)
        }
      }
    }
  }
}

object FsLoggerTest {
  private val nanos = 1000 * 1000
  private val bytes = 1024
}
