/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.io._
import java.nio.channels._

import javax.annotation.concurrent._
import net.java.truecommons.cio._
import net.java.truecommons.io._
import net.java.truecommons.logging._
import net.java.truecommons.shed.ControlFlowException
import net.java.truevfs.kernel.impl.FinalizeController._
import net.java.truevfs.kernel.spec._

/** Finalizes unclosed resources returned by its decorated controller.
  *
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait FinalizeController extends FsController {

  abstract override def input(options: AccessOptions, name: FsNodeName): AnyInputSocket = {
    new DelegatingInputSocket[Entry] {

      val socket: InputSocket[_ <: Entry] = FinalizeController.super.input(options, name)

      override def stream(peer: AnyOutputSocket) =
        new FinalizeInputStream(socket.stream(peer))

      override def channel(peer: AnyOutputSocket) =
        new FinalizeSeekableChannel(socket.channel(peer))
    }
  }

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Entry): AnyOutputSocket = {
    new DelegatingOutputSocket[Entry] {

      val socket: OutputSocket[_ <: Entry] = FinalizeController.super.output(options, name, template)

      override def stream(peer: AnyInputSocket) =
        new FinalizeOutputStream(socket.stream(peer))

      override def channel(peer: AnyInputSocket) =
        new FinalizeSeekableChannel(socket.channel(peer))
    }
  }
}

private object FinalizeController {

  private val logger = new LocalizedLogger(classOf[FinalizeController])

  private trait FinalizeResource extends Closeable {

    @volatile var ioException: Option[IOException] = _ // accessed by finalizer thread!

    abstract override def close(): Unit = {
      try {
        super.close()
        ioException = None
      } catch {
        case ex: IOException => ioException = Some(ex); throw ex
      }
    }

    abstract override def finalize(): Unit = {
      try {
        ioException match {
          case Some(ex) => logger trace("closeFailed", ex)
          case None => logger trace "closeCleared"
          case _ =>
            try {
              super.close()
              logger info "finalizeCleared"
            } catch {
              case ex: ControlFlowException => // log and swallow!
                logger error("finalizeFailed",
                  new AssertionError("Unexpected control flow exception!", ex))
              case ex: Throwable => // log and swallow!
                logger warn("finalizeFailed", ex)
            }
        }
      } finally {
        super.finalize()
      }
    }
  }

  private final class FinalizeInputStream(in: InputStream)
    extends DecoratingInputStream(in) with FinalizeResource

  private final class FinalizeOutputStream(out: OutputStream)
    extends DecoratingOutputStream(out) with FinalizeResource

  private final class FinalizeSeekableChannel(channel: SeekableByteChannel)
    extends DecoratingSeekableChannel(channel) with FinalizeResource
}
