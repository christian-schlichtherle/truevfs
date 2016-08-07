/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import java.{lang => jl, util => ju}

import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio._
import net.java.truecommons.shed.BitField
import net.java.truevfs.kernel.spec._

private final class ArchiveControllerAdapter(
  override val getParent: FsController,
  controller: ArchiveController[_]
) extends FsAbstractController(controller.model) with Immutable {

  override def node(options: AccessOptions, name: FsNodeName) =
    (controller node (options, name)).orNull
  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    controller checkAccess (options, name, types)
  override def setReadOnly(options: AccessOptions, name: FsNodeName) =
    controller setReadOnly (options, name)
  override def setTime(options: AccessOptions, name: FsNodeName, times: ju.Map[Access, jl.Long]) =
    controller setTime (options, name, times)
  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    controller setTime (options, name, types, value)
  def input(options: AccessOptions, name: FsNodeName) = controller input (options, name)
  def output(options: AccessOptions, name: FsNodeName, template: Entry) =
    controller output (options, name, Option(template))
  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Entry) =
    controller make (options, name, tµpe, Option(template))
  override def unlink(options: AccessOptions, name: FsNodeName) =
    controller unlink (options, name)
  override def sync(options: SyncOptions) = controller sync options
}
