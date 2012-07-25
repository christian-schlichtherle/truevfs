/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel.impl

import de.schlichtherle.truecommons.io._
import edu.umd.cs.findbugs.annotations._
import java.io._
import java.nio.channels._
import javax.annotation.concurrent._
import net.truevfs.kernel.spec.cio.Entry._
import net.truevfs.kernel.spec.cio._
import net.truevfs.kernel.spec.util._
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
  *   [[net.truevfs.kernel.impl.CacheEntry.Strategy.]], entry data
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
  private[this] val pool: AnyIoBufferPool
) extends Entry with Releasable[IOException] with Flushable with Closeable {

  private[this] var _input: Option[AnyInputSocket] = None
  private[this] var _output: Option[AnyOutputSocket] = None
  private[this] val inputBufferPool = new InputBufferPool
  private[this] val outputBufferPool =
    strategy.newOutputBufferPool(this).asInstanceOf[OutputBufferPool]
  private[this] var _buffer: Option[Buffer] = None

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
    _input = new Some(input)
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
    _output = new Some(output)
    this
  }

  def getName = "Johnny Cache!"

  def getSize(tµpe: Size) = {
    _buffer match {
      case Some(buffer) => buffer.getSize(tµpe)
      case _ => UNKNOWN
    }
  }

  def getTime(tµpe: Access) = {
    _buffer match {
      case Some(buffer) => buffer.getTime(tµpe)
      case _ => UNKNOWN
    }
  }

  /** Returns `null` in order to block copying of access permissions of cache
    * entries.
    */
  def isPermitted(tµpe: Access, entity: Entity) = null

  /** Returns an input socket for reading the cached entry data.
    *
    * @return An input socket for reading the cached entry data.
    */
  def input: AnyInputSocket = {
    final class Input extends DelegatingInputSocket[Entry] with BufferAllocator {
      def socket() = buffer(inputBufferPool).input
      override def target() = target(_input.get)
    }
    new Input
  }

  /** Returns an output socket for writing the cached entry data.
    *
    * @return An output socket for writing the cached entry data.
    */
  def output: AnyOutputSocket = {
    final class Output extends DelegatingOutputSocket[Entry] with BufferAllocator {
      def socket() = buffer(outputBufferPool).output
      override def target() = target(_output.get)
    }
    new Output
  }

  private trait BufferAllocator {
    private[this] var _buffer: Option[Buffer] = None

    def buffer(pool: Pool[Buffer, _]) = {
      val buffer = pool.allocate()
      _buffer = new Some(buffer)
      buffer
    }

    def target(socket: IoSocket[_ <: Entry]) = {
      _buffer match {
        case Some(buffer) => buffer
        case None => new ProxyEntry(socket.target())
      }
    }
  }

  /** Writes the cached entry data to the backing store unless already done.
    * Whether or not this method needs to be called depends on the caching
    * strategy.
    * E.g. the caching strategy
    * [[net.truevfs.kernel.impl.CacheEntry.Strategy.WRITE_THROUGH]]
    * writes any changed entry data immediately, so calling this method has no
    * effect.
    */
  def flush() {
    _buffer foreach { outputBufferPool.release(_) }
  }

  /** Clears the entry data from this cache without flushing it.
    *
    * @throws IOException on any I/O error.
    */
  def release() { setBuffer(None) }

  def close() {
    flush()
    release()
  }

  private def setBuffer(newBuffer: Option[Buffer]) {
    val oldBuffer = _buffer
    if (oldBuffer.orNull ne newBuffer.orNull) {
      _buffer = newBuffer
      oldBuffer foreach { buffer =>
        if (0 == buffer.writers && 0 == buffer.readers)
          buffer.release()
      }
    }
  }

  private final class InputBufferPool extends Pool[Buffer, IOException] {
    def allocate() = {
      if (_buffer.isEmpty) {
        val buffer = new Buffer
        try {
          buffer.load(_input.get)
        } catch {
          case ex: Throwable =>
            try {
              buffer.release()
            } catch {
              case ex2: Throwable =>
                ex.addSuppressed(ex2)
            }
            throw ex
        }
        setBuffer(new Some(buffer))
      }
      val buffer = _buffer.get
      assert(Strategy.WriteBack.eq(strategy) || 0 == buffer.writers)
      buffer.readers += 1
      buffer
    }

    def release(buffer: Buffer) {
      assert(Strategy.WriteBack.eq(strategy) || 0 == buffer.writers)
      buffer.readers -= 1
      if (0 == buffer.readers && 0 == buffer.writers
          && _buffer.orNull.ne(buffer))
        buffer.release()
    }
  } // InputBufferPool

  private[CacheEntry] abstract class OutputBufferPool
  extends Pool[Buffer, IOException] {
    def allocate() = {
      val buffer = new Buffer
      assert(0 == buffer.readers)
      buffer.writers = 1
      buffer
    }

    def release(buffer: Buffer) {
      assert(Strategy.WriteBack.eq(strategy) || 0 == buffer.readers)
      buffer.writers = 0
      try {
        buffer.save(_output.get)
      } finally {
        setBuffer(new Some(buffer))
      }
    }
  } // OutputBufferPool

  private[CacheEntry] final class WriteThroughOutputBufferPool extends OutputBufferPool {
    override def release(buffer: Buffer) {
      if (0 != buffer.writers)
        super.release(buffer)
    }
  } // WriteThroughOutputBufferPool

  private[CacheEntry] final class WriteBackOutputBufferPool extends OutputBufferPool {
    override def release(buffer: Buffer) {
      if (0 != buffer.writers)
        if (_buffer.orNull ne buffer)
          setBuffer(new Some(buffer))
        else
          super.release(buffer)
    }
  } // WriteBackOutputBufferPool

  /** An I/O buffer with the cached contents. */
  private final class Buffer extends IoBuffer[Buffer] {
    private[this] val data = pool.allocate()

    var readers: Int = _
    var writers: Int = _

    def getName = data.getName
    def getSize(tµpe: Size) = data.getSize(tµpe)
    def getTime(tµpe: Access) = data.getTime(tµpe)
    def isPermitted(tµpe: Access, entity: Entity) =
      data.isPermitted(tµpe, entity)

    def load(input: AnyInputSocket) { IoSockets.copy(input, data.output) }
    def save(output: AnyOutputSocket) { IoSockets.copy(data.input, output) }

    def release {
      assert(0 == writers)
      assert(0 == readers)
      data.release()
    }

    private trait CacheResource extends Closeable {
      private[this] var closed: Boolean = _

      protected final def close(pool: Pool[Buffer, _]) {
        if (!closed) {
          // HC SUNT DRACONES!
          super.close()
          pool.release(Buffer.this)
          closed = true
        }
      }

      abstract override def close()
    }

    private trait CacheInputResource extends CacheResource {
      abstract override def close() = close(inputBufferPool)
    }

    private trait CacheOutputResource extends CacheResource {
      abstract override def close() = close(outputBufferPool)
    }
    
    private final class CacheInputStream(in: InputStream)
    extends DecoratingInputStream(in) with CacheInputResource

    private final class CacheReadOnlyChannel(channel: SeekableByteChannel)
    extends ReadOnlyChannel(channel) with CacheInputResource

    private final class CacheOutputStream(out: OutputStream)
    extends DecoratingOutputStream(out) with CacheOutputResource

    private final class CacheSeekableChannel(channel: SeekableByteChannel)
    extends DecoratingSeekableChannel(channel) with CacheOutputResource

    def input: InputSocket[Buffer] = {
      final class Input extends AbstractInputSocket[Buffer] {
        private[this] val socket = data.input

        def target() = Buffer.this

        override def stream(peer: AnyOutputSocket) =
          new CacheInputStream(socket.stream(peer))

        override def channel(peer: AnyOutputSocket) =
          new CacheReadOnlyChannel(socket.channel(peer))
      }
      new Input
    }

    def output: OutputSocket[Buffer] = {
      final class Output extends AbstractOutputSocket[Buffer] {
        private[this] val socket = data.output

        def target() = Buffer.this

        override def stream(peer: AnyInputSocket) =
          new CacheOutputStream(socket stream peer)

        override def channel(peer: AnyInputSocket) =
          new CacheSeekableChannel(socket channel peer)
      }
      new Output
    }
  }
}

private object CacheEntry {
  /** Defines different cache entry strategies. */
  sealed trait Strategy {
    final def newCacheEntry(pool: AnyIoBufferPool) = new CacheEntry(this, pool)

    private[CacheEntry] def newOutputBufferPool(cache: CacheEntry): CacheEntry#OutputBufferPool
  }

  /** Provided different cache entry strategies. */
  object Strategy {
    /** A write-through cache flushes any written data as soon as the
      * output stream created by `CacheEntry.output` gets closed.
      */
    object WriteThrough extends Strategy {
      private[CacheEntry] def newOutputBufferPool(cache: CacheEntry): CacheEntry#OutputBufferPool =
        new cache.WriteThroughOutputBufferPool
    }

    /** A write-back cache flushes any written data if and only if it gets
      * explicitly `CacheEntry.flushed`.
      */
    object WriteBack extends Strategy {
      private[CacheEntry] def newOutputBufferPool(cache: CacheEntry): CacheEntry#OutputBufferPool =
        new cache.WriteBackOutputBufferPool
    }
  }

  /** Used to proxy the backing store entries. */
  private final class ProxyEntry(entry: Entry)
  extends DecoratingEntry[Entry](entry)
}
