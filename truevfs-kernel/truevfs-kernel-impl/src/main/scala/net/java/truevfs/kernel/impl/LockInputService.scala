/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.util
import java.util.concurrent.locks._

import edu.umd.cs.findbugs.annotations._
import javax.annotation._
import net.java.truecommons.cio._
import net.java.truecommons.io._

/**
  * Decorates another input service to allow concurrent access which is
  * synchronized by a [[java.util.concurrent.locks.Lock]] object.
  *
  * @tparam E the type of the entries in the decorated input service.
  * @see    LockOutputService
  * @author Christian Schlichtherle
  */
private class LockInputService[E <: Entry](@WillCloseWhenClosed input: InputService[E])
extends DecoratingInputService[E](input) with LockAspect[Lock] {

  final override val lock: Lock = new ReentrantLock

  @DischargesObligation
  override def close(): Unit = locked(container.close())

  override def size: Int = locked(container.size)

  override def iterator: util.Iterator[E] = throw new UnsupportedOperationException("The returned iterator would not be thread-safe!")

  override def entry(name: String): E = locked(container entry name)

  override def input(name: String): InputSocket[E] = {
    new AbstractInputSocket[E] {

      private val socket = container input name

      override def target(): E = locked(socket.target())

      override def stream(peer: AnyOutputSocket): LockInputStream = {
        new LockInputStream(lock, locked(socket stream peer))
      }

      override def channel(peer: AnyOutputSocket): LockSeekableChannel = {
        new LockSeekableChannel(lock, locked(socket channel peer))
      }
    }
  }
}
