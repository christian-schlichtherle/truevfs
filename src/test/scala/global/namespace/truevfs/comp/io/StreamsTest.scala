/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.comp.io

import global.namespace.truevfs.comp.io.Streams._
import global.namespace.truevfs.comp.io.StreamsTest._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock

import java.io._
import scala.util._

/** @author Christian Schlichtherle */
class StreamsTest extends AnyWordSpec {

  "Streams.cat(InputStream, OutputStream)" should {
    "fail with a NullPointerException " when givenA {
      "null InputStream" in {
        intercept[NullPointerException] {
          cat(null, out)
        }
      }

      "null OutputStream)" in {
        intercept[NullPointerException] {
          cat(in, null)
        }
      }
    }

    "call only InputStream.read(byte[], int,  int)" in {
      val in = mock[InputStream]
      when(in.read(any, any, any)).thenReturn(-1)
      cat(in, out)
      verify(in).read(any, any, any)
      verifyNoMoreInteractions(in)
    }

    "call OutputStream.write(byte[], int,  int) at least once and OutputStream.flush() exactly once and nothing else" in {
      val out = mock[OutputStream]
      cat(in, out)
      verify(out, atLeastOnce).write(any, any, any)
      verify(out).flush()
      verifyNoMoreInteractions(out)
    }

    "fail with the original IOException from InputStream.read(byte[], int, int)" in {
      val in = mock[InputStream]
      val e = new IOException
      doThrow(e).when(in).read(any, any, any)
      intercept[IOException](cat(in, out)) should be theSameInstanceAs e
    }

    "fail with the original IOException from OutputStream.write(byte[], int, int)" in {
      val out = mock[OutputStream]
      val e = new IOException
      doThrow(e).when(out).write(any, any, any)
      intercept[IOException](cat(in, out)) should be theSameInstanceAs e
    }

    "produce a copy of the data" when {
      "returning" in {
        val in = StreamsTest.in
        val out = StreamsTest.out
        for (_ <- 0 to 1) {
          cat(in, out)
          in.available shouldBe 0
          in.bytes shouldBe out.toByteArray
        }
      }
    }
  }

  "Streams.copy(InputStream, OutputStream)" should {
    "fail with a NullPointerException" when givenA {
      "null InputStream" in {
        intercept[NullPointerException] {
          copy(null.asInstanceOf[InputStream], new ByteArrayOutputStream)
        }
      }

      "null OutputStream" in {
        intercept[NullPointerException] {
          copy(new ByteArrayInputStream(Array[Byte]()), null.asInstanceOf[OutputStream])
        }
      }
    }

    "call InputStream.close()" when {
      "returning" in {
        val in = mock[InputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        copy(in, out)
        verify(in, times(2)).close()
      }

      "throwing an IOException from InputStream.read(byte[], int, int)" in {
        val in = mock[InputStream]
        doThrow(classOf[IOException]).when(in).read(any, any, any)
        intercept[IOException](copy(in, out))
        verify(in, times(2)).close()
      }

      "throwing an IOException from InputStream.close()" in {
        val in = mock[InputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        doThrow(classOf[IOException]).when(in).close()
        intercept[IOException](copy(in, out))
        verify(in, times(2)).close()
      }

      "throwing an IOException from OutputStream.write(byte[], int, int)" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        when(in.read(any, any, any)).thenReturn(1)
        doThrow(classOf[IOException]).when(out).write(any, any, any)
        intercept[IOException](copy(in, out))
        verify(in, times(2)).close()
        verify(out).close()
      }

      "throwing an IOException from OutputStream.close()" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        doThrow(classOf[IOException]).when(out).close()
        intercept[IOException](copy(in, out))
        verify(in, times(2)).close()
        verify(out).close()
      }
    }

    "call OutputStream.close()" when {
      "returning" in {
        val out = mock[OutputStream]
        copy(in, out)
        verify(out).close()
      }

      "throwing an IOException from InputStream.read(byte[], int, int)" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        doThrow(classOf[IOException]).when(in).read(any, any, any)
        intercept[IOException](copy(in, out))
        verify(in, times(2)).close()
        verify(out).close()
      }

      "throwing an IOException from InputStream.close()" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        doThrow(classOf[IOException]).when(in).close()
        intercept[IOException](copy(in, out))
        verify(in, times(2)).close()
        verify(out).close()
      }

      "throwing an IOException from OutputStream.write(byte[], int, int)" in {
        val out = mock[OutputStream]
        doThrow(classOf[IOException]).when(out).write(any, any, any)
        intercept[IOException](copy(in, out))
        verify(out).close()
      }

