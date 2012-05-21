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
  type AccessOptions = BitField[FsAccessOption]
  type SyncOptions = BitField[FsSyncOption]
  type AnyArchiveDriver = FsArchiveDriver[_ <: FsArchiveEntry]
  type AnyController = FsController[_ <: FsModel]
  type AnyInputSocket = InputSocket[_ <: Entry]
  type AnyOutputSocket = OutputSocket[_ <: Entry]
  type AnyIoPool = IOPool[_ <: IOBuffer[_]]
  type AnyIoBuffer = IOBuffer[_ <: IOBuffer[_]]

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
}
