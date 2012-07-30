/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import java.util.concurrent.locks._
import javax.annotation._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec.cio._

/**
  * Decorates another input service to allow concurrent access which is
  * synchronized by a protected {@link Lock} object.
  *
  * @tparam E the type of the entries in the decorated input service.
  * @see    LockOutputService
  * @author Christian Schlichtherle
  */
@Immutable
private class LockInputService[E <: Entry]
(@WillCloseWhenClosed input: InputService[E])
extends DecoratingInputService[E, InputService[E]](input)
with LockedOperation {

  @DischargesObligation
  override def close() = locked(container close ())
  override def size = locked(container.size)
  override def iterator = throw new UnsupportedOperationException("The returned iterator would not be thread-safe!")
  override def entry(name: String) = locked(container entry name)

  override def input(name: String): InputSocket[E] = {
    final class Input extends AbstractInputSocket[E] {
      private[this] val socket = container input name

      override def target() = locked(socket target ())

      override def stream(peer: AnyOutputSocket) =
        new LockInputStream(lock, locked(socket stream peer))

      override def channel(peer: AnyOutputSocket) =
        new LockSeekableChannel(lock, locked(socket channel peer))
    }
    new Input
  }
}
