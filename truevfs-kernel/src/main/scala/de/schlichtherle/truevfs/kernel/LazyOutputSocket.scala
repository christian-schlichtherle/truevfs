/*
 * Copyright (C) 2005-2012 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package de.schlichtherle.truevfs.kernel

import javax.annotation.concurrent._
import net.truevfs.kernel.cio._

/** An output socket which obtains its delegate socket lazily and `reset`s
  * it upon any [[java.lang.Throwable]].
  *
  * @see    LazyInputSocket
  * @tparam E the type of the `target`.
  * @author Christian Schlichtherle
  */
@NotThreadSafe
private abstract class LazyOutputSocket[E <: Entry]
extends AbstractOutputSocket[E] with LazySocketLike[E, OutputSocket[_ <: E]] {
  override def target() = apply(_.target())
  override def stream(peer: AnyInputSocket) = apply(_.stream(peer))
  override def channel(peer: AnyInputSocket) = apply(_.channel(peer))
}
