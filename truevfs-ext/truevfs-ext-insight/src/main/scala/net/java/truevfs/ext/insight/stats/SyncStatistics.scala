/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

object SyncStatistics {
  /** Returns sync statistics with all properties set to zero. */
  def apply() = new SyncStatistics(0, 0, 0) // cannot cache because of timeMillis!
}

/**
  * An immutable record of statistics for sync operations.
  *
  * @param  sequenceNumber the non-negative sequence number.
  * @throws IllegalArgumentException if any parameter value is negative.
  * @author Christian Schlichtherle
  */
final case class SyncStatistics private (
  sequenceNumber: Long,
  nanosecondsTotal: Long,
  threadsTotal: Int,
  timeMillis: Long = System.currentTimeMillis
) extends Immutable {

  require(0 <= (sequenceNumber | nanosecondsTotal | threadsTotal | timeMillis))

  def nanosecondsPerOperation =
    if (0 == sequenceNumber) 0L else nanosecondsTotal / sequenceNumber

  /**
    * Logs a sync operation with the given sample data and returns a new
    * object to reflect the updated statistics at the current system time.
    * If any property would overflow to a negative value as a result of the
    * update, then the returned object will simply have its sequence number
    * set to one (!) and its other properties will be reset to reflect only
    * the given parameter values at the current system time.
    * In other words, the statistics would restart from fresh.
    *
    * @param  nanosDelta the execution time.
    * @return A new object which reflects the updated statistics at the
    *         current system time.
    * @throws IllegalArgumentException if any parameter value is negative.
    */
  @throws(classOf[IllegalArgumentException])
  def log(nanosDelta: Long, threadsTotal: Int) = {
    require(0 <= (nanosDelta | threadsTotal))
    try {
      new SyncStatistics(sequenceNumber + 1,
                         nanosecondsTotal + nanosDelta,
                         threadsTotal)
    } catch {
      case _: IllegalArgumentException =>
        new SyncStatistics(1, nanosDelta, 1)
    }
  }

  def equalsIgnoreTime(that: SyncStatistics) =
    this.sequenceNumber == that.sequenceNumber &&
    this.nanosecondsTotal == that.nanosecondsTotal &&
    this.threadsTotal == that.threadsTotal
}
