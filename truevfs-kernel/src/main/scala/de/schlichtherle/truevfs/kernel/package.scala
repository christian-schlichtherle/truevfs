/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs

import net.truevfs.kernel._
import net.truevfs.kernel.cio._
import net.truevfs.kernel.cio.Entry._
import net.truevfs.kernel.util._
import java.{lang => jl}
import java.{util => ju}

/** Implements the Kernel API.
  * 
  * @author Christian Schlichtherle
  */
package object kernel {
  private[kernel] type AccessOptions = BitField[FsAccessOption]
  private[kernel] type SyncOptions = BitField[FsSyncOption]
  private[kernel] type AnyArchiveDriver = FsArchiveDriver[_ <: FsArchiveEntry]
  private[kernel] type AnyInputSocket = InputSocket[_ <: Entry]
  private[kernel] type AnyOutputSocket = OutputSocket[_ <: Entry]
  private[kernel] type AnyIoPool = IoPool[_ <: IoBuffer[_]]
  private[kernel] type AnyIoBuffer = IoBuffer[_ <: IoBuffer[_]]

  // Used for looping through BitField, Container etc.
  implicit private[kernel] def asScalaIterable[E](i: jl.Iterable[E]): Iterable[E] = {
    collection.JavaConversions.iterableAsScalaIterable(i)
  }

  implicit private[kernel] def asScalaMapFromAccessToLong(input: ju.Map[Access, jl.Long]): Map[Access, Long] = {
    var output = Map[Access, Long]()
    for (entry <- input.entrySet)
      output += entry.getKey -> Long.unbox(entry.getValue)
    output
  }

  implicit private[kernel] def asJavaMapFromAccessToLong(input: Map[Access, Long]): ju.Map[Access, jl.Long] = {
    var output = new ju.HashMap[Access, jl.Long]()
    for ((key, value) <- input)
      output.put(key, Long.box(value))
    output
  }

  private[kernel] def asFsController[M <: FsModel](
    controller: Controller[M],
    parent: FsController[_ <: FsModel]) =
    new ControllerAdapter(controller, parent)
}
