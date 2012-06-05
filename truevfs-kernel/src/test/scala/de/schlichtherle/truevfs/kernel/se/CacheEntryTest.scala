/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import de.schlichtherle.truevfs.kernel.se.CacheEntry.Strategy._
import java.io._
import net.truevfs.kernel.cio.Entry.Access._
import net.truevfs.kernel.cio.Entry.Size._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.cio._
import org.junit._
import org.scalatest.junit._

/**
 * @author Christian Schlichtherle
 */
class CacheEntryTest extends ShouldMatchersForJUnit {
  import CacheEntryTest._

  private final val initialCapacity = 32
  private final val mockEntryName = "mock"
  private final val mockEntryDataRead = "read"
  private final val mockEntryDataWrite = "write"
  private val pool = new ByteArrayIoPool(5)

  @Test
  def testCaching() {
    for (strategy <- Array[CacheEntry.Strategy](WRITE_THROUGH, WRITE_BACK)) {
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
      cache.getSize(DATA) should be (UNKNOWN)
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
      cache.getSize(DATA) should be (UNKNOWN)
      cache .configure(back.input)
            .configure(back.output)
      pool should have size (0)
      front.getData should be (null)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (0)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (UNKNOWN)
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      pool should have size (0)
      front.getData should be (null)
      IoSockets.copy(cache.input, front.output)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataRead)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataRead.length)
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      front.setData(mockEntryDataWrite.getBytes)
      cache .configure(new BrokenInputSocket(back))
            .configure(new BrokenOutputSocket(back))
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataRead.length)
      intercept[IOException] {
        IoSockets.copy(front.input, cache.output)
        if (WRITE_THROUGH ne strategy) {
          back.getCount(WRITE) should be (0)
          cache.flush()
        }
      }
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      cache.configure(back.input).configure(back.output)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      IoSockets.copy(front.input, cache.output)
      if (WRITE_THROUGH ne strategy) {
        back.getCount(WRITE) should be (0)
        cache.flush()
      }
      cache.getSize(DATA) should not be UNKNOWN
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataWrite)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (1)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      back = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      back.setData(mockEntryDataRead.getBytes)
      cache .configure(new BrokenInputSocket(back))
            .configure(new BrokenOutputSocket(back))
      cache.getSize(DATA) should not be UNKNOWN
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (0)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      IoSockets.copy(cache.input, front.output)
      cache.getSize(DATA) should not be UNKNOWN
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (0)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      cache.release()
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (0)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (UNKNOWN)
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      intercept[IOException] {
        IoSockets.copy(cache.input, front.output)
      }
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      front.getData should be (null)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (0)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (UNKNOWN)
      cache.configure(back.input).configure(back.output)
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      front.getData should be (null)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (0)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (UNKNOWN)
      IoSockets.copy(cache.input, front.output)
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataRead)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataRead.length)
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      front.setData(mockEntryDataWrite.getBytes)
      cache .configure(new BrokenInputSocket(back))
            .configure(new BrokenOutputSocket(back))
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataRead.length)
      intercept[IOException] {
        IoSockets.copy(front.input, cache.output)
        if (WRITE_THROUGH ne strategy) {
          back.getCount(WRITE) should be (0)
          cache.flush()
        }
      }
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      cache.configure(back.input).configure(back.output)
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataRead)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (0)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      IoSockets.copy(front.input, cache.output)
      if (WRITE_THROUGH ne strategy) {
        back.getCount(WRITE) should be (0)
        cache.flush()
      }
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataWrite)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (1)
      cache.getSize(DATA) should be (mockEntryDataWrite.length)
      cache .configure(new BrokenInputSocket(back))
            .configure(new BrokenOutputSocket(back))
            .release()
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      new String(front.getData) should be (mockEntryDataWrite)
      new String(back.getData) should be (mockEntryDataWrite)
      back.getCount(READ) should be (1)
      back.getCount(WRITE) should be (1)
      cache.getSize(DATA) should be (UNKNOWN)
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
