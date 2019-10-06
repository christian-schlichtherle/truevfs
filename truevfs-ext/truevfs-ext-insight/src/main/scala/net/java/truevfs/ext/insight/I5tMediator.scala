/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.insight

import java.io._
import java.nio.channels._

import net.java.truecommons.cio._
import net.java.truevfs.comp.inst._
import net.java.truevfs.comp.jmx._
import net.java.truevfs.ext.insight.stats._
import net.java.truevfs.kernel.spec._

/** A mediator for the instrumentation of the TrueVFS Kernel with JMX.
  *
  * @author Christian Schlichtherle
  */
private abstract class I5tMediator(val subject: String) extends JmxMediator[I5tMediator] {

  assert(null ne subject)

  private[this] val logger = new FsLogger

  def newStats(offset: Int): I5tStatistics

  private def activateStats(offset: Int): Unit = { activate(newStats(offset)) }

  final def activateStats(origin: JmxComponent): Unit = { activateStats(0) }

  final def activateAllStats(origin: JmxComponent): Unit = {
    for (mediator <- mediators)
      mediator activateStats origin
  }

  def rotateStats(origin: JmxComponent): Unit = { activateStats(logger.rotate()) }

  final def rotateAllStats(origin: JmxComponent): Unit = {
    for (mediator <- mediators)
      mediator rotateStats origin
  }

  final def logRead(nanos: Long, bytes: Int): Unit = { logger logRead (nanos, bytes) }
  final def logWrite(nanos: Long, bytes: Int): Unit = { logger logWrite (nanos, bytes) }
  final def logSync(nanos: Long): Unit = { logger logSync nanos }
  final def stats(offset: Int): FsStatistics = { logger stats offset }

  final def formatOffset(offset: Int): String = { logger format offset }

  final override def toString: String = "%s[subject=%s]" format (getClass.getName, subject)

  final override def instrument(subject: FsManager): FsManager =
    activate(new I5tManager(syncOperationsMediator, subject))

  final override def instrument(subject: IoBufferPool): IoBufferPool =
    new InstrumentingBufferPool[I5tMediator](bufferIoMediator, subject)

  final override def instrument(context: InstrumentingManager[I5tMediator], subject: FsCompositeDriver): FsCompositeDriver =
    new InstrumentingCompositeDriver(this, subject)

  final override def instrument(context: InstrumentingManager[I5tMediator], subject: FsController): FsController =
    new InstrumentingController[I5tMediator](applicationIoMediator, subject)

  final override def instrument(context: InstrumentingBufferPool[I5tMediator], subject: IoBuffer): IoBuffer =
    activate(new JmxBuffer(this, subject))

  final override def instrument(context: InstrumentingCompositeDriver[I5tMediator], subject: FsModel): FsModel =
    activate(new JmxModel(this, subject))

  final override def instrument(context: InstrumentingCompositeDriver[I5tMediator], subject: FsController): FsController =
    new InstrumentingController[I5tMediator](kernelIoMediator, subject)

  final override def instrument[E <: Entry](context: InstrumentingController[I5tMediator], subject: InputSocket[E]): InputSocket[E] =
    new InstrumentingInputSocket(this, subject)

  final override def instrument[E <: Entry](context: InstrumentingController[I5tMediator], subject: OutputSocket[E]): OutputSocket[E] =
    new InstrumentingOutputSocket(this, subject)

  final override def instrument[B <: IoBuffer](context: InstrumentingBuffer[I5tMediator], subject: InputSocket[B]): InputSocket[B] =
    new InstrumentingInputSocket(this, subject)

  final override def instrument[B <: IoBuffer](context: InstrumentingBuffer[I5tMediator], subject: OutputSocket[B]): OutputSocket[B] =
    new InstrumentingOutputSocket(this, subject)

  final override def instrument[E <: Entry](context: InstrumentingInputSocket[I5tMediator, E], subject: InputStream): InputStream =
    activate(new I5tInputStream(this, subject))

  final override def instrument[E <: Entry](context: InstrumentingInputSocket[I5tMediator, E], subject: SeekableByteChannel): SeekableByteChannel =
    activate(new I5tSeekableChannel(this, subject))

  final override def instrument[E <: Entry](context: InstrumentingOutputSocket[I5tMediator, E], subject: OutputStream): OutputStream =
    activate(new I5tOutputStream(this, subject))

  final override def instrument[E <: Entry](context: InstrumentingOutputSocket[I5tMediator, E], subject: SeekableByteChannel): SeekableByteChannel =
    activate(new I5tSeekableChannel(this, subject))
}
