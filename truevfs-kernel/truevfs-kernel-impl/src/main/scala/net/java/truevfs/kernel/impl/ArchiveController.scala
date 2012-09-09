/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl

import net.java.truecommons.shed._
import java.io._
import java.nio.file._
import net.java.truevfs.kernel.spec._
import net.java.truevfs.kernel.spec.cio._
import net.java.truevfs.kernel.spec.cio.Entry._

/** Provides read/write access to an archive file system.
  *
  * @author Christian Schlichtherle
  */
private trait ArchiveController[E <: FsArchiveEntry]
extends ArchiveModelAspect[E] {

  /** Returns the controller for the parent file system or `null` if and only
    * if this file system is not federated, i.e. not a member of another file
    * system.
    * Multiple invocations must return the same object.
    * 
    * @return The nullable controller for the parent file system.
    */
  //def parent: Option[Controller[_ <: FsModel]]

  /** Returns the file system node for the given `name` or `null` if it
    * doesn't exist.
    * Modifying the returned node does not show any effect on the file system
    * and should result in an {@link UnsupportedOperationException}.
    * 
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @return A file system entry or `null` if no file system entry exists for
    *         the given name.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def node(options: AccessOptions, name: FsNodeName): Option[FsNode]

  /** Checks if the file system entry for the given `name` exists when
    * constrained by the given access {@code options} and permits the given
    * access `types`.
    * 
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @param  types the types of the desired access.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def checkAccess(options: AccessOptions, name: FsNodeName, types: BitField[Access])

  /** Sets the named file system entry as read-only.
    * This method will fail for typical federated (archive) file system
    * controller implementations because they do not support it.
    * 
    * @param  name the name of the file system entry.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def setReadOnly(name: FsNodeName)

  /** Makes an attempt to set the last access time of all types in the given
    * map for the file system entry with the given name.
    * If `false` is returned or an [[java.io.IOException]] is thrown, then
    * still some of the last access times may have been set.
    * Whether or not this is an atomic operation is specific to the
    * implementation.
    * 
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @param  times the access times.
    * @return `true` if and only if setting the access time for all types in
    *         `times` succeeded.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def setTime(options: AccessOptions, name: FsNodeName, times: Map[Access, Long]): Boolean

  /** Makes an attempt to set the last access time of all types in the given
    * bit field for the file system entry with the given name.
    * If `false` gets returned or an [[java.io.IOException]] gets thrown, then
    * still some of the last access times may have been set.
    * 
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @param  types the access types.
    * @param  value the last access time.
    * @return `true` if and only if setting the access time for all
    *         types in `types` succeeded.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def setTime(options: AccessOptions, name: FsNodeName, types: BitField[Access], value: Long): Boolean

  /** Returns an input socket for reading the contents of the file system
    * entry addressed by the given name from the file system.
    *
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @return An {@code InputSocket}.
    */
  def input(options: AccessOptions, name: FsNodeName): AnyInputSocket

  /** Returns an output socket for writing the contents of the entry addressed
    * by the given name to the file system.
    *
    * @param  options the options for accessing the file system entry.
    *         If [[net.java.truevfs.kernel.impl.FsAccessOption.CREATE_PARENTS]] is set,
    *         any missing parent directories shall get created with an
    *         undefined last modification time.
    * @param  name the name of the file system entry.
    * @param  template if not `null`, then the file system entry
    *         at the end of the chain shall inherit as much properties from
    *         this entry as possible - with the exception of its name and type.
    * @return An [[net.java.truevfs.kernel.impl.cio.OutputSocket]].
    */
  def output(options: AccessOptions, name: FsNodeName, template: Option[Entry]): AnyOutputSocket

  /** Creates or replaces and finally links a chain of one or more entries
    * for the given entry `name` into the file system.
    *
    * @param  options the options for accessing the file system entry.
    *         If [[net.java.truevfs.kernel.impl.FsAccessOption.CREATE_PARENTS]] is set,
    *         any missing parent directories shall get created with an
    *         undefined last modification time.
    * @param  name the name of the file system entry.
    * @param  type the file system entry type.
    * @param  template if not `null`, then the file system entry
    *         at the end of the chain shall inherit as much properties from
    *         this entry as possible - with the exception of its name and type.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def make(options: AccessOptions, name: FsNodeName, tµpe: Type, template: Option[Entry])

  /** Removes the named file system entry from the file system.
    * If the named file system entry is a directory, it must be empty.
    * 
    * @param  options the options for accessing the file system entry.
    * @param  name the name of the file system entry.
    * @throws FileSystemException on any file system error.
    * @throws IOException on any I/O error.
    */
  def unlink(options: AccessOptions, name: FsNodeName)

  /** Commits all unsynchronized changes to the contents of this file system
    * to its parent file system,
    * releases the associated resources (e.g. target archive files) for
    * access by third parties (e.g. other processes), cleans up any temporary
    * allocated resources (e.g. temporary files) and purges any cached data.
    * Note that temporary resources may get allocated even if the federated
    * file systems were accessed read-only.
    * If this is not a federated file system, i.e. if its not a member of a
    * parent file system, then nothing happens.
    * Otherwise, the state of this file system controller is reset.
    *
    * @param  options the options for synchronizing the file system.
    * @throws FsSyncWarningException if ''only'' warning conditions
    *         apply.
    *         This implies that the respective parent file system has been
    *         synchronized with constraints, e.g. if an unclosed archive entry
    *         stream gets forcibly closed.
    * @throws FsSyncException if any error conditions apply.
    */
  def sync(options: SyncOptions)

  /** Two file system controllers are considered equal if and only if
    * they are identical.
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

  /** Returns a string representation of this object for debugging and logging
    * purposes.
    * 
    * @return A string representation of this object for debugging and logging
    *         purposes.
    */
  final override def toString =
    "%s@%x[model=%s]".format(getClass.getName, hashCode, model)
}
