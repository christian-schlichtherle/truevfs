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

  override def instrument(context: InstrumentingManager[LogMediator], subject: FsCompositeDriver): FsCompositeDriver =
    new InstrumentingCompositeDriver[LogMediator](this, subject)

  override def instrument(context: InstrumentingBufferPool[LogMediator], subject: IoBuffer): IoBuffer =
    new LogBuffer(this, subject)

  override def instrument(context: InstrumentingCompositeDriver[LogMediator], subject: FsController): FsController =
    new InstrumentingController[LogMediator](this, subject)

  override def instrument[E <: Entry](context: InstrumentingController[LogMediator], subject: InputSocket[E]): InputSocket[E] =
    new InstrumentingInputSocket[LogMediator, E](this, subject)

  override def instrument[E <: Entry](context: InstrumentingController[LogMediator], subject: OutputSocket[E]): OutputSocket[E] =
    new InstrumentingOutputSocket[LogMediator, E](this, subject)

  override def instrument[B <: IoBuffer](context: InstrumentingBuffer[LogMediator], subject: InputSocket[B]): InputSocket[B] =
    new InstrumentingInputSocket[LogMediator, B](this, subject)

  override def instrument[B <: IoBuffer](context: InstrumentingBuffer[LogMediator], subject: OutputSocket[B]): OutputSocket[B] =
    new InstrumentingOutputSocket[LogMediator, B](this, subject)

  override def instrument[E <: Entry](context: InstrumentingInputSocket[LogMediator, E], subject: InputStream): InputStream =
    new LogInputStream(context, subject)

  override def instrument[E <: Entry](context: InstrumentingInputSocket[LogMediator, E], subject: SeekableByteChannel): SeekableByteChannel =
    new LogInputChannel(context, subject)

  override def instrument[E <: Entry](context: InstrumentingOutputSocket[LogMediator, E], subject: OutputStream): OutputStream =
    new LogOutputStream(context, subject)

  override def instrument[E <: Entry](context: InstrumentingOutputSocket[LogMediator, E], subject: SeekableByteChannel): SeekableByteChannel =
    new LogOutputChannel(context, subject)
}

private object LogMediator extends LogMediator
