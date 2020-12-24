/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.io._
import java.nio._

import net.java.truecommons.cio.Entry.Access._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.{Entry, _}
import net.java.truevfs.kernel.impl.CacheEntry.Strategy._
import net.java.truevfs.kernel.impl.CacheEntryTest._
import net.java.truevfs.kernel.impl.cio.EntryAspect._
import org.scalatest.Matchers._
import org.scalatest._
import org.scalatest.prop.TableDrivenPropertyChecks._

/** @author Christian Schlichtherle */
class CacheEntryTest extends WordSpec {

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
        back.setBuffer(mockEntryDataRead)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        pool should have size 0
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0

        cache.dataSize shouldBe UNKNOWN

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.getBuffer shouldBe null
        intercept[IOException] {
          IoSockets copy (cache.input, front.output)
        }
        pool should have size 0
        front.getBuffer shouldBe null
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        cache .configure(back.input)
              .configure(back.output)
        pool should have size 0
        front.getBuffer shouldBe null
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        pool should have size 0
        front.getBuffer shouldBe null
        IoSockets copy (cache.input, front.output)
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataRead
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.setBuffer(mockEntryDataWrite)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        intercept[IOException] {
          IoSockets copy (front.input, cache.output)
          if (WriteThrough ne strategy) {
            back getCount WRITE shouldBe 0
            cache.flush()
          }
        }
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache .configure(back.input)
              .configure(back.output)
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        IoSockets copy (front.input, cache.output)
        if (WriteThrough ne strategy) {
          back getCount WRITE shouldBe 0
          cache.flush()
        }
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataWrite
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 1
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        back = new MemoryBuffer(mockEntryName, initialCapacity)
        back.setBuffer(mockEntryDataRead)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        IoSockets copy (cache.input, front.output)
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache.release()
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        intercept[IOException] {
          IoSockets.copy(cache.input, front.output)
        }
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe null
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        cache .configure(back.input)
              .configure(back.output)
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe null
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 0
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe UNKNOWN

        IoSockets copy (cache.input, front.output)
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataRead
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front setBuffer mockEntryDataWrite
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataRead.limit()

        intercept[IOException] {
          IoSockets copy (front.input, cache.output)
          if (WriteThrough ne strategy) {
            back getCount WRITE shouldBe 0
            cache.flush()
          }
        }
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache .configure(back.input)
              .configure(back.output)
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataRead
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 0
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        IoSockets copy (front.input, cache.output)
        if (WriteThrough ne strategy) {
          back getCount WRITE shouldBe 0
          cache.flush()
        }
        cache.dataSize should not be UNKNOWN
        pool should have size 1
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataWrite
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 1
        cache.dataSize shouldBe mockEntryDataWrite.limit()

        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
              .release()
        cache.dataSize shouldBe UNKNOWN
        pool should have size 0
        front.getBuffer shouldBe mockEntryDataWrite
        back.getBuffer shouldBe mockEntryDataWrite
        back getCount READ shouldBe 1
        back getCount WRITE shouldBe 1
        cache.dataSize shouldBe UNKNOWN
      }
    }
  }
}

/** @author Christian Schlichtherle */
private object CacheEntryTest {

  class BrokenInputSocket(override val target: Entry) extends AbstractInputSocket[Entry] {

    require(null ne target)

    override def stream(peer: OutputSocket[_ <: Entry]): InputStream = new InputStream {

      override def read: Int = throw new IOException
    }
  }

  class BrokenOutputSocket(override val target: Entry) extends AbstractOutputSocket[Entry] {

    require(null ne target)

    override def stream(peer: InputSocket[_ <: Entry]): OutputStream = new OutputStream {

      override def write(b: Int): Unit = { throw new IOException }
    }
  }
}