      "throwing an IOException from OutputStream.close()" in {
        val out = mock[OutputStream]
        doThrow(classOf[IOException]).when(out).close()
        intercept[IOException](copy(in, out))
        verify(out).close()
      }
    }

    "fail with some IOException from InputStream.close()" in {
      val in = mock[InputStream]
      when(in.read(any, any, any)).thenReturn(-1)
      doThrow(classOf[IOException]).when(in).close()
      intercept[IOException](copy(in, out))
      verify(in, times(2)).close()
    }

    "fail with some IOException from OutputStream.close()" in {
      val out = mock[OutputStream]
      doThrow(classOf[IOException]).when(out).close()
      intercept[IOException](copy(in, out))
      verify(out).close()
    }

    "produce a copy of the data" when {
      "returning" in {
        val in = StreamsTest.in
        val out = StreamsTest.out
        copy(in, out)
        in.available should be(0)
        in.bytes should equal(out.toByteArray)
      }
    }
  }

  "Streams.copy(InputStrema, OutputStream)" should {
    "fail with a NullPointerException" when givenA {
      "null InputStream" in {
        intercept[NullPointerException] {
          copy(null.asInstanceOf[InputStream], new ByteArrayOutputStream)
        }
      }

      "null OutputStream" in {
        intercept[NullPointerException] {
          copy(new ByteArrayInputStream(Array[Byte]()), null.asInstanceOf[OutputStream])
        }
      }
    }

    "call InputStream.close()" when {
      "returning" in {
        val in = mock[InputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        copy(source(in), sink(out))
        verify(in, times(2)).close()
      }

      "throwing an IOException from InputStream.read(byte[], int, int)" in {
        val in = mock[InputStream]
        doThrow(classOf[IOException]).when(in).read(any, any, any)
        intercept[IOException](copy(source(in), sink(out)))
        verify(in, times(2)).close()
      }

      "throwing an IOException from InputStream.close()" in {
        val in = mock[InputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        doThrow(classOf[IOException]).when(in).close()
        intercept[IOException](copy(source(in), sink(out)))
        verify(in, times(2)).close()
      }

      "throwing an IOException from OutputStream.write(byte[], int, int)" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        when(in.read(any, any, any)).thenReturn(1)
        doThrow(classOf[IOException]).when(out).write(any, any, any)
        intercept[IOException](copy(source(in), sink(out)))
        verify(in, times(2)).close()
      }

      "throwing an IOException from OutputStream.close()" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        doThrow(classOf[IOException]).when(out).close()
        intercept[IOException](copy(source(in), sink(out)))
        verify(in, times(2)).close()
        verify(out).close()
      }
    }

    "call OutputStream.close()" when {
      "returning" in {
        val out = mock[OutputStream]
        copy(source(in), sink(out))
        verify(out).close()
      }

      "throwing an IOException from InputStream.read(byte[], int, int)" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        doThrow(classOf[IOException]).when(in).read(any, any, any)
        intercept[IOException](copy(source(in), sink(out)))
        verify(in, times(2)).close()
        verify(out).close()
      }

      "throwing an IOException from InputStream.close()" in {
        val in = mock[InputStream]
        val out = mock[OutputStream]
        when(in.read(any, any, any)).thenReturn(-1)
        doThrow(classOf[IOException]).when(in).close()
        intercept[IOException](copy(source(in), sink(out)))
        verify(in, times(2)).close()
        verify(out).close()
      }

      "throwing an IOException from OutputStream.write(byte[], int, int)" in {
        val out = mock[OutputStream]
        doThrow(classOf[IOException]).when(out).write(any, any, any)
        intercept[IOException](copy(source(in), sink(out)))
        verify(out).close()
      }

      "throwing an IOException from OutputStream.close()" in {
        val out = mock[OutputStream]
        doThrow(classOf[IOException]).when(out).close()
        intercept[IOException](copy(source(in), sink(out)))
        verify(out).close()
      }
    }

    "not call Sink.stream()" when {
      "throwing an IOException from Source.stream()" in {
        val source = mock[Source]
        val sink = mock[Sink]
        doThrow(classOf[IOException]).when(source).stream()
        intercept[IOException](copy(source, sink))
        verify(sink, never).stream()
      }
    }

    "call InputStream.close()" when {
      "throwing an IOException from Sink.stream()" in {
        val in = mock[InputStream]
        val sink = mock[Sink]
        doThrow(classOf[IOException]).when(sink).stream()
        intercept[IOException](copy(source(in), sink))
        verify(in).close()
      }
    }

    "fail with some IOException from Source.stream()" in {
      val source = mock[Source]
      doThrow(classOf[IOException]).when(source).stream()
      intercept[IOException](copy(source, sink(out)))
    }

    "fail with some IOException from Sink.stream()" in {
      val sink = mock[Sink]
      doThrow(classOf[IOException]).when(sink).stream()
      intercept[IOException](copy(source(in), sink))
    }

    "produce a copy of the data" when {
      "returning" in {
        val in = StreamsTest.in
        val out = StreamsTest.out
        copy(source(in), sink(out))
        in.available should be(0)
        in.bytes should equal(out.toByteArray)
      }
    }
  }

  private def givenA = afterWord("given a")
}

private object StreamsTest {

  val bufferSize: Int = 2 * Streams.FIFO_SIZE * Streams.BUFFER_SIZE

  private def source(in: InputStream) = new AbstractSource {

    private var optIn: Option[InputStream] = Some(in)

    override def stream(): InputStream = {
      optIn match {
        case Some(in2) => optIn = None; in2
        case None => throw new IllegalStateException
      }
    }
  }

  private def in = {
    val b = new Array[Byte](bufferSize)
    Random.nextBytes(b)
    new ByteArrayInputStreamWithBuffer(b)
  }

  private def sink(out: OutputStream) = new AbstractSink {

    private var optOut: Option[OutputStream] = Some(out)

    override def stream(): OutputStream = {
      optOut match {
        case Some(out2) => optOut = None; out2
        case None => throw new IllegalStateException
      }
    }
  }

  private def out = new ByteArrayOutputStream(bufferSize)

  private class ByteArrayInputStreamWithBuffer(val bytes: Array[Byte])
    extends ByteArrayInputStream(bytes)

}
