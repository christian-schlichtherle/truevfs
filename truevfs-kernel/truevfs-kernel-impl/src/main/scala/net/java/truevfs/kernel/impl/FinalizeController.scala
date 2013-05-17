/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.logging._
import net.java.truecommons.shed._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import net.java.truevfs.kernel.spec._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._;

/** Finalizes unclosed resources returned by its decorated controller.
  * 
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait FinalizeController extends FsController {
  import FinalizeController._

  abstract override def input(options: AccessOptions, name: FsNodeName) = {
    final class Input extends DelegatingInputSocket[Entry] {
      val socket = FinalizeController.super.input(options, name)

      override def stream(peer: AnyOutputSocket) =
        new FinalizeInputStream(socket.stream(peer))

      override def channel(peer: AnyOutputSocket) =
        new FinalizeSeekableChannel(socket.channel(peer))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsNodeName, template: Entry) = {
    final class Output extends DelegatingOutputSocket[Entry] {
      val socket = FinalizeController.super.output(options, name, template)

      override def stream(peer: AnyInputSocket) =
        new FinalizeOutputStream(socket.stream(peer))

      override def channel(peer: AnyInputSocket) =
        new FinalizeSeekableChannel(socket.channel(peer))
    }
    new Output
  }: AnyOutputSocket
}

private object FinalizeController {
  private val logger = new LocalizedLogger(classOf[FinalizeController])

  private trait FinalizeResource extends Closeable {
    @volatile var ioException: Option[IOException] = _ // accessed by finalizer thread!

    abstract override def close() {
      try {
        super.close()
        ioException = None
      } catch {
        case ex: IOException => ioException = Some(ex); throw ex
      }
    }

    abstract override def finalize() {
      try {
        ioException match {
          case Some(ex) => logger trace ("closeFailed", ex)
          case None => logger trace "closeCleared"
          case _ =>
            try {
              super.close()
              logger info "finalizeCleared"
            } catch {
              case ex: ControlFlowException => // log and swallow!
                logger error ("finalizeFailed",
                           new AssertionError("Unexpected control flow exception!", ex))
              case ex: Throwable => // log and swallow!
                logger warn ("finalizeFailed", ex)
            }
        }
      } finally {
        super.finalize()
      }
    }
  } // FinalizeResource

  private final class FinalizeInputStream(in: InputStream)
  extends DecoratingInputStream(in) with FinalizeResource

  private final class FinalizeOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) with FinalizeResource

  private final class FinalizeSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) with FinalizeResource
}
