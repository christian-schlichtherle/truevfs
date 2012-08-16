/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import java.util._
import javax.annotation.concurrent._
import javax.management._
import net.java.truevfs.ext.insight.stats._
import scala.math._

/**
  * A view for [[net.java.truevfs.ext.insight.stats.FsStatistics]].
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private abstract class I5tStatisticsView(tµpe: Class[_], isMXBean: Boolean)
extends StandardMBean(tµpe, isMXBean) {

  protected override def getDescription(info: MBeanAttributeInfo) = {
    info.getName match {
      case "ReadBytesPerOperation" =>
        "The average number of bytes per read operation."
      case "ReadBytesTotal" =>
        "The total number of bytes read."
      case "ReadKilobytesPerSecond" =>
        "The average throughput for read operations."
      case "ReadNanosecondsPerOperation" =>
        "The average execution time per read operation."
      case "ReadNanosecondsTotal" =>
        "The total execution time for read operations."
      case "ReadOperations" =>
        "The total number of read operations."
      case "ReadThreadsTotal" =>
        "The total number of reading threads."
      case "Subject" =>
        "The subject of this log."
      case "SyncNanosecondsPerOperation" =>
        "The average execution time per sync operation."
      case "SyncNanosecondsTotal" =>
        "The total execution time for sync operations."
      case "SyncOperations" =>
        "The total number of sync operations."
      case "SyncThreadsTotal" =>
        "The total number of syncing threads."
      case "TimeCreatedDate" =>
        "The time this log has been created."
      case "TimeCreatedMillis" =>
        "The time this log has been created in milliseconds."
      case "TimeUpdatedDate" =>
        "The last time this log has been updated."
      case "TimeUpdatedMillis" =>
        "The last time this log has been updated in milliseconds."
      case "WriteBytesPerOperation" =>
        "The average number of bytes per write operation."
      case "WriteBytesTotal" =>
        "The total number of bytes written."
      case "WriteKilobytesPerSecond" =>
        "The average throughput for write operations."
      case "WriteNanosecondsPerOperation" =>
        "The average execution time per write operation."
      case "WriteNanosecondsTotal" =>
        "The total execution time for write operations."
      case "WriteOperations" =>
        "The total number of write operations."
      case "WriteThreadsTotal" =>
        "The total number of writing threads."
      case _ =>
        null
    }
  }

  protected override def getDescription(info: MBeanOperationInfo) = {
    info.getName match {
      case "rotate" =>
        "Rotates the underlying statistics. This operation does not affect snapshots."
      case _ =>
        null
    }
  }

  def stats: FsStatistics

  final def readStats = stats.readStats
  final def writeStats = stats.writeStats
  final def syncStats = stats.syncStats

  final def getReadBytesPerOperation = readStats.bytesPerOperation
  final def getReadBytesTotal = readStats.bytesTotal
  final def getReadKilobytesPerSecond = readStats.kilobytesPerSecond
  final def getReadNanosecondsPerOperation = readStats.nanosecondsPerOperation
  final def getReadNanosecondsTotal = readStats.nanosecondsTotal
  final def getReadOperations = readStats.sequenceNumber
  final def getReadThreadsTotal = readStats.threadsTotal

  def getSubject: String

  final def getSyncNanosecondsPerOperation = syncStats.nanosecondsPerOperation
  final def getSyncNanosecondsTotal = syncStats.nanosecondsTotal
  final def getSyncOperations = syncStats.sequenceNumber
  final def getSyncThreadsTotal = syncStats.threadsTotal
  final def getTimeCreatedDate = new Date(getTimeCreatedMillis).toString
  final def getTimeCreatedMillis = stats.timeMillis
  final def getTimeUpdatedDate = new Date(getTimeUpdatedMillis).toString
  final def getTimeUpdatedMillis =
    max(max(readStats.timeMillis, writeStats.timeMillis), syncStats.timeMillis)
  final def getWriteBytesPerOperation = writeStats.bytesPerOperation
  final def getWriteBytesTotal = writeStats.bytesTotal
  final def getWriteKilobytesPerSecond = writeStats.kilobytesPerSecond
  final def getWriteNanosecondsPerOperation = writeStats.nanosecondsPerOperation
  final def getWriteNanosecondsTotal = writeStats.nanosecondsTotal
  final def getWriteOperations = writeStats.sequenceNumber
  final def getWriteThreadsTotal = writeStats.threadsTotal

  def rotate()
}
