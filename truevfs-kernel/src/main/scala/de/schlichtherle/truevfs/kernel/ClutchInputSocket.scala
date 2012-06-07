/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._
import net.truevfs.kernel.cio._
import scala.annotation.tailrec

/** An input socket which obtains its delegate socket lazily and `reset`s
  * it upon any [[java.lang.Throwable]].
  *
  * @see    ClutchOutputSocket
  * @tparam E the type of the `localTarget`.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class ClutchInputSocket[E <: Entry]
extends DelegatingInputSocket[E] {

  private[this] var _socket: Option[InputSocket[_ <: E]] = None

  override def localTarget() = apply(_.localTarget())

  override def stream() = apply(_.stream())

  override def channel() = apply(_.channel())

  private def apply[A](f: InputSocket[_ <: E] => A) = {
    try { f(boundSocket) }
    catch { case ex => reset(); throw ex }
  }

  protected final def reset() { _socket = None }

  @tailrec
  protected final override def socket: InputSocket[_ <: E] = {
    _socket match {
      case Some(socket) => socket
      // In case lazySocket returns null, this will produce an NPE, which is
      // better than a retry which may probably only result in an endless loop.
      case None => _socket = Some(lazySocket); socket
    }
  }

  /** Returns the input socket for lazy initialization.
    * 
    * @return the input socket for lazy initialization.
    * @throws IOException on any I/O error. 
    */
  protected def lazySocket: InputSocket[_ <: E]

  /** Returns a string representation of this object for debugging and logging
    * purposes.
    */
  override def toString = "%s[socket=%s]" format (getClass.getName, socket)
}
