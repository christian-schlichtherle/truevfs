/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import java.{lang => jl}
import java.{util => ju}
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._

private final class ControllerAdapter[M <: FsModel](
  c: Controller[M],
  override val getParent: FsController[_ <: FsModel]
) extends FsAbstractController[M](c.model) {

  override def stat(options: AccessOptions, name: FsEntryName) =
    c.stat(options, name).orNull
  override def checkAccess(options: AccessOptions, name: FsEntryName, types: BitField[Access]) =
    c.checkAccess(options, name, types)
  override def setReadOnly(name: FsEntryName) = c.setReadOnly(name)
  override def setTime(options: AccessOptions, name: FsEntryName, times: ju.Map[Access, jl.Long]) =
    c.setTime(options, name, times)
  override def setTime(options: AccessOptions, name: FsEntryName, types: BitField[Access], value: Long) =
    c.setTime(options, name, types, value)
  def input(options: AccessOptions, name: FsEntryName) = c.input(options, name)
  def output(options: AccessOptions, name: FsEntryName, template: Entry) =
    c.output(options, name, Option(template))
  override def mknod(options: AccessOptions, name: FsEntryName, tµpe: Type, template: Entry) =
    c.mknod(options, name, tµpe, Option(template))
  override def unlink(options: AccessOptions, name: FsEntryName) =
    c.unlink(options, name)
  override def sync(options: SyncOptions) = c.sync(options)
}
