/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel._
import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._;
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
import java.io._
import java.nio.channels._
import java.util.logging._
import java.util.logging.Level._

/**
 * Finalizes unclosed resources returned by its decorated controller.
 * 
 * @author Christian Schlichtherle
 */
private trait FinalizeController extends FsController[FsModel] {
  import FinalizeController._

  abstract override def input(options: AccessOptions, name: FsEntryName) = {
    final class Input extends DecoratingInputSocket[Entry](super.input(options, name)) {
      override def stream() = new FinalizeInputStream(boundSocket.stream())
      override def channel() = new FinalizeSeekableChannel(boundSocket.channel())
    }
    new Input
  }: AnyInputSocket

  abstract override def output(options: AccessOptions, name: FsEntryName, template: Entry) = {
    final class Output extends DecoratingOutputSocket[Entry](super.output(options, name, template)) {
      override def stream() = new FinalizeOutputStream(boundSocket.stream())
      override def channel() = new FinalizeSeekableChannel(boundSocket.channel())
    }
    new Output
  }: AnyOutputSocket
}

private object FinalizeController {
  private val logger = Logger.getLogger(
    classOf[FinalizeController].getName,
    classOf[FinalizeController].getName);

  private val OK = new IOException(null: Throwable)

  private trait FinalizeCloseable extends Closeable {
    @volatile var result: Option[IOException] = None // accessed by finalizer thread!

    abstract override def close() {
      try {
        super.close()
      } catch {
        case ex: IOException => result = Some(ex); throw ex
      }
      result = Some(OK)
    }

    override def finalize() {
      try {
        result match {
          case Some(OK) => logger.log(FINEST, "closeCleared");
          case Some(ex) => logger.log(FINER, "closeFailed", ex);
          case None =>
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
  } // FinalizeCloseable

  private final class FinalizeInputStream(in: InputStream)
  extends DecoratingInputStream(in) with FinalizeCloseable

  private final class FinalizeOutputStream(out: OutputStream)
  extends DecoratingOutputStream(out) with FinalizeCloseable

  private final class FinalizeSeekableChannel(channel: SeekableByteChannel)
  extends DecoratingSeekableChannel(channel) with FinalizeCloseable
}
