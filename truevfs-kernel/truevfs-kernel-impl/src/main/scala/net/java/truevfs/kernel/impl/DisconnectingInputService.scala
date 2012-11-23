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
import net.java.truecommons.cio._

/**
  * Decorates another input service in order to disconnect any resources when
  * this input service gets closed.
  * Once `close`d, all methods of all products of this service, including all
  * sockets, streams etc. but excluding `output` and all `close` methods of all
  * products will throw an [[net.java.truecommons.io.ClosedInputException]]
  * when called.
  *
  * @tparam E the type of the entries.
  * @see    DisconnectingOutputService
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private class DisconnectingInputService[E <: Entry]
(@WillCloseWhenClosed input: InputService[E])
extends DecoratingInputService[E](input)
with CheckedCloseable {

  override def size = checked(container.size)
  override def iterator = checked(container.iterator)
  override def entry(name: String) = checked(container entry name)

  override def input(name: String): InputSocket[E] = {
    final class Input extends AbstractInputSocket[E] {
      private[this] val socket = container input name

      override def target() = checked(socket target ())

      override def stream(peer: AnyOutputSocket) =
        new DisconnectingInputStreamImpl(checked(socket stream peer))

      override def channel(peer: AnyOutputSocket) =
        new DisconnectingSeekableChannelImpl(checked(socket channel peer))
    }
    new Input
  }

  override protected def check() { if (!isOpen) throw new ClosedInputException }

  private final class DisconnectingInputStreamImpl
  (@WillCloseWhenClosed in: InputStream)
  extends DisconnectingInputStream(in) {
    override def isOpen = DisconnectingInputService.this.isOpen
    @DischargesObligation override def close() = if (isOpen) in close ()
  }

  private final class DisconnectingSeekableChannelImpl
  (@WillCloseWhenClosed channel: SeekableByteChannel)
  extends DisconnectingSeekableChannel(channel) {
    override def isOpen = DisconnectingInputService.this.isOpen
    @DischargesObligation override def close() = if (isOpen) channel close ()
  }
}
