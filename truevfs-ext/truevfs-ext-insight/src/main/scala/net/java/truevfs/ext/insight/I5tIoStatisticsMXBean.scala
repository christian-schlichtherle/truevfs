/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent._

/**
 * An MXBean interface for [[net.java.truevfs.ext.insight.stats.IoStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
trait I5tIoStatisticsMXBean {

  def getReadBytesPerOperation: Int
  def getReadBytesTotal: Long
  def getReadKilobytesPerSecond: Long
  def getReadNanosecondsPerOperation: Long
  def getReadNanosecondsTotal: Long
  def getReadOperations: Long
  def getReadThreadsTotal: Int
  def getSubject: String
  def getTimeCreatedDate: String
  def getTimeCreatedMillis: Long
  def getTimeUpdatedDate: String
  def getTimeUpdatedMillis: Long
  def getWriteBytesPerOperation: Int
  def getWriteBytesTotal: Long
  def getWriteKilobytesPerSecond: Long
  def getWriteNanosecondsPerOperation: Long
  def getWriteNanosecondsTotal: Long
  def getWriteOperations: Long
  def getWriteThreadsTotal: Int

  def rotate()
}
