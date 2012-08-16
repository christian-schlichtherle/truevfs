/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.ext.insight.stats._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._

/**
 * A mediator for the instrumentation of the TrueVFS Kernel with JMX.
 *
 * @author Christian Schlichtherle
 */
@ThreadSafe
private abstract class I5tMediator(val subject: String) extends JmxMediator[I5tMediator] {

  import I5tMediator._

  assert(null ne subject)

  private[this] val logger = new FsLogger

  def newStats(offset: Int): I5tStatistics

  private def startStats(offset: Int) { start(newStats(offset)) }

  def startStats(origin: JmxColleague) { startStats(0) }

  final def startAllStats(origin: JmxColleague) {
    for (mediator <- mediators) mediator startStats origin
  }

  def rotateStats(origin: JmxColleague) { startStats(logger.rotate) }

  final def rotateAllStats(origin: JmxColleague) {
    for (mediator <- mediators) mediator rotateStats origin
  }

  final def logRead(nanos: Long, bytes: Int) { logger logRead (nanos, bytes) }
  final def logWrite(nanos: Long, bytes: Int) { logger logWrite (nanos, bytes) }
  final def logSync(nanos: Long) { logger logSync nanos }
  final def stats(offset: Int) = { logger stats offset }

  final def formatOffset(offset: Int) = { logger format offset }

  final override def instrument(`object`: FsManager) = start(new I5tManager(syncOps, `object`))

  final override def instrument(`object`: IoBufferPool) =
    new InstrumentingBufferPool[I5tMediator](bufferIo, `object`)

  final override def instrument(origin: InstrumentingManager[I5tMediator], `object`: FsMetaDriver) =
    new InstrumentingMetaDriver[I5tMediator](this, `object`)

  final override def instrument(origin: InstrumentingManager[I5tMediator], `object`: FsController) =
    new InstrumentingController[I5tMediator](appIo, `object`)

  final override def instrument(origin: InstrumentingBufferPool[I5tMediator], `object`: IoBuffer) =
    start(new JmxBuffer[I5tMediator](this, `object`))

  final override def instrument(origin: InstrumentingMetaDriver[I5tMediator], `object`: FsModel) =
    start(new JmxModel[I5tMediator](this, `object`))

  final override def instrument(origin: InstrumentingMetaDriver[I5tMediator], `object`: FsController) =
    new InstrumentingController[I5tMediator](kernelIo, `object`)

  final override def instrument[E <: Entry](origin: InstrumentingController[I5tMediator], `object`: InputSocket[E]) =
    new InstrumentingInputSocket[I5tMediator, E](this, `object`)

  final override def instrument[E <: Entry](origin: InstrumentingController[I5tMediator], `object`: OutputSocket[E]) =
    new InstrumentingOutputSocket[I5tMediator, E](this, `object`)

  final override def instrument[B <: IoBuffer](origin: InstrumentingBuffer[I5tMediator], `object`: InputSocket[B]) =
    new InstrumentingInputSocket[I5tMediator, B](this, `object`)

  final override def instrument[B <: IoBuffer](origin: InstrumentingBuffer[I5tMediator], `object`: OutputSocket[B]) =
    new InstrumentingOutputSocket[I5tMediator, B](this, `object`)

  final override def instrument[E <: Entry](origin: InstrumentingInputSocket[I5tMediator, E], `object`: InputStream) =
    start(new I5tInputStream(this, `object`))

  final override def instrument[E <: Entry](origin: InstrumentingInputSocket[I5tMediator, E], `object`: SeekableByteChannel) =
    start(new I5tSeekableChannel(this, `object`))

  final override def instrument[E <: Entry](origin: InstrumentingOutputSocket[I5tMediator, E], `object`: OutputStream) =
    start(new I5tOutputStream(this, `object`))

  final override def instrument[E <: Entry](origin: InstrumentingOutputSocket[I5tMediator, E], `object`: SeekableByteChannel) =
    start(new I5tSeekableChannel(this, `object`))

  override def toString: String = "%s[subject=%s]".format(getClass.getName, subject)
}

private object I5tMediator {

  def apply() = syncOps

  private val appIo = new I5tIoMediator("Application I/O")
  private val bufferIo = new I5tIoMediator("Buffer I/O")
  private val kernelIo = new I5tIoMediator("Kernel I/O")
  private val syncOps = new I5tSyncMediator("Sync Operations")

  private val mediators = Array(syncOps, appIo, kernelIo, bufferIo)
}
