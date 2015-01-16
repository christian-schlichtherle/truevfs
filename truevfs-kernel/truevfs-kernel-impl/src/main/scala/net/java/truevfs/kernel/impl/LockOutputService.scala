/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import java.util.concurrent.locks._
import javax.annotation._
import net.java.truecommons.cio._

/**
  * Decorates another output service to allow concurrent access which is
  * synchronized by a protected {@link Lock} object.
  *
  * @tparam E the type of the entries in the decorated output service.
  * @see    LockInputService
  * @author Christian Schlichtherle
  */
private class LockOutputService[E <: Entry]
(@WillCloseWhenClosed output: OutputService[E])
extends DecoratingOutputService[E](output) with LockAspect[Lock] with Immutable {

  final override val lock = new ReentrantLock

  @DischargesObligation
  override def close() = locked(container close ())
  override def size = locked(container.size)
  override def iterator = throw new UnsupportedOperationException("The returned iterator would not be thread-safe!")
  override def entry(name: String) = locked(container entry name)

  override def output(entry: E): OutputSocket[E] = {
    final class Output extends AbstractOutputSocket[E] {
      private[this] val socket = container output entry

      override def target() = locked(socket target ())

      override def stream(peer: AnyInputSocket) =
        new LockOutputStream(lock, locked(socket stream peer))

      override def channel(peer: AnyInputSocket) =
        new LockSeekableChannel(lock, locked(socket channel peer))
    }
    new Output
  }
}
