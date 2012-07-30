/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import javax.annotation._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec.cio._

/**
  * Decorates another output service in order to disconnect any resources when
  * this output service gets closed.
  * Once `close`d, all methods of all products of this service, including all
  * sockets, streams etc. but excluding `output` and all `close` methods of all
  * products will throw an [[net.java.truecommons.io.ClosedOutputException]]
  * when called.
  *
  * @tparam E the type of the entries.
  * @see    DisconnectingInputService
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private class DisconnectingOutputService[E <: Entry]
(@WillCloseWhenClosed output: OutputService[E])
extends DecoratingOutputService[E, OutputService[E]](output)
with CheckedCloseable {

  override def size = checked(container.size)
  override def iterator = checked(container.iterator)
  override def entry(name: String) = checked(container entry name)

  override def output(entry: E): OutputSocket[E] = {
    final class Output extends AbstractOutputSocket[E] {
      private[this] val socket = container output entry

      override def target() = checked(socket target ())

      override def stream(peer: AnyInputSocket) =
        new DisconnectingOutputStreamImpl(checked(socket stream peer))

      override def channel(peer: AnyInputSocket) =
        new DisconnectingSeekableChannelImpl(checked(socket channel peer))
    }
    new Output
  }

  override protected def check() { if (!isOpen) throw new ClosedOutputException }

  private final class DisconnectingOutputStreamImpl
  (@WillCloseWhenClosed out: OutputStream)
  extends DisconnectingOutputStream(out) {
    override def isOpen = DisconnectingOutputService.this.isOpen
    @DischargesObligation override def close() = if (isOpen) out close ()
  }

  private final class DisconnectingSeekableChannelImpl
  (@WillCloseWhenClosed channel: SeekableByteChannel)
  extends DisconnectingSeekableChannel(channel) {
    override def isOpen = DisconnectingOutputService.this.isOpen
    @DischargesObligation override def close() = if (isOpen) channel close ()
  }
}
