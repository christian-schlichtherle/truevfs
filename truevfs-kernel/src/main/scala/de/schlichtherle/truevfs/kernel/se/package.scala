/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.util._
import java.{lang => jl}
import java.{util => ju}

/**
 * Implements the Kernel API.
 * 
 * @author Christian Schlichtherle
 */
package object se {
  private[se] type AccessOptions = BitField[FsAccessOption]
  private[se] type SyncOptions = BitField[FsSyncOption]
  private[se] type AnyArchiveDriver = FsArchiveDriver[_ <: FsArchiveEntry]
  private[se] type AnyInputSocket = InputSocket[_ <: Entry]
  private[se] type AnyOutputSocket = OutputSocket[_ <: Entry]
  private[se] type AnyIoPool = IoPool[_ <: IoBuffer[_]]
  private[se] type AnyIoBuffer = IoBuffer[_ <: IoBuffer[_]]

  // Used for looping through BitField, Container etc.
  private[se] implicit def asScalaIterable[E](i: jl.Iterable[E]): Iterable[E] = {
    collection.JavaConversions.asIterable(i)
  }

  private[se] implicit def asScalaMapFromAccessToLong(input: ju.Map[Access, jl.Long]): Map[Access, Long] = {
    var output = Map[Access, Long]()
    for (e <- input.entrySet)
      output += e.getKey -> Long.unbox(e.getValue)
    output
  }

  private[se] implicit def asJavaMapFromAccessToLong(input: Map[Access, Long]): ju.Map[Access, jl.Long] = {
    var output = new ju.HashMap[Access, jl.Long]()
    for (e <- input.entrySet)
      output.put(e.getKey, Long.box(e.getValue))
    output
  }

  private[se] def asFsController[M <: FsModel](
    controller: Controller[M],
    parent: FsController[_ <: FsModel]) =
    new ControllerAdapter(controller, parent)
}
