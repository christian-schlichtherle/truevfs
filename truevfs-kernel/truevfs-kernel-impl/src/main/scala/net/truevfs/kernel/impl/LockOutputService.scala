/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import java.util.concurrent.locks._
import javax.annotation._
import javax.annotation.concurrent._
import net.truevfs.kernel.spec.cio._
import net.truevfs.kernel.spec.io._

/**
  * Decorates another output service to allow concurrent access which is
  * synchronized by a protected {@link Lock} object.
  *
  * @tparam E the type of the entries in the decorated output service.
  * @see    LockInputService
  * @author Christian Schlichtherle
  */
@Immutable
private class LockOutputService[E <: Entry]
(@WillCloseWhenClosed output: OutputService[E])
extends DecoratingOutputService[E, OutputService[E]](output)
with LockedOperation {

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
