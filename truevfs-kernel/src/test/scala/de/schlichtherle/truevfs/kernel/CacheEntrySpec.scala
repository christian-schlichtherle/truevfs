/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import java.io._
import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry.Size._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio._
import org.junit.runner._
import org.scalatest.junit._
import org.scalatest.matchers._
import org.scalatest.prop._
import org.scalatest._
import CacheEntry.Strategy._

/**
 * @author Christian Schlichtherle
 */
@RunWith(classOf[JUnitRunner])
class CacheEntrySpec extends WordSpec with ShouldMatchers with PropertyChecks {
  import CacheEntryTest._

  private val initialCapacity = 32
  private val mockEntryName = "mock"
  private val mockEntryDataRead = "read"
  private val mockEntryDataWrite = "write"

  "A cache entry" should {
    "behave correctly" in {
      pending // FIXME: The following test code causes the Scala compiler to crash (2.9.2)!
      /*
      val pool = new ByteArrayIoPool(5)
      forAll(Table("strategy", WriteThrough, WriteBack)) { strategy =>
        val cache: CacheEntry = strategy.newCacheEntry(pool)
        var front: ByteArrayIoBuffer = null
        var back: ByteArrayIoBuffer = null

        back = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        back.setData(mockEntryDataRead.getBytes)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        pool should have size (0)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)

        cache.dataSize should be (UNKNOWN)

        front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        front.getData should be (null)
        intercept[IOException] {
          IoSockets.copy(cache.input, front.output)
        }
        pool should have size (0)
        front.getData should be (null)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (UNKNOWN)

        cache .configure(back.input)
              .configure(back.output)
        pool should have size (0)
        front.getData should be (null)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (UNKNOWN)

        front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        pool should have size (0)
        front.getData should be (null)
        IoSockets.copy(cache.input, front.output)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataRead)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataRead.length)

        front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        front.setData(mockEntryDataWrite.getBytes)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataRead.length)

        intercept[IOException] {
          IoSockets.copy(front.input, cache.output)
          if (WriteThrough ne strategy) {
            back.getCount(WRITE) should be (0)
            cache.flush()
          }
        }
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataWrite.length)

        cache .configure(back.input)
              .configure(back.output)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataWrite.length)

        IoSockets.copy(front.input, cache.output)
        if (WriteThrough ne strategy) {
          back.getCount(WRITE) should be (0)
          cache.flush()
        }
        cache.dataSize should not be UNKNOWN
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataWrite)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (1)
        cache.dataSize should be (mockEntryDataWrite.length)

        back = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        back.setData(mockEntryDataRead.getBytes)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be UNKNOWN
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataWrite.length)

        front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        IoSockets.copy(cache.input, front.output)
        cache.dataSize should not be UNKNOWN
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataWrite.length)

        cache.release()
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (UNKNOWN)

        front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        intercept[IOException] {
          IoSockets.copy(cache.input, front.output)
        }
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        front.getData should be (null)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (UNKNOWN)

        cache .configure(back.input)
              .configure(back.output)
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        front.getData should be (null)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (0)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (UNKNOWN)

        IoSockets.copy(cache.input, front.output)
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataRead)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataRead.length)

        front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
        front.setData(mockEntryDataWrite.getBytes)
        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataRead.length)

        intercept[IOException] {
          IoSockets.copy(front.input, cache.output)
          if (WriteThrough ne strategy) {
            back.getCount(WRITE) should be (0)
            cache.flush()
          }
        }
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataWrite.length)

        cache .configure(back.input)
              .configure(back.output)
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataRead)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (0)
        cache.dataSize should be (mockEntryDataWrite.length)

        IoSockets.copy(front.input, cache.output)
        if (WriteThrough ne strategy) {
          back.getCount(WRITE) should be (0)
          cache.flush()
        }
        cache.dataSize should not be (UNKNOWN)
        pool should have size (1)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataWrite)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (1)
        cache.dataSize should be (mockEntryDataWrite.length)

        cache .configure(new BrokenInputSocket(back))
              .configure(new BrokenOutputSocket(back))
              .release()
        cache.dataSize should be (UNKNOWN)
        pool should have size (0)
        new String(front.getData) should be (mockEntryDataWrite)
        new String(back.getData) should be (mockEntryDataWrite)
        back.getCount(READ) should be (1)
        back.getCount(WRITE) should be (1)
        cache.dataSize should be (UNKNOWN)
      }
      */
    }
  }
}

private object CacheEntryTest {
  class BrokenInputSocket(override val localTarget: Entry)
  extends AbstractInputSocket[Entry] {
    require(null ne localTarget)

    override def stream = new InputStream {
      override def read = throw new IOException
    }
  }

  class BrokenOutputSocket(override val localTarget: Entry)
  extends AbstractOutputSocket[Entry] {
    require(null ne localTarget)

    override def stream = new OutputStream {
      override def write(b: Int) { throw new IOException }
    }
  }
}
