/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.io._
import net.java.truecommons.shed._
import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio._
import CacheEntry._

/** Provides caching services for input and output sockets with the following
  * features:
  *
  * - Upon the first read operation, the entry data will be read from the
  *   backing store and temporarily stored in the cache.
  *   Subsequent or concurrent read operations will be served from the cache
  *   without re-reading the entry data from the backing store again until
  *   the cache gets `release`d.
  * - At the discretion of the
  *   [[net.java.truevfs.kernel.impl.CacheEntry.Strategy.]], entry data
  *   written to the cache may not be written to the backing store until the
  *   cache gets `flush`ed.
  * - After a write operation, the entry data will be stored in the cache for
  *   subsequent read operations until the cache gets `release`d.
  * - As a side effect, caching decouples the underlying storage from its
  *   clients, allowing it to create, read, update or delete the entry data
  *   while some clients are still busy on reading or writing the cached
  *   entry data.
  *
  * Note that you need to call `configure` before you can do any input or
  * output.
  *
  * @param strategy the caching strategy.
  * @param pool the pool for allocating and releasing temporary I/O entries.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
@CleanupObligation
private final class CacheEntry private (
  private[this] val strategy: Strategy,
  private[this] val pool: IoBufferPool
) extends Entry with Releasable[IOException] with Flushable with Closeable {

  private[this] val inputBufferPool = new InputBufferPool
  private[this] val outputBufferPool =
    strategy.newOutputBufferPool(this).asInstanceOf[OutputBufferPool]

  private[this] var _buffer: Option[Buffer] = None
  private[this] var _input: Option[AnyInputSocket] = None
  private[this] var _output: Option[AnyOutputSocket] = None

  private def buffer = _buffer

  private def buffer_=(nb: Option[Buffer]) {
    val ob = _buffer
    if (ob != nb) {
      _buffer = nb
      ob foreach { b => if (0 == b.writers && 0 == b.readers) b release () }
    }
  }

  override def getName = "Johnny Cache!"

  override def getSize(tµpe: Size) = {
    buffer match {
      case Some(b) => b getSize tµpe
      case None => UNKNOWN
    }
  }

  override def getTime(tµpe: Access) = {
    buffer match {
      case Some(b) => b getTime tµpe
      case None => UNKNOWN
    }
  }

  /** Returns `null` in order to block copying of access permissions of cache
    * entries.
    */
  override def isPermitted(tµpe: Access, entity: Entity) = null

  /** Configures the input socket for reading the entry data from the backing
    * store.
    * This method needs to be called before any input can be done - otherwise
    * a [[java.lang.NullPointerException]] will be thrown on the first read
    * attempt.
    * Note that calling this method does ''not'' `release` this cache.
    *
    * @param  input an input socket for reading the entry data from the
    *         backing store.
    * @return `this`
    */
  def configure(input: AnyInputSocket) = {
    require(null ne input)
    _input = Some(input)
    this
  }

  /** Configures the output socket for writing the entry data to the backing
    * store.
    * This method needs to be called before any output can be done - otherwise
    * a [[java.lang.NullPointerException]] will be thrown on the first write
    * attempt.
    * Note that calling this method does ''not'' `flush` this cache.
    *
    * @param  output an output socket for writing the entry data to the
    *         backing store.
    * @return `this`
    */
  def configure(output: AnyOutputSocket) = {
    require(null ne output)
    _output = Some(output)
    this
  }

  /** Returns an input socket for reading the cached entry data.
    *
    * @return An input socket for reading the cached entry data.
    */
  def input: AnyInputSocket = {
    final class Input extends DelegatingInputSocket[Entry]
    with BufferAllocator {
      override def socket() = buffer(inputBufferPool).input
      override def target() = target(_input.get)
    }
    new Input
  }

  /** Returns an output socket for writing the cached entry data.
    *
    * @return An output socket for writing the cached entry data.
    */
  def output: AnyOutputSocket = {
    final class Output extends DelegatingOutputSocket[Entry]
    with BufferAllocator {
      override def socket() = buffer(outputBufferPool).output
      override def target() = target(_output.get)
    }
    new Output
  }

  private trait BufferAllocator {
    private[this] var allocated: Option[Buffer] = None

    def buffer(pool: Pool[Buffer, IOException]) = {
      val b = pool allocate ()
      allocated = Some(b)
      b
    }

    def target(socket: AnyIoSocket) = {
      allocated match {
        case Some(b) => b
        case None => new ProxyEntry(socket target ())
      }
    }
  }

  /** Writes the cached entry data to the backing store unless already done.
    * Whether or not this method needs to be called depends on the caching
    * strategy.
    * E.g. the caching strategy
    * [[net.java.truevfs.kernel.impl.CacheEntry.Strategy.WRITE_THROUGH]]
    * writes any changed entry data immediately, so calling this method has no
    * effect.
    */
  override def flush() {
    buffer foreach { outputBufferPool release _ }
  }

  /** Clears the entry data from this cache without flushing it.
    *
    * @throws IOException on any I/O error.
    */
  override def release() { buffer = None }

  override def close() {
    flush()
    release()
  }

  private final class InputBufferPool extends Pool[Buffer, IOException] {
    override def allocate() = {
      val b = buffer match {
        case Some(b) => b
        case None =>
          val b = new Buffer
          try {
            b load _input.get
          } catch {
            case ex: Throwable =>
              try {
                b release ()
              } catch {
                case ex2: Throwable =>
                  ex addSuppressed ex2
              }
              throw ex
          }
          buffer = Some(b)
          b
      }
      assert(Strategy.WriteBack.eq(strategy) || 0 == b.writers)
      b.readers += 1
      b
    }

    override def release(b: Buffer) {
      assert(Strategy.WriteBack.eq(strategy) || 0 == b.writers)
      if (0 < b.readers) {
        b.readers -= 1
        if (1 == b.readers && 0 == b.writers && buffer.orNull.ne(b)) b release ()
      }
    }
  } // InputBufferPool

  private[CacheEntry] abstract class OutputBufferPool
  extends Pool[Buffer, IOException] {
    override def allocate() = {
      val b = new Buffer
      assert(0 == b.readers)
      b.writers = 1
      b
    }

    override def release(b: Buffer) {
      assert(Strategy.WriteBack.eq(strategy) || 0 == b.readers)
      b.writers = 0
      try {
        b save _output.get
      } finally {
        buffer = Some(b)
      }
    }
  } // OutputBufferPool

  private[CacheEntry] final class WriteThroughOutputBufferPool
  extends OutputBufferPool {
    override def release(b: Buffer) {
      if (0 != b.writers) super.release(b)
    }
  } // WriteThroughOutputBufferPool

  private[CacheEntry] final class WriteBackOutputBufferPool
  extends OutputBufferPool {
    override def release(b: Buffer) {
      if (0 != b.writers) {
        if (buffer.orNull ne b) buffer = Some(b)
        else super.release(b)
      }
    }
  } // WriteBackOutputBufferPool

  /** An I/O buffer for the cached contents. */
  private final class Buffer extends IoBuffer {
    private[this] val data = pool allocate ()

    var readers: Int = _
    var writers: Int = _

    override def getName = data.getName
    override def getSize(tµpe: Size) = data getSize tµpe
    override def getTime(tµpe: Access) = data getTime tµpe
    override def isPermitted(tµpe: Access, entity: Entity) =
      data isPermitted (tµpe, entity)

    def load(input: AnyInputSocket) { IoSockets copy (input, data.output) }
    def save(output: AnyOutputSocket) { IoSockets copy (data.input, output) }

    override def release {
      assert(0 == writers)
      assert(0 == readers)
      data release ()
    }

    override def input: InputSocket[Buffer] = {
      final class Input extends AbstractInputSocket[Buffer] {
        private[this] val socket = data.input

        override def target() = Buffer.this

        override def stream(peer: AnyOutputSocket) =
          new CacheInputStream(socket stream peer)

        override def channel(peer: AnyOutputSocket) =
          new CacheReadOnlyChannel(socket channel peer)
      }
      new Input
    }

    override def output: OutputSocket[Buffer] = {
      final class Output extends AbstractOutputSocket[Buffer] {
        private[this] val socket = data.output

        override def target() = Buffer.this

        override def stream(peer: AnyInputSocket) =
          new CacheOutputStream(socket stream peer)

        override def channel(peer: AnyInputSocket) =
          new CacheSeekableChannel(socket channel peer)
      }
      new Output
    }

    private final class CacheInputStream(in: InputStream)
    extends DecoratingInputStream(in) with InputResource

    private final class CacheReadOnlyChannel(channel: SeekableByteChannel)
    extends ReadOnlyChannel(channel) with InputResource

    private final class CacheOutputStream(out: OutputStream)
    extends DecoratingOutputStream(out) with OutputResource

    private final class CacheSeekableChannel(channel: SeekableByteChannel)
    extends DecoratingSeekableChannel(channel) with OutputResource

    private trait InputResource extends Resource {
      abstract override def close() = close(inputBufferPool)
    }

    private trait OutputResource extends Resource {
      abstract override def close() = close(outputBufferPool)
    }

    private trait Resource extends Closeable {
      private[this] var closed: Boolean = _

      protected final def close(pool: Pool[Buffer, IOException]) {
        // HC SUNT DRACONES!
        if (closed) return
        super.close()
        pool release Buffer.this
        closed = true
      }

      abstract override def close()
    }
  }
}

private object CacheEntry {
  /** Defines different cache entry strategies. */
  sealed trait Strategy {
    final def newCacheEntry(pool: IoBufferPool) = new CacheEntry(this, pool)

    private[CacheEntry] def newOutputBufferPool(cache: CacheEntry)
    : CacheEntry#OutputBufferPool
  }

  /** Provided different cache entry strategies. */
  object Strategy {
    /** A write-through cache flushes any written data as soon as the
      * output stream created by `CacheEntry.output` gets closed.
      */
    object WriteThrough extends Strategy {
      private[CacheEntry] override def newOutputBufferPool(cache: CacheEntry)
      : CacheEntry#OutputBufferPool =
        new cache.WriteThroughOutputBufferPool
    }

    /** A write-back cache flushes any written data if and only if it gets
      * explicitly `CacheEntry.flushed`.
      */
    object WriteBack extends Strategy {
      private[CacheEntry] override def newOutputBufferPool(cache: CacheEntry)
      : CacheEntry#OutputBufferPool =
        new cache.WriteBackOutputBufferPool
    }
  }

  /** Used to proxy the backing store entries. */
  private final class ProxyEntry(entry: Entry)
  extends DecoratingEntry[Entry](entry)
}
