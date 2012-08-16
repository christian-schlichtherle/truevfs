/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

/**
  * An immutable record of statistics for I/O operations.
  * 
  * @param  sequenceNumber the non-negative sequence number.
  * @throws IllegalArgumentException if any parameter value is negative.
  * @author Christian Schlichtherle
  */
final case class IoStatistics private (
  sequenceNumber: Long,
  nanosecondsTotal: Long,
  bytesTotal: Long,
  threadsTotal: Int,
  timeMillis: Long = System.currentTimeMillis
) {
  require(0 <= (sequenceNumber | nanosecondsTotal | bytesTotal | threadsTotal | timeMillis))

  def nanosecondsPerOperation =
    if (0 == sequenceNumber) 0L else nanosecondsTotal / sequenceNumber

  def bytesPerOperation =
    if (0 == sequenceNumber) 0 else (bytesTotal / sequenceNumber).toInt

  def kilobytesPerSecond = {
    import IoStatistics._
    if (0 == nanosecondsTotal) 0L
    else (BigInt(bytesTotal) * tenPowNine /
          (BigInt(nanosecondsTotal) * oneK)).toLong
  }

  /**
    * Logs an I/O operation with the given sample data and returns a new
    * object to reflect the updated statistics at the current system time.
    * If any property would overflow to a negative value as a result of the
    * update, then the returned object will simply have its sequence number
    * set to one (!) and its other properties will be reset to reflect only
    * the given parameter values at the current system time.
    * In other words, the statistics would restart from fresh.
    * 
    * @param  nanosDelta the execution time.
    * @param  bytesDelta the number of bytes read or written.
    * @return A new object which reflects the updated statistics at the
    *         current system time.
    * @throws IllegalArgumentException if any parameter value is negative.
    */
  @throws(classOf[IllegalArgumentException])
  def log(nanosDelta: Long, bytesDelta: Long, threadsTotal: Int) = {
    require(0 <= (nanosDelta | bytesDelta | threadsTotal))
    try {
      new IoStatistics(sequenceNumber + 1,
                       nanosecondsTotal + nanosDelta,
                       bytesTotal + bytesDelta,
                       threadsTotal)
    } catch {
      case _: IllegalArgumentException =>
        new IoStatistics(1, nanosDelta, bytesDelta, 1)
    }
  }

  def equalsIgnoreTime(that: IoStatistics) =
    this.sequenceNumber == that.sequenceNumber &&
      this.nanosecondsTotal == that.nanosecondsTotal &&
      this.bytesTotal == that.bytesTotal &&
      this.threadsTotal == that.threadsTotal
}

object IoStatistics {
  private val tenPowNine = BigInt(10) pow 9
  private val oneK = BigInt(1024)

  /** Returns I/O statistics with all properties set to zero. */
  def apply() = new IoStatistics(0, 0, 0, 0) // cannot cache because of timeMillis!
}
