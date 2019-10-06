/*
 * Copyright (C) 2005-2015 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.ext.pacemaker

import java.io.{InputStream, OutputStream}
import java.nio.channels.SeekableByteChannel

import javax.annotation._
import net.java.truecommons.cio.Entry._
import net.java.truecommons.cio._
import net.java.truecommons.shed._
import net.java.truevfs.kernel.spec._

/** Calls a template method to apply an aspect to every file system operation.
  *
  * @see #apply
  * @author Christian Schlichtherle
  */
private abstract class AspectController(controller: FsController) extends FsDecoratingController(controller) {

  /**
    * Applies the aspect to the given file system operation.
    *
    * @param  operation the file system operation to apply an aspect to.
    * @return The return value of the file system operation.
    */
  protected def apply[V](operation: () => V): V

  override def node(options: AccessOptions, name: FsNodeName): FsNode = {
    apply(() => controller node(options, name))
  }

  override def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access]): Unit = {
    apply(() => controller checkAccess(options, name, types))
  }

  override def setReadOnly(options: AccessOptions, name: FsNodeName): Unit = {
    apply(() => controller setReadOnly(options, name))
  }

  override def setTime(options: AccessOptions, name: FsNodeName, times: java.util.Map[Access, java.lang.Long]): Boolean = {
    apply(() => controller setTime(options, name, times))
  }

  override def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long): Boolean = {
    apply(() => controller setTime(options, name, types, value))
  }

  override def input(options: AccessOptions, name: FsNodeName): InputSocket[_ <: Entry] = {
    new Input(controller input(options, name)): InputSocket[_ <: Entry]
  }

  private class Input(socket: InputSocket[_ <: Entry]) extends AbstractInputSocket[Entry] {

    override def target(): Entry = apply(() => socket.target())

    override def stream(peer: OutputSocket[_ <: Entry]): InputStream = apply(() => socket stream peer)

    override def channel(peer: OutputSocket[_ <: Entry]): SeekableByteChannel = apply(() => socket channel peer)
  }

  override def output(options: AccessOptions, name: FsNodeName, @CheckForNull template: Entry): OutputSocket[_ <: Entry] = {
    new Output(controller output(options, name, template)): OutputSocket[_ <: Entry]
  }

  private class Output(socket: OutputSocket[_ <: Entry]) extends AbstractOutputSocket[Entry] {

    override def target(): Entry = apply(() => socket.target())

    override def stream(peer: InputSocket[_ <: Entry]): OutputStream = apply(() => socket stream peer)

    override def channel(peer: InputSocket[_ <: Entry]): SeekableByteChannel = apply(() => socket channel peer)
  }

  override def make(options: AccessOptions, name: FsNodeName, tµpe: Type, @CheckForNull template: Entry): Unit = {
    apply(() => controller make(options, name, tµpe, template))
  }

  override def unlink(options: AccessOptions, name: FsNodeName): Unit = {
    apply(() => controller unlink(options, name))
  }

  override def sync(options: BitField[FsSyncOption]): Unit = {
    apply(() => controller sync options)
  }
}
