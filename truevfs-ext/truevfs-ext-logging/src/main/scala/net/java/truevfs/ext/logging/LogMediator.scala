/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.logging

import java.io._
import java.nio.channels._
import net.java.truevfs.comp.inst._
import net.java.truevfs.kernel.spec._
import net.java.truecommons.cio._

/**
 * @author Christian Schlichtherle
 */
private class LogMediator extends Mediator[LogMediator] with Immutable {

  override def instrument(subject: FsManager): FsManager =
    new InstrumentingManager[LogMediator](this, subject)

  override def instrument(subject: IoBufferPool): IoBufferPool =
    new InstrumentingBufferPool[LogMediator](this, subject)

  override def instrument(origin: InstrumentingManager[LogMediator], subject: FsCompositeDriver): FsCompositeDriver =
    new InstrumentingCompositeDriver[LogMediator](this, subject)

  override def instrument(origin: InstrumentingBufferPool[LogMediator], subject: IoBuffer): IoBuffer =
    new LogBuffer(this, subject)

  override def instrument(origin: InstrumentingCompositeDriver[LogMediator], subject: FsController): FsController =
    new InstrumentingController[LogMediator](this, subject)

  override def instrument[E <: Entry](origin: InstrumentingController[LogMediator], subject: InputSocket[E]): InputSocket[E] =
    new InstrumentingInputSocket[LogMediator, E](this, subject)

  override def instrument[E <: Entry](origin: InstrumentingController[LogMediator], subject: OutputSocket[E]): OutputSocket[E] =
    new InstrumentingOutputSocket[LogMediator, E](this, subject)

  override def instrument[B <: IoBuffer](origin: InstrumentingBuffer[LogMediator], subject: InputSocket[B]): InputSocket[B] =
    new InstrumentingInputSocket[LogMediator, B](this, subject)

  override def instrument[B <: IoBuffer](origin: InstrumentingBuffer[LogMediator], subject: OutputSocket[B]): OutputSocket[B] =
    new InstrumentingOutputSocket[LogMediator, B](this, subject)

  override def instrument[E <: Entry](origin: InstrumentingInputSocket[LogMediator, E], subject: InputStream): InputStream =
    new LogInputStream(origin, subject)

  override def instrument[E <: Entry](origin: InstrumentingInputSocket[LogMediator, E], subject: SeekableByteChannel): SeekableByteChannel =
    new LogInputChannel(origin, subject)

  override def instrument[E <: Entry](origin: InstrumentingOutputSocket[LogMediator, E], subject: OutputStream): OutputStream =
    new LogOutputStream(origin, subject)

  override def instrument[E <: Entry](origin: InstrumentingOutputSocket[LogMediator, E], subject: SeekableByteChannel): SeekableByteChannel =
    new LogOutputChannel(origin, subject)
}

private object LogMediator extends LogMediator
