/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight.stats

object FsStatistics {
  /** Returns file system statistics with all properties set to zero. */
  def apply() = {
    val io = IoStatistics()
    val sync = SyncStatistics()
    new FsStatistics(io, io, sync) // cannot cache because of timeMillis!
  }
}

/**
  * An immutable record of statistics for file system operations.
  *
  * @throws IllegalArgumentException if any parameter value is negative.
  * @author Christian Schlichtherle
  */
final case class FsStatistics private (
  readStats: IoStatistics,
  writeStats: IoStatistics,
  syncStats: SyncStatistics,
  timeMillis: Long = System.currentTimeMillis
) extends Immutable {
  require(0 <= timeMillis)
  require(null ne readStats)
  require(null ne writeStats)
  require(null ne syncStats)

  /**
    * Logs a read operation with the given sample data and returns a new
    * object to reflect the updated statistics.
    *
    * @param  nanosDelta the execution time in nanoseconds.
    * @param  bytesDelta the number of bytes read.
    * @return A new object which reflects the updated statistics.
    * @throws IllegalArgumentException if any parameter value is negative.
    */
  @throws(classOf[IllegalArgumentException])
  def logRead(nanosDelta: Long, bytesDelta: Long, threadsTotal: Int) =
    new FsStatistics(readStats.log(nanosDelta, bytesDelta, threadsTotal), writeStats, syncStats, timeMillis)

  /**
    * Logs a write operation with the given sample data and returns a new
    * object to reflect the updated statistics.
    *
    * @param  nanosDelta the execution time in nanoseconds.
    * @param  bytesDelta the number of bytes written.
    * @return A new object which reflects the updated statistics.
    * @throws IllegalArgumentException if any parameter is negative.
    */
  @throws(classOf[IllegalArgumentException])
  def logWrite(nanosDelta: Long, bytesDelta: Long, threadsTotal: Int) =
    new FsStatistics(readStats, writeStats.log(nanosDelta, bytesDelta, threadsTotal), syncStats, timeMillis)

  /**
    * Logs a sync operation with the given sample data and returns a new
    * object to reflect the updated statistics.
    *
    * @param  nanosDelta the execution time in nanoseconds.
    * @return A new object which reflects the updated statistics.
    * @throws IllegalArgumentException if any parameter value is negative.
    */
  @throws(classOf[IllegalArgumentException])
  def logSync(nanosDelta: Long, threadsTotal: Int) =
    new FsStatistics(readStats, writeStats, syncStats.log(nanosDelta, threadsTotal), timeMillis)

  def equalsIgnoreTime(that: FsStatistics) =
    this.readStats.equalsIgnoreTime(that.readStats) &&
      this.writeStats.equalsIgnoreTime(that.writeStats) &&
      this.syncStats.equalsIgnoreTime(that.syncStats)
}
