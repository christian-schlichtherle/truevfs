/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemanager

import javax.annotation._
import javax.annotation.concurrent._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._

/** Applies an aspect to every file system operation.
  * 
  * @see    #apply
  * @author Christian Schlichtherle
  */
@ThreadSafe
private abstract class AspectController(c: FsController)
extends FsDecoratingController(c) {

  /**
    * Applies an aspect to the given file system operation.
    * 
    * @param  operation the file system operation to apply an aspect to.
    * @return The return value of the file system operation.
    */
  def apply[V](operation: => V): V

  private type AccessOptions = BitField[FsAccessOption]

  override def node(options: AccessOptions, name: FsNodeName) =
    apply(c node (options, name))

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    apply(c checkAccess (options, name, types))

  override def setReadOnly(name: FsNodeName) =
    apply(c setReadOnly (name))

  override def setTime(options: AccessOptions, name: FsNodeName, times: java.util.Map[Access, java.lang.Long]) =
    apply(c setTime (options, name, times))

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    apply(c setTime (options, name, types, value))

  override def input(options: AccessOptions, name: FsNodeName) =
    new AspectInputSocket(c input (options, name))

  protected class AspectInputSocket(socket: InputSocket[_ <: Entry])
  extends AbstractInputSocket[Entry] {
    override def target() = apply(socket target ())

    override def stream(peer: OutputSocket[_ <: Entry]) =
      apply(socket stream peer)

    override def channel(peer: OutputSocket[_ <: Entry]) =
      apply(socket channel peer)
  }

  override def output(options: AccessOptions, name: FsNodeName, @CheckForNull template: Entry) =
    new AspectOutputSocket(c output (options, name, template))

  protected class AspectOutputSocket(socket: OutputSocket[_ <: Entry])
  extends AbstractOutputSocket[Entry] {
    override def target() = apply(socket target ())

    override def stream(peer: InputSocket[_ <: Entry]) =
      apply(socket stream peer)

    override def channel(peer: InputSocket[_ <: Entry]) =
      apply(socket channel peer)
  }

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, @CheckForNull template: Entry) =
    apply(c make (options, name, tµpe, template))

  override def unlink(options: AccessOptions, name: FsNodeName) =
    apply(c unlink (options, name))

  override def sync(options: BitField[FsSyncOption]) =
    apply(c sync options)
}
