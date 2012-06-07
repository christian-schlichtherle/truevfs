/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import java.io._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.io._
import net.truevfs.kernel.util._
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
  *   [[de.schlichtherle.truevfs.kernel.se.CacheEntry.Strategy.]], entry data
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
private final class CacheEntry private (
  private[this] val strategy: Strategy,
  private[this] val pool: AnyIoPool
) extends Entry with Releasable[IOException] with Flushable with Closeable {

  private[this] var _input: Option[AnyInputSocket] = None
  private[this] var _output: Option[AnyOutputSocket] = None
  private[this] var _inputBufferPool: Option[InputBufferPool] = None
  private[this] var _outputBufferPool: Option[OutputBufferPool] = None
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
      def socket() = buffer(getInputBufferPool).input
      override def localTarget() = localTarget(_input.get)
    }
    new Input
  }

  /** Returns an output socket for writing the cached entry data.
    *
    * @return An output socket for writing the cached entry data.
    */
  def output: AnyOutputSocket = {
    final class Output extends DelegatingOutputSocket[Entry] with BufferAllocator {
      def socket() = buffer(getOutputBufferPool).output
      override def localTarget() = localTarget(_output.get)
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

    def localTarget(socket: IoSocket[_ <: Entry, _]) = {
      _buffer match {
        case Some(buffer) => buffer
        case _ => new CacheEntry.ProxyEntry(socket/*.bind(this)*/.localTarget()) // do NOT bind!
      }
    }
  }

  /** Writes the cached entry data to the backing store unless already done.
    * Whether or not this method needs to be called depends on the caching
    * strategy.
    * E.g. the caching strategy
    * [[de.schlichtherle.truevfs.kernel.se.CacheEntry.Strategy.WRITE_THROUGH]]
    * writes any changed entry data immediately, so calling this method has no
    * effect.
    */
  def flush() {
    _buffer foreach { getOutputBufferPool.release(_) }
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

  private def getInputBufferPool: InputBufferPool = {
    _inputBufferPool match {
      case Some(pool) => pool
      case _ =>
        val ibp = strategy.newInputBufferPool(this).asInstanceOf[InputBufferPool]
        _inputBufferPool = new Some(ibp)
        ibp
    }
  }

  private def getOutputBufferPool: OutputBufferPool = {
    _outputBufferPool match {
      case Some(pool) => pool
      case _ =>
        val obp = strategy.newOutputBufferPool(this).asInstanceOf[OutputBufferPool]
        _outputBufferPool = new Some(obp)
        obp
    }
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

  private[CacheEntry] final class InputBufferPool extends Pool[Buffer, IOException] {
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

  private[CacheEntry] abstract class OutputBufferPool extends Pool[Buffer, IOException] {
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

    def input = {
      final class Input extends AbstractInputSocket[Buffer] {
        private[this] val socket = data.input

        def boundSocket = socket.bind(this)

        def localTarget() = Buffer.this

        override def stream() = {
          final class Stream extends DecoratingInputStream(boundSocket.stream()) {
            private[this] var closed: Boolean = _

            override def close() {
              if (!closed) {
                // HC SUNT DRACONES!
                in.close()
                getInputBufferPool.release(Buffer.this)
                closed = true
              }
            }
          }
          new Stream
        }

        override def channel() = {
          final class Channel extends DecoratingReadOnlyChannel(boundSocket.channel()) {
            private[this] var closed: Boolean = _

            override def close() {
              if (!closed) {
                // HC SUNT DRACONES!
                channel.close()
                getInputBufferPool.release(Buffer.this)
                closed = true
              }
            }
          }
          new Channel
        }
      }
      new Input
    }

    def output = {
      final class Output extends AbstractOutputSocket[Buffer] {
        private[this] val socket = data.output

        def boundSocket = socket.bind(this)

        def localTarget() = Buffer.this

        override def stream() = {
          final class Stream extends DecoratingOutputStream(boundSocket.stream()) {
            private[this] var closed: Boolean = _

            override def close() {
              if (!closed) {
                // HC SUNT DRACONES!
                out.close()
                getOutputBufferPool.release(Buffer.this)
                closed = true
              }
            }
          }
          new Stream
        }

        override def channel() = {
          final class Channel extends DecoratingSeekableChannel(boundSocket.channel()) {
            private[this] var closed: Boolean = _

            override def close() {
              if (!closed) {
                // HC SUNT DRACONES!
                channel.close()
                getOutputBufferPool.release(Buffer.this)
                closed = true
              }
            }
          }
          new Channel
        }
      }
      new Output
    }
  }
}

private object CacheEntry {
  /** Defines different cache entry strategies. */
  sealed trait Strategy {
    private[se] final def newCacheEntry(pool: AnyIoPool) =
      new CacheEntry(this, pool)

    private[CacheEntry] final def newInputBufferPool(cache: CacheEntry): CacheEntry#InputBufferPool =
      new cache.InputBufferPool

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
