/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import java.util

import javax.annotation._
import javax.annotation.concurrent._
import net.java.truecommons.cio._

/**
  * Decorates another output service in order to disconnect any resources when
  * this output service gets closed.
  * Once `close`d, all methods of all products of this service, including all
  * sockets, streams etc. but excluding `output` and all `close` methods of all
  * products will throw an [[net.java.truecommons.io.ClosedOutputException]]
  * when called.
  *
  * @tparam E the type of the entries.
  * @see DisconnectingInputService
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private class DisconnectingOutputService[E <: Entry](@WillCloseWhenClosed output: OutputService[E])
  extends DecoratingOutputService[E](output)
    with CheckedCloseable {

  override def size: Int = checked(container.size)

  override def iterator: util.Iterator[E] = checked(container.iterator)

  override def entry(name: String): E = checked(container entry name)

  override def output(entry: E): OutputSocket[E] = {
    new AbstractOutputSocket[E] {

      private val socket = container output entry

      override def target(): E = checked(socket.target())

      override def stream(peer: AnyInputSocket): OutputStream = {
        new DisconnectingOutputStreamImpl(checked(socket stream peer))
      }

      override def channel(peer: AnyInputSocket): SeekableByteChannel = {
        new DisconnectingSeekableChannelImpl(checked(socket channel peer))
      }
    }
  }

  override protected def check(): Unit = {
    if (!isOpen) {
      throw new ClosedOutputException
    }
  }

  private final class DisconnectingOutputStreamImpl(@WillCloseWhenClosed out: OutputStream)
    extends DisconnectingOutputStream(out) {

    override def isOpen: Boolean = DisconnectingOutputService.this.isOpen

    @DischargesObligation override def close(): Unit = {
      if (isOpen) {
        out.close()
      }
    }
  }

  private final class DisconnectingSeekableChannelImpl(@WillCloseWhenClosed channel: SeekableByteChannel)
    extends DisconnectingSeekableChannel(channel) {

    override def isOpen: Boolean = DisconnectingOutputService.this.isOpen

    @DischargesObligation override def close(): Unit = {
      if (isOpen) {
        channel.close()
      }
    }
  }

}
