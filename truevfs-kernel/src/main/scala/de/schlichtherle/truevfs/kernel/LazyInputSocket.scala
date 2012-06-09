/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._
import net.truevfs.kernel.cio._

/** An input socket which obtains its delegate socket lazily and `reset`s
  * it upon any [[java.lang.Throwable]].
  *
  * @see    LazyOutputSocket
  * @tparam E the type of the `target`.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class LazyInputSocket[E <: Entry]
extends AbstractInputSocket[E] with LazySocketLike[E, InputSocket[_ <: E]] {
  override def target() = apply(_.target())
  override def stream(peer: AnyOutputSocket) = apply(_.stream(peer))
  override def channel(peer: AnyOutputSocket) = apply(_.channel(peer))
}
