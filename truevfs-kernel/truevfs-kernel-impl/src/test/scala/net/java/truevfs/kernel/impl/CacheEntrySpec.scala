/*
 * Copyright (C) 2005-2013 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.io._
import java.nio._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio.Entry.Access._
import net.java.truecommons.cio.Entry.Size._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.impl.cio.EntryAspect._
import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._
import CacheEntry.Strategy._

/** @author Christian Schlichtherle */
private object CacheEntrySpec {
  class BrokenInputSocket(override val target: Entry)
  extends AbstractInputSocket[Entry] {
    require(null ne target)

    override def stream(peer: AnyOutputSocket) = new InputStream {
      override def read = throw new IOException
    }
  }

  class BrokenOutputSocket(override val target: Entry)
  extends AbstractOutputSocket[Entry] {
    require(null ne target)

    override def stream(peer: AnyInputSocket) = new OutputStream {
      override def write(b: Int) { throw new IOException }
    }
  }
}

/** @author Christian Schlichtherle */
@RunWith(classOf[JUnitRunner])
class CacheEntrySpec extends WordSpec with ShouldMatchers with PropertyChecks {
  import CacheEntrySpec._

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
        pool should have size (0)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)

        cache.dataSize should be (UNKNOWN)

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.getBuffer should be (null)
        intercept[IOException] {
          IoSockets copy (cache.input, front.output)
        }
        pool should have size (0)
        front.getBuffer should be (null)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (UNKNOWN)

        cache .configure(back.input)
              .configure(back.output)
        pool should have size (0)
        front.getBuffer should be (null)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (UNKNOWN)

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        pool should have size (0)
        front.getBuffer should be (null)
        IoSockets copy (cache.input, front.output)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataRead)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataRead.limit)

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front.setBuffer(mockEntryDataWrite)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataRead.limit)

        intercept[IOException] {
          IoSockets copy (front.input, cache.output)
          if (WriteThrough ne strategy) {
            back getCount WRITE should be (0)
            cache flush ()
          }
        }
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataWrite.limit)

        cache .configure(back.input)
              .configure(back.output)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataWrite.limit)

        IoSockets copy (front.input, cache.output)
        if (WriteThrough ne strategy) {
          back getCount WRITE should be (0)
          cache flush ()
        }
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataWrite)
        back getCount READ should be (1)
        back getCount WRITE should be (1)
        cache.dataSize should be (mockEntryDataWrite.limit)

        back = new MemoryBuffer(mockEntryName, initialCapacity)
        back.setBuffer(mockEntryDataRead)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataWrite.limit)

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        IoSockets copy (cache.input, front.output)
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataWrite.limit)

        cache release ()
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (UNKNOWN)

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        intercept[IOException] {
          IoSockets copy (cache.input, front.output)
        }
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        front.getBuffer should be (null)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (UNKNOWN)

        cache .configure(back.input)
              .configure(back.output)
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        front.getBuffer should be (null)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (0)
        back getCount WRITE should be (0)
        cache.dataSize should be (UNKNOWN)

        IoSockets copy (cache.input, front.output)
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataRead)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataRead.limit)

        front = new MemoryBuffer(mockEntryName, initialCapacity)
        front setBuffer mockEntryDataWrite
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataRead.limit)

        intercept[IOException] {
          IoSockets copy (front.input, cache.output)
          if (WriteThrough ne strategy) {
            back getCount WRITE should be (0)
            cache flush ()
          }
        }
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataWrite.limit)

        cache .configure(back.input)
              .configure(back.output)
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataRead)
        back getCount READ should be (1)
        back getCount WRITE should be (0)
        cache.dataSize should be (mockEntryDataWrite.limit)

        IoSockets copy (front.input, cache.output)
        if (WriteThrough ne strategy) {
          back getCount WRITE should be (0)
          cache flush ()
        }
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataWrite)
        back getCount READ should be (1)
        back getCount WRITE should be (1)
        cache.dataSize should be (mockEntryDataWrite.limit)

        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
              .release()
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        front.getBuffer should equal (mockEntryDataWrite)
        back.getBuffer should equal (mockEntryDataWrite)
        back getCount READ should be (1)
        back getCount WRITE should be (1)
        cache.dataSize should be (UNKNOWN)
      }
    }
  }
}
