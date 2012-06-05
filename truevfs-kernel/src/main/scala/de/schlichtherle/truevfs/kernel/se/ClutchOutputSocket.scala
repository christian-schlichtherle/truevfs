/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel.se

import javax.annotation.concurrent._
import net.truevfs.kernel.cio._
import scala.annotation.tailrec

/** An output socket which obtains its delegate socket lazily and `reset`s
  * it upon any [[java.lang.Throwable]].
  *
  * @see    ClutchInputSocket
  * @tparam E the type of the `localTarget`.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class ClutchOutputSocket[E <: Entry]
extends DelegatingOutputSocket[E] {

  private[this] var _socket: Option[OutputSocket[_ <: E]] = None

  override def localTarget() = apply(_.localTarget())

  override def stream() = apply(_.stream())

  override def channel() = apply(_.channel())

  private def apply[A](f: OutputSocket[_ <: E] => A) = {
    try { f(boundSocket) }
    catch { case ex => reset(); throw ex }
  }

  protected final def reset() { _socket = None }

  @tailrec
  protected final override def socket: OutputSocket[_ <: E] = {
    _socket match {
      case Some(socket) => socket
      case None => _socket = Some(lazySocket); socket
    }
  }

  /** Returns the output socket for lazy initialization.
    * 
    * @return the output socket for lazy initialization.
    * @throws IOException on any I/O error. 
    */
  protected def lazySocket: OutputSocket[_ <: E]

  /** Returns a string representation of this object for debugging and logging
    * purposes.
    */
  override def toString = "%s[socket=%s]" format (getClass.getName, socket)
}
