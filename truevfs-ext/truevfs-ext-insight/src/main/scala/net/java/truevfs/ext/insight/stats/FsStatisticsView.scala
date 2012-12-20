/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

import java.util._
import scala.math._

/**
 * @author Christian Schlichtherle
 */
trait FsStatisticsView {

  def stats: FsStatistics

  final def readStats = stats.readStats
  final def writeStats = stats.writeStats
  final def syncStats = stats.syncStats
  final def timeMillis = stats.timeMillis

  final def getReadBytesPerOperation = readStats.bytesPerOperation
  final def getReadBytesTotal = readStats.bytesTotal
  final def getReadKilobytesPerSecond = readStats.kilobytesPerSecond
  final def getReadNanosecondsPerOperation = readStats.nanosecondsPerOperation
  final def getReadNanosecondsTotal = readStats.nanosecondsTotal
  final def getReadOperations = readStats.sequenceNumber
  final def getReadThreadsTotal = readStats.threadsTotal

  final def getWriteBytesPerOperation = writeStats.bytesPerOperation
  final def getWriteBytesTotal = writeStats.bytesTotal
  final def getWriteKilobytesPerSecond = writeStats.kilobytesPerSecond
  final def getWriteNanosecondsPerOperation = writeStats.nanosecondsPerOperation
  final def getWriteNanosecondsTotal = writeStats.nanosecondsTotal
  final def getWriteOperations = writeStats.sequenceNumber
  final def getWriteThreadsTotal = writeStats.threadsTotal

  final def getSyncNanosecondsPerOperation = syncStats.nanosecondsPerOperation
  final def getSyncNanosecondsTotal = syncStats.nanosecondsTotal
  final def getSyncOperations = syncStats.sequenceNumber
  final def getSyncThreadsTotal = syncStats.threadsTotal

  final def getTimeCreatedDate = new Date(getTimeCreatedMillis).toString
  final def getTimeCreatedMillis = timeMillis
  final def getTimeUpdatedDate = new Date(getTimeUpdatedMillis).toString
  final def getTimeUpdatedMillis =
    max(max(readStats.timeMillis, writeStats.timeMillis), syncStats.timeMillis)
}
