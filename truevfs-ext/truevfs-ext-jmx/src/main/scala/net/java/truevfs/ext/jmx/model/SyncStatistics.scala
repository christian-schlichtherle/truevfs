/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.jmx.model

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
  threads: Set[Long],
  timeMillis: Long = System.currentTimeMillis
) {
  require(0 <= (sequenceNumber | nanosecondsTotal | timeMillis))
  require(null ne threads)

  def nanosecondsPerOperation =
    if (0 == sequenceNumber) 0L else nanosecondsTotal / sequenceNumber

  def threadsTotal = threads.size

  /**
    * Logs a sync operation with the given sample data and returns a new
    * object to reflect the updated statistics at the current system time.
    * If any property would overflow to a negative value as a result of the
    * update, then the returned object will simply have its sequence number
    * set to one (!) and its other properties will be reset to reflect only
    * the given parameter values at the current system time.
    * In other words, the statistics would restart from fresh.
    * 
    * @param  nanos the execution time.
    * @return A new object which reflects the updated statistics at the
    *         current system time.
    * @throws IllegalArgumentException if any parameter value is negative.
    */
  @throws(classOf[IllegalArgumentException])
  def log(nanos: Long) = {
    require(0 <= nanos)
    try {
      new SyncStatistics(sequenceNumber + 1,
                         nanosecondsTotal + nanos,
                         threads + hash(Thread.currentThread))
    } catch {
      case _: IllegalArgumentException =>
        new SyncStatistics(1, nanos, Set(hash(Thread.currentThread)))
    }
  }

  def equalsIgnoreTime(that: SyncStatistics) =
    this.sequenceNumber == that.sequenceNumber &&
      this.nanosecondsTotal == that.nanosecondsTotal &&
      this.threads == that.threads
}

object SyncStatistics {
  /** Returns sync statistics with all properties set to zero. */
  def apply() = new SyncStatistics(0, 0, Set()) // cannot cache because of timeMillis!
}
