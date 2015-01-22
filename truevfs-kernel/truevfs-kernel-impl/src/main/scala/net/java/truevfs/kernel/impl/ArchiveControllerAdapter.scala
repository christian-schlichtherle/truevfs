/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.{lang => jl}
import java.{util => ju}
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._
import scala.Option

private final class ArchiveControllerAdapter(
  override val getParent: FsController,
  c: ArchiveController[_]
) extends FsAbstractController(c.model) with Immutable {

  override def node(options: AccessOptions, name: FsNodeName) =
    (c node (options, name)).orNull
  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    c checkAccess (options, name, types)
  override def setReadOnly(options: AccessOptions, name: FsNodeName) =
    c setReadOnly (options, name)
  override def setTime(options: AccessOptions, name: FsNodeName, times: ju.Map[Access, jl.Long]) =
    c setTime (options, name, times)
  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    c setTime (options, name, types, value)
  def input(options: AccessOptions, name: FsNodeName) = c input (options, name)
  def output(options: AccessOptions, name: FsNodeName, template: Entry) =
    c output (options, name, Option(template))
  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Entry) =
    c make (options, name, tµpe, Option(template))
  override def unlink(options: AccessOptions, name: FsNodeName) =
    c unlink (options, name)
  override def sync(options: SyncOptions) = c sync(options)
}
