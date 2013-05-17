/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._
import javax.management._
import net.java.truevfs.ext.insight.stats._

/**
  * A view for [[net.java.truevfs.ext.insight.stats.FsStatistics]].
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private abstract class I5tStatisticsView(tµpe: Class[_], isMXBean: Boolean)
extends StandardMBean(tµpe, isMXBean) with FsStatisticsView {

  protected override def getDescription(info: MBeanAttributeInfo) = {
    info.getName match {
      case "ReadBytesPerOperation"        => "The average number of bytes per read operation."
      case "ReadBytesTotal"               => "The total number of bytes read."
      case "ReadKilobytesPerSecond"       => "The average throughput for read operations."
      case "ReadNanosecondsPerOperation"  => "The average execution time per read operation."
      case "ReadNanosecondsTotal"         => "The total execution time for read operations."
      case "ReadOperations"               => "The total number of read operations."
      case "ReadThreadsTotal"             => "The total number of reading threads."
      case "Subject"                      => "The subject of this log."
      case "SyncNanosecondsPerOperation"  => "The average execution time per sync operation."
      case "SyncNanosecondsTotal"         => "The total execution time for sync operations."
      case "SyncOperations"               => "The total number of sync operations."
      case "SyncThreadsTotal"             => "The total number of syncing threads."
      case "TimeCreatedDate"              => "The time this log has been created."
      case "TimeCreatedMillis"            => "The time this log has been created in milliseconds."
      case "TimeUpdatedDate"              => "The last time this log has been updated."
      case "TimeUpdatedMillis"            => "The last time this log has been updated in milliseconds."
      case "WriteBytesPerOperation"       => "The average number of bytes per write operation."
      case "WriteBytesTotal"              => "The total number of bytes written."
      case "WriteKilobytesPerSecond"      => "The average throughput for write operations."
      case "WriteNanosecondsPerOperation" => "The average execution time per write operation."
      case "WriteNanosecondsTotal"        => "The total execution time for write operations."
      case "WriteOperations"              => "The total number of write operations."
      case "WriteThreadsTotal"            => "The total number of writing threads."
      case _                              => null
    }
  }

  protected override def getDescription(info: MBeanOperationInfo) = {
    info.getName match {
      case "rotate" => "Rotates the underlying statistics. This operation does not affect snapshots."
      case _        => null
    }
  }

  def getSubject: String

  def rotate()
}
