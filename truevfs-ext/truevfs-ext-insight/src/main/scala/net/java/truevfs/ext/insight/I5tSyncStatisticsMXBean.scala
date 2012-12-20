/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import javax.annotation.concurrent.ThreadSafe

/**
 * An MXBean interface for [[net.java.truevfs.ext.insight.stats.SyncStatistics]].
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
trait I5tSyncStatisticsMXBean {

  def getSubject: String
  def getSyncNanosecondsPerOperation: Long
  def getSyncNanosecondsTotal: Long
  def getSyncOperations: Long
  def getSyncThreadsTotal: Int
  def getTimeCreatedDate: String
  def getTimeCreatedMillis: Long
  def getTimeUpdatedDate: String
  def getTimeUpdatedMillis: Long

  def rotate ()
}
