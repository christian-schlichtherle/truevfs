/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pace

import javax.annotation._
import javax.annotation.concurrent._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._

/** Calls a template method to apply an aspect to every file system operation.
  * 
  * @see    #apply
  * @author Christian Schlichtherle
  */
@ThreadSafe
private[pace] abstract class AspectController(controller: FsController)
extends FsDecoratingController(controller) {

  /**
    * Applies the aspect to the given file system operation.
    * 
    * @param  operation the file system operation to apply an aspect to.
    * @return The return value of the file system operation.
    */
  protected def apply[V](operation: () => V): V

  private type AccessOptions = BitField[FsAccessOption]

  override def node(options: AccessOptions, name: FsNodeName) =
    apply(() => controller node (options, name))

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]) =
    apply(() => controller checkAccess (options, name, types))

  override def setReadOnly(name: FsNodeName) =
    apply(() => controller setReadOnly (name))

  override def setTime(options: AccessOptions, name: FsNodeName, times: java.util.Map[Access, java.lang.Long]) =
    apply(() => controller setTime (options, name, times))

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long) =
    apply(() => controller setTime (options, name, types, value))

  override def input(options: AccessOptions, name: FsNodeName) =
    new Input(controller input (options, name)): InputSocket[_ <: Entry]

  private class Input(socket: InputSocket[_ <: Entry])
  extends AbstractInputSocket[Entry] {
    override def target() = apply(() => socket target ())

    override def stream(peer: OutputSocket[_ <: Entry]) =
      apply(() => socket stream peer)

    override def channel(peer: OutputSocket[_ <: Entry]) =
      apply(() => socket channel peer)
  }

  override def output(options: AccessOptions, name: FsNodeName, @CheckForNull template: Entry) =
    new Output(controller output (options, name, template)): OutputSocket[_ <: Entry]

  private class Output(socket: OutputSocket[_ <: Entry])
  extends AbstractOutputSocket[Entry] {
    override def target() = apply(() => socket target ())

    override def stream(peer: InputSocket[_ <: Entry]) =
      apply(() => socket stream peer)

    override def channel(peer: InputSocket[_ <: Entry]) =
      apply(() => socket channel peer)
  }

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, @CheckForNull template: Entry) =
    apply(() => controller make (options, name, tµpe, template))

  override def unlink(options: AccessOptions, name: FsNodeName) =
    apply(() => controller unlink (options, name))

  override def sync(options: BitField[FsSyncOption]) =
    apply(() => controller sync options)
}
