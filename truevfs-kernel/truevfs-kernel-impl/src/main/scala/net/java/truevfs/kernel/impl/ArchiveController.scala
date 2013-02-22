/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import java.io._
import java.nio.file._
import net.java.truevfs.kernel.spec._
import net.java.truecommons.cio._
import net.java.truecommons.cio.Entry._

/** Provides read/write access to an archive file system.
  * This is a mirror of [[net.java.truevfs.kernel.spec.FsController]] which has
  * been customozed to leverage the Scala language and library.
  *
  * @author Christian Schlichtherle
  */
private trait ArchiveController[E <: FsArchiveEntry] {

  /** Returns the archive model.
    *
    * @return The archive model.
    */
  def model: ArchiveModel[E]

  def node(options: AccessOptions, name: FsNodeName): Option[FsNode]
  def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access])
  def setReadOnly(options: AccessOptions, name: FsNodeName)
  def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]): Boolean
  def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long): Boolean
  def input(options: AccessOptions, name: FsNodeName): AnyInputSocket
  def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]): AnyOutputSocket
  def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry])
  def unlink(options: AccessOptions, name: FsNodeName)
  def sync(options: SyncOptions)

  /** Two archive controllers are considered equal if and only if they are
    * identical.
    *
    * @param  that the object to compare.
    * @return `this == that`
    */
  final override def equals(that: Any) = this == that

  /** Returns a hash code which is consistent with `equals`.
    *
    * @return A hash code which is consistent with `equals`.
    * @see    Object#hashCode
    */
   final override def hashCode = System.identityHashCode(this)

  /** Returns a string representation of this object for logging and debugging
    * purposes.
    */
  final override def toString =
    "%s@%x[model=%s]".format(getClass.getName, hashCode, model)
}
