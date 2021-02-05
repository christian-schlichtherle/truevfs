/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.kernel.impl

import global.namespace.truevfs.commons.cio.Entry.Access._
import global.namespace.truevfs.commons.cio.Entry._
import global.namespace.truevfs.commons.cio.{Entry, InputSocket, _}
import global.namespace.truevfs.it.cio.EntryAspect.apply
import global.namespace.truevfs.it.kernel.impl.CacheEntrySpec.{BrokenInputSocket, BrokenOutputSocket}
import global.namespace.truevfs.kernel.impl.CacheEntry.Strategy._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.wordspec.AnyWordSpec

import java.io._
import java.nio._
import java.util.Optional

/** @author Christian Schlichtherle */
class CacheEntrySpec extends AnyWordSpec {

  private val initialCapacity = 32
  private val mockEntryName = "mock"

  private def mockEntryDataRead = ByteBuffer.wrap("read".getBytes).asReadOnlyBuffer

  private def mockEntryDataWrite = ByteBuffer.wrap("write".getBytes).asReadOnlyBuffer

  "A cache entry" should {
    "behave correctly" in {
      val pool = new MemoryBufferPool(5)
      forAll(Table("strategy", WriteThrough, WriteBack)) { strategy =>
        val cache = strategy.newCacheEntry(pool)
        var front: MemoryBuffer = null
        var back: MemoryBuffer = null

        back = new MemoryBuffer(mockEntryName, initialCapacity)
        back.setBuffer(Optional.of(mockEntryDataRead))
        cache.configure(new BrokenInputSocket(back)).configure(new BrokenOutputSocket(back))
        pool should have size 0
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0

        cache.dataSize shouldBe UNKNOWN

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.getBuffer shouldBe Optional.empty
        intercept[IOException] {
          IoSockets copy(cache.input, front.output)
        }
        pool should have size 0
        front.getBuffer shouldBe Optional.empty
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        cache.configure(back.input).configure(back.output)
        pool should have size 0
        front.getBuffer shouldBe Optional.empty
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        pool should have size 0
        front.getBuffer shouldBe Optional.empty
        IoSockets copy(cache.input, front.output)
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.setBuffer(Optional.of(mockEntryDataWrite))
        cache.configure(new BrokenInputSocket(back))
          .configure(new BrokenOutputSocket(back))
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        intercept[IOException] {
          IoSockets.copy(front.input, cache.output)
          if (WriteThrough ne strategy) {
            back getCount WRITE shouldBe 0
            cache.flush()
          }
        }
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache.configure(back.input).configure(back.output)
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        IoSockets.copy(front.input, cache.output)
        if (WriteThrough ne strategy) {
          back getCount WRITE shouldBe 0
          cache.flush()
        }
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 1
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        back = new MemoryBuffer(mockEntryName, initialCapacity)
        back.setBuffer(Optional.of(mockEntryDataRead))
        cache.configure(new BrokenInputSocket(back))
          .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        IoSockets copy(cache.input, front.output)
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache.release()
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        intercept[IOException] {
          IoSockets.copy(cache.input, front.output)
        }
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe Optional.empty
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        cache.configure(back.input).configure(back.output)
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe Optional.empty
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        IoSockets copy(cache.input, front.output)
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.setBuffer(Optional.of(mockEntryDataWrite))
        cache.configure(new BrokenInputSocket(back))
          .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        intercept[IOException] {
          IoSockets.copy(front.input, cache.output)
          if (WriteThrough ne strategy) {
            back getCount WRITE shouldBe 0
            cache.flush()
          }
        }
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache.configure(back.input)
          .configure(back.output)
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataRead)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        IoSockets.copy(front.input, cache.output)
        if (WriteThrough ne strategy) {
          back getCount WRITE shouldBe 0
          cache.flush()
        }
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 1
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache.configure(new BrokenInputSocket(back))
          .configure(new BrokenOutputSocket(back))
          .release()
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back.getBuffer shouldBe Optional.of(mockEntryDataWrite)
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 1
        cache.dataSize shouldBe UNKNOWN
      }
    }
  }
}

/** @author Christian Schlichtherle */
private object CacheEntrySpec {

  class BrokenInputSocket(override val getTarget: Entry) extends InputSocket[Entry] {

    require(null ne getTarget)

    override def stream(peer: Optional[_ <: OutputSocket[_ <: Entry]]): InputStream = new InputStream {

      override def read: Int = throw new IOException
    }
  }

  class BrokenOutputSocket(override val getTarget: Entry) extends OutputSocket[Entry] {

    require(null ne getTarget)

    override def stream(peer: Optional[_ <: InputSocket[_ <: Entry]]): OutputStream = new OutputStream {

      override def write(b: Int): Unit = {
        throw new IOException
      }
    }
  }

}
