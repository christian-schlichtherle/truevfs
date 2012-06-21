/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.truevfs.kernel

import net.truevfs.kernel.spec._
import net.truevfs.kernel.spec.cio._
import net.truevfs.kernel.spec.cio.Entry._
import net.truevfs.kernel.spec.util._
import java.{lang => jl}
import java.{util => ju}

/** Implements the Kernel API.
  * 
  * @author Christian Schlichtherle
  */
package object impl {
  private[impl] type AccessOptions = BitField[FsAccessOption]
  private[impl] type SyncOptions = BitField[FsSyncOption]
  private[impl] type AnyArchiveDriver = FsArchiveDriver[_ <: FsArchiveEntry]
  private[impl] type AnyInputSocket = InputSocket[_ <: Entry]
  private[impl] type AnyOutputSocket = OutputSocket[_ <: Entry]
  private[impl] type AnyIoPool = IoPool[_ <: IoBuffer[_]]
  private[impl] type AnyIoBuffer = IoBuffer[_ <: IoBuffer[_]]

  // Used for looping through BitField, Container etc.
  implicit private[impl] def asScalaIterable[E](i: jl.Iterable[E]): Iterable[E] = {
    collection.JavaConversions.iterableAsScalaIterable(i)
  }

  implicit private[impl] def asScalaMapFromAccessToLong(input: ju.Map[Access, jl.Long]): Map[Access, Long] = {
    var output = Map[Access, Long]()
    for (entry <- input.entrySet)
      output += entry.getKey -> Long.unbox(entry.getValue)
    output
  }

  implicit private[impl] def asJavaMapFromAccessToLong(input: Map[Access, Long]): ju.Map[Access, jl.Long] = {
    var output = new ju.HashMap[Access, jl.Long]()
    for ((key, value) <- input)
      output.put(key, Long.box(value))
    output
  }

  private[impl] def asFsController[M <: FsModel](
    controller: Controller[M],
    parent: FsController[_ <: FsModel]) =
    new ControllerAdapter(controller, parent)
}
