/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import de.schlichtherle.truevfs.kernel._
import java.io._
import java.nio.channels._
import java.util.logging._
import java.util.logging.Level._
import javax.annotation.concurrent._
import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._

/** Finalizes unclosed resources returned by its decorated controller.
  * 
  * @author Christian Schlichtherle
  */
@ThreadSafe
private trait FinalizeController extends FsController[FsModel] {
  import FinalizeController._

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    final class Input extends DelegatingInputSocket[Entry] {
      val socket = FinalizeController.super.input(options, name)

      override def stream(peer: AnyOutputSocket) =
        new FinalizeInputStream(socket.stream(peer))

      override def channel(peer: AnyOutputSocket) =
        new FinalizeSeekableChannel(socket.channel(peer))
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Entry) = {
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
  private val logger = Logger.getLogger(
    classOf[FinalizeController].getName,
    classOf[FinalizeController].getName);

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
          case Some(ex) => logger.log(FINER, "closeFailed", ex);
          case None => logger.log(FINEST, "closeCleared");
          case _ =>
            try {
              super.close();
              logger.log(FINE, "finalizeCleared");
            } catch {
              case ex: ControlFlowException => // report and swallow
                logger.log(WARNING, "finalizeFailed",
                           new AssertionError("Unexpected control flow exception!", ex));
              case ex: Throwable => // report and swallow
                logger.log(INFO, "finalizeFailed", ex);
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
