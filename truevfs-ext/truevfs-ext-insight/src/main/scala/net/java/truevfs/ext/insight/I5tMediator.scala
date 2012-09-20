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
private abstract class I5tMediator(val subject: String)
extends JmxMediator[I5tMediator] {

  assert(null ne subject)

  private[this] val logger = new FsLogger

  def newStats(offset: Int): I5tStatistics

  private def startStats(offset: Int) { start(newStats(offset)) }

  def startStats(origin: JmxColleague) { startStats(0) }

  final def startAllStats(origin: JmxColleague) {
    for (mediator <- mediators) mediator startStats origin
  }

  def rotateStats(origin: JmxColleague) { startStats(logger rotate ()) }

  final def rotateAllStats(origin: JmxColleague) {
    for (mediator <- mediators) mediator rotateStats origin
  }

  final def logRead(nanos: Long, bytes: Int) { logger logRead (nanos, bytes) }
  final def logWrite(nanos: Long, bytes: Int) { logger logWrite (nanos, bytes) }
  final def logSync(nanos: Long) { logger logSync nanos }
  final def stats(offset: Int) = { logger stats offset }

  final def formatOffset(offset: Int) = { logger format offset }

  final override def instrument(subject: FsManager) =
    start(new I5tManager(syncOperationsMediator, subject))

  final override def instrument(subject: IoBufferPool) =
    new InstrumentingBufferPool[I5tMediator](bufferIoMediator, subject)

  final override def instrument(origin: InstrumentingManager[I5tMediator], subject: FsMetaDriver) =
    new InstrumentingMetaDriver[I5tMediator](this, subject)

  final override def instrument(origin: InstrumentingManager[I5tMediator], subject: FsController) =
    new InstrumentingController[I5tMediator](applicationIoMediator, subject)

  final override def instrument(origin: InstrumentingBufferPool[I5tMediator], subject: IoBuffer) =
    start(new JmxBuffer[I5tMediator](this, subject))

  final override def instrument(origin: InstrumentingMetaDriver[I5tMediator], subject: FsModel) =
    start(new JmxModel[I5tMediator](this, subject))

  final override def instrument(origin: InstrumentingMetaDriver[I5tMediator], subject: FsController) =
    new InstrumentingController[I5tMediator](kernelIoMediator, subject)

  final override def instrument[E <: Entry](origin: InstrumentingController[I5tMediator], subject: InputSocket[E]) =
    new InstrumentingInputSocket[I5tMediator, E](this, subject)

  final override def instrument[E <: Entry](origin: InstrumentingController[I5tMediator], subject: OutputSocket[E]) =
    new InstrumentingOutputSocket[I5tMediator, E](this, subject)

  final override def instrument[B <: IoBuffer](origin: InstrumentingBuffer[I5tMediator], subject: InputSocket[B]) =
    new InstrumentingInputSocket[I5tMediator, B](this, subject)

  final override def instrument[B <: IoBuffer](origin: InstrumentingBuffer[I5tMediator], subject: OutputSocket[B]) =
    new InstrumentingOutputSocket[I5tMediator, B](this, subject)

  final override def instrument[E <: Entry](origin: InstrumentingInputSocket[I5tMediator, E], subject: InputStream) =
    start(new I5tInputStream(this, subject))

  final override def instrument[E <: Entry](origin: InstrumentingInputSocket[I5tMediator, E], subject: SeekableByteChannel) =
    start(new I5tSeekableChannel(this, subject))

  final override def instrument[E <: Entry](origin: InstrumentingOutputSocket[I5tMediator, E], subject: OutputStream) =
    start(new I5tOutputStream(this, subject))

  final override def instrument[E <: Entry](origin: InstrumentingOutputSocket[I5tMediator, E], subject: SeekableByteChannel) =
    start(new I5tSeekableChannel(this, subject))

  override def toString: String = "%s[subject=%s]".format(getClass.getName, subject)
}
