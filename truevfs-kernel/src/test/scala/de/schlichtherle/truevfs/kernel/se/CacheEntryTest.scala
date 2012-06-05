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
import org.hamcrest.CoreMatchers._
import org.junit.Assert._
import org.junit._
import org.scalatest.junit._

/**
 * @author Christian Schlichtherle
 */
class CacheEntryTest extends AssertionsForJUnit with ShouldMatchersForJUnit {
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
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(UNKNOWN.asInstanceOf[Long]))
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      front.getData should be (null)
      intercept[IOException] {
        IoSockets.copy(cache.input, front.output)
      }
      pool should have size (0)
      front.getData should be (null)
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(UNKNOWN.asInstanceOf[Long]))
      cache.configure(back.input).configure(back.output)
      pool should have size (0)
      front.getData should be (null)
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(UNKNOWN.asInstanceOf[Long]))
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      pool should have size (0)
      front.getData should be (null)
      IoSockets.copy(cache.input, front.output)
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataRead))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataRead.length.asInstanceOf[Long]))
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      front.setData(mockEntryDataWrite.getBytes)
      cache.configure(new CacheEntryTest.BrokenInputSocket(back)).configure(new CacheEntryTest.BrokenOutputSocket(back))
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataRead.length.asInstanceOf[Long]))
      intercept[IOException] {
        IoSockets.copy(front.input, cache.output)
        if (WRITE_THROUGH ne strategy) {
          assertThat(back.getCount(WRITE), is(0))
          cache.flush()
        }
      }
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      cache.configure(back.input).configure(back.output)
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      IoSockets.copy(front.input, cache.output)
      if (WRITE_THROUGH ne strategy) {
        assertThat(back.getCount(WRITE), is(0))
        cache.flush()
      }
      cache.getSize(DATA) should not be UNKNOWN
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataWrite))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(1))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      back = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      back.setData(mockEntryDataRead.getBytes)
      cache.configure(new CacheEntryTest.BrokenInputSocket(back)).configure(new CacheEntryTest.BrokenOutputSocket(back))
      cache.getSize(DATA) should not be UNKNOWN
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      IoSockets.copy(cache.input, front.output)
      cache.getSize(DATA) should not be UNKNOWN
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      cache.release()
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      cache.getSize(DATA) should be (UNKNOWN)
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      intercept[IOException] {
        IoSockets.copy(cache.input, front.output)
      }
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      front.getData should be (null)
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      cache.getSize(DATA) should be (UNKNOWN)
      cache.configure(back.input).configure(back.output)
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      front.getData should be (null)
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(0))
      assertThat(back.getCount(WRITE), is(0))
      cache.getSize(DATA) should be (UNKNOWN)
      IoSockets.copy(cache.input, front.output)
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataRead))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataRead.length.asInstanceOf[Long]))
      front = new ByteArrayIoBuffer(mockEntryName, initialCapacity)
      front.setData(mockEntryDataWrite.getBytes)
      cache.configure(new CacheEntryTest.BrokenInputSocket(back)).configure(new CacheEntryTest.BrokenOutputSocket(back))
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataRead.length.asInstanceOf[Long]))
      intercept[IOException] {
        IoSockets.copy(front.input, cache.output)
        if (WRITE_THROUGH ne strategy) {
          assertThat(back.getCount(WRITE), is(0))
          cache.flush()
        }
      }
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      cache.configure(back.input).configure(back.output)
      cache.getSize(DATA) should not be (UNKNOWN)
      assertThat(pool.size, is(1))
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataRead))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(0))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      IoSockets.copy(front.input, cache.output)
      if (WRITE_THROUGH ne strategy) {
        assertThat(back.getCount(WRITE), is(0))
        cache.flush()
      }
      cache.getSize(DATA) should not be (UNKNOWN)
      pool should have size (1)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataWrite))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(1))
      assertThat(cache.getSize(DATA), is(mockEntryDataWrite.length.asInstanceOf[Long]))
      cache .configure(new BrokenInputSocket(back))
            .configure(new BrokenOutputSocket(back))
            .release()
      cache.getSize(DATA) should be (UNKNOWN)
      pool should have size (0)
      assertThat(new String(front.getData), equalTo(mockEntryDataWrite))
      assertThat(new String(back.getData), equalTo(mockEntryDataWrite))
      assertThat(back.getCount(READ), is(1))
      assertThat(back.getCount(WRITE), is(1))
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
