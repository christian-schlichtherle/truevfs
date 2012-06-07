/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._
import net.truevfs.kernel._
import ControlFlowException._

/** Indicates a condition which requires non-local control flow within a
  * decorator chain of
  * [[net.truevfs.kernel.FsController file system controllers]].
  * 
  * File system controllers are typically arranged in a decorator and
  * chain-of-responsibility pattern.
  * Unfortunately, some aspects of file system management make it necessary to
  * use exceptions for non-local control flow in these chains.
  * For example:
  * 
  * - A file system controller may throw an instance of a sub-class to indicate
  *   a false positive archive file.
  *   Some other file system controller further up the chain is then expected
  *   to catch this exception in order to route the file system operation to
  *   the parent file system controller instead.
  * - Yet another file system controller may detect a potential dead lock and
  *   throw an instance of a sub-class to indicate this.
  *   Some other file system controller further up the chain is then expected
  *   to catch this exception in order to pause the current thread for a small
  *   random time interval and retry the operation.
  * - This pattern continues for automatic synchronization etc...
  * 
  * === How This Applies To You (Or The Future Me) ===
  * 
  * If you are only using a file system controller, for example by calling
  * `FsManager.controller`, then you don't need to be concerned about file
  * system controller exceptions at all because they shall never pass to client
  * applications (this would be a bug).
  * 
  * As an implementor of a file system controller however, for example when
  * writing a custom controller for an archive file system driver by extending
  * this class, then you need to be aware that you may receive file system
  * controller exceptions whenever you call a method on the decorated file
  * system controller.
  * Unless you have special requirements, you don't need to catch such an
  * exception.
  * Just make sure to always leave your controller in a consistent state, for
  * example by protecting all access to the decorated controller with a
  * try-finally block:
  * 
  * {{{
  * override def stat(options: BitField[FsAccessOption], name: FsEntryName) = {
  *   prepareMyResources()
  *   try {
  *     delegate.stat() // may throw ControlFlowException!
  *   } finally {
  *     cleanUpMyResources()
  *   }
  * }
  * }}}
  * 
  * @author Christian Schlichtherle
  */
@Immutable
private abstract class ControlFlowException
extends RuntimeException(null, null, traceable, traceable)

private object ControlFlowException {

  /** Controls whether or not instances of this class have a regular stack
    * trace or an empty stack trace.
    * If and only if the system property with the name
    * `de.schlichtherle.truevfs.kernel.ControlFlowException.traceable`
    * is set to `true` (whereby case is ignored), then instances of this
    * class will have a regular stack trace, otherwise their stack trace will
    * be empty.
    * Note that this should be set to `true` for debugging purposes only.
    */
  val traceable = sys.BooleanProp
  .valueIsTrue(classOf[ControlFlowException].getName + ".traceable")
}
