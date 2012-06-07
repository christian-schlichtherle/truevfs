/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._
import net.truevfs.kernel.cio._
import scala.annotation.tailrec

/** A mixin for an I/O socket which obtains its delegate socket lazily and
  * `reset`s it upon any [[java.lang.Throwable]].
  *
  * @tparam E the type of the `localTarget`.
  * @tparam S the type of the socket returned by `lazySocket`.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
trait ClutchSocketLike[E <: Entry, +S <: IoSocket[_ <: E, Entry]] {
  this: IoSocket[E, Entry] =>

  private[this] var _socket: Option[S] = None

  protected final def apply[A](f: S => A) = {
    try { f(boundSocket) }
    catch { case ex => reset(); throw ex }
  }

  protected final def reset() { _socket = None }

  protected def boundSocket(): S

  @tailrec
  protected final def socket: S = {
    _socket match {
      case Some(socket) => socket
      // In case lazySocket returns null, this will produce an NPE, which is
      // better than a retry which may probably only result in an endless loop.
      case None => _socket = Some(lazySocket); socket
    }
  }

  /** Returns the I/O socket for lazy initialization.
    * 
    * @return the I/O socket for lazy initialization.
    * @throws IOException on any I/O error. 
    */
  protected def lazySocket: S

  /** Returns a string representation of this object for debugging and logging
    * purposes.
    */
  override def toString = "%s[socket=%s]" format (getClass.getName, socket)
}
